package com.graffitab.server.service.user;

import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.javatuples.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.user.ExternalProviderDto.ExternalProviderType;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.errors.EntityNotFoundException;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.api.errors.UserNotLoggedInException;
import com.graffitab.server.api.errors.ValidationErrorException;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.asset.Asset.AssetType;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.PagingService;
import com.graffitab.server.service.ProxyUtilities;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.email.EmailService;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.util.GuidGenerator;
import com.graffitab.server.util.PasswordGenerator;

import lombok.extern.log4j.Log4j2;

/**
 * Created by david
 */
@Service
@Log4j2
public class UserService {

	@Resource
	private HibernateDaoImpl<User, Long> userDao;

	@Resource
	private UserSessionService userSessionService;

	@Resource
	private DatastoreService datastoreService;

	@Resource
	private UserValidationService validationService;

	@Resource
	private PasswordEncoder passwordEncoder;

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private EmailService emailService;

	@Resource
	private NotificationService notificationService;

	@Resource
	private OrikaMapper mapper;

	@Resource
	private HttpServletRequest request;

	@Resource
	private PagingService pagingService;

	@Resource
	private ActivityService activityService;

	public static final String ACTIVATION_TOKEN_METADATA_KEY = "activationToken";
	public static final String ACTIVATION_TOKEN_DATE_METADATA_KEY = "activationTokenDate";
	public static final String RESET_PASSWORD_ACTIVATION_TOKEN = "resetPasswordToken";
	public static final String RESET_PASSWORD_ACTIVATION_TOKEN_DATE = "resetPasswordTokenDate";
	public static final String EXTERNAL_PROVIDER_ID_KEY = "%s_externalId";
	public static final String EXTERNAL_PROVIDER_TOKEN_KEY = "%s_externalToken";

	// 6 hours in milliseconds.
	public static final Long ACTIVATION_TOKEN_EXPIRATION_MS = 6 * 60 * 60 * 1000L;
	// 30 minutes
	public static final Long RESET_PASSWORD_TOKEN_EXPIRATION_MS = 30 * 60 * 1000L;

	private static ThreadLocal<User> threadLocalUserCache = new ThreadLocal<>();

	@Transactional
	public List<Long> getUserFollowersIds(User user) {
		Query query = userDao.createNamedQuery("User.getFollowerIds");
		query.setParameter("currentUser", user);
		@SuppressWarnings("unchecked")
		List<Long> ids = (List<Long>) query.list();
		return ids;
	}

	@Transactional(readOnly = true)
	public User getUser(Long id) {
		User user = findUserById(id);

		if (user != null) {
			throw new EntityNotFoundException(ResultCode.USER_NOT_FOUND, "Could not find user with id " + id);
		}

		return user;
	}

	@Transactional(readOnly = true)
	public User getUserProfile(Long id) {
		User user = findUserById(id);

		if (user != null) {
			return user;
		} else {
			throw new EntityNotFoundException(ResultCode.NOT_FOUND, "Could not find user with id " + id);
		}
	}

	public User getCurrentUser() {

		if (RunAsUser.get() != null) {
			return RunAsUser.get();
		}

		User user = threadLocalUserCache.get();

		if (user == null) {
			// If no user is cached, we get it from the session
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.getPrincipal() != null) {
				User currentUser = (User) auth.getPrincipal();

				// Fully initialize the user, but avoid initializing collections inside collections
				user = transactionUtils.executeInTransactionWithResult(() -> {
					User storedUser = findUserById(currentUser.getId());
					ProxyUtilities.initializeObjectWithOneLevelCollections(storedUser);
					return storedUser;
				});

				threadLocalUserCache.set(user);
			} else {
				String msg = "Cannot get logged in user";
				throw new UserNotLoggedInException(msg);
			}
		}

		return user;
	}

	@Transactional(readOnly = true)
	public User getUserByUsername(String username) {
		try {
			User user = (User) findUserByUsername(username);

			return user;
		} catch (UsernameNotFoundException e) {
			throw new EntityNotFoundException(ResultCode.USER_NOT_FOUND, "Could not find user " + username);
		}
	}

	@Transactional
	public void linkExternalProvider(String externalProviderId, String externalProviderToken,
			ExternalProviderType externalProviderType) {
		User user = findUsersWithMetadataValues(String.format(EXTERNAL_PROVIDER_ID_KEY, externalProviderType.name()),
				externalProviderToken);
		User currentUser = getCurrentUser();
		merge(currentUser);

		// Check if a user with that externalId already exists.
		if (user != null) {
			throw new RestApiException(ResultCode.ALREADY_EXISTS,
					"A user with externalId " + externalProviderId + " already exists");
		}

		currentUser.getMetadataItems().put(String.format(EXTERNAL_PROVIDER_ID_KEY, externalProviderType.name()),
				externalProviderId);
		currentUser.getMetadataItems().put(String.format(EXTERNAL_PROVIDER_TOKEN_KEY, externalProviderType.name()),
				externalProviderToken);
	}

	@Transactional
	public void unlinkExternalProvider(ExternalProviderType externalProviderType) {
		User currentUser = getCurrentUser();
		merge(currentUser);

		// User can only have 1 instance of a provider associated with their account, so delete it if it's found.
		currentUser.getMetadataItems().remove(String.format(EXTERNAL_PROVIDER_ID_KEY, externalProviderType.name()));
		currentUser.getMetadataItems().remove(String.format(EXTERNAL_PROVIDER_TOKEN_KEY, externalProviderType.name()));
	}

	@Transactional
	public User verifyExternalProvider(String externalId, String accessToken,
			ExternalProviderType externalProviderType) {
		User user = findUsersWithMetadataValues(String.format(EXTERNAL_PROVIDER_ID_KEY, externalProviderType.name()),
				externalId);

		if (user == null) {
			throw new EntityNotFoundException(ResultCode.NOT_FOUND,
					"Could not find user with externalId " + externalId);
		}

		// The user is effectively authenticated using ExternalProviderAuthenticationFilter

		return user;
	}

	public User activateUser(String token) {
		User user = transactionUtils.executeInTransactionWithResult(() -> {
			User innerUser = findUsersWithMetadataValues(ACTIVATION_TOKEN_METADATA_KEY, token);

			if (innerUser == null) {
				throw new EntityNotFoundException(ResultCode.NOT_FOUND, "Could not find token " + token);
			}

			Long tokenDate = Long.parseLong(innerUser.getMetadataItems().get(ACTIVATION_TOKEN_DATE_METADATA_KEY));
			Long now = System.currentTimeMillis();

			if ((now - tokenDate) > ACTIVATION_TOKEN_EXPIRATION_MS) {
				throw new RestApiException(ResultCode.TOKEN_EXPIRED, "This token has expired.");
			}

			if (innerUser.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
				throw new RestApiException(ResultCode.USER_NOT_IN_EXPECTED_STATE, "Current user is not in the expected state " +
						AccountStatus.PENDING_ACTIVATION.name());
			}

			innerUser.setAccountStatus(AccountStatus.ACTIVE);

			return innerUser;
		});

		// Add notification to the activated user.
		notificationService.addWelcomeNotificationAsync(user);

		return user;
	}

	public User createUser(User user, String userToken) {
		if (validationService.validateUser(user)) {
			if (user.getId() == null) {
				transactionUtils.executeInTransaction(() -> {
					user.setPassword(passwordEncoder.encode(user.getPassword()));
					user.setGuid(GuidGenerator.generate());
					user.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
					user.getMetadataItems().put(ACTIVATION_TOKEN_METADATA_KEY, userToken);
					user.getMetadataItems().put(ACTIVATION_TOKEN_DATE_METADATA_KEY, System.currentTimeMillis() + "");
					userDao.persist(user);
				});

				emailService.sendWelcomeEmail(user.getUsername(), user.getEmail(),
						generateUserAccountActivationLink(userToken));

				return user;
			} else {
				throw new RestApiException(ResultCode.BAD_REQUEST,
						"ID has been provided to create endpoint -- This is not allowed");
			}
		} else {
			throw new ValidationErrorException("Validation error creating user");
		}
	}

	public User createExternalUser(User user, final String externalProviderId, String externalProviderToken,
			ExternalProviderType externalProviderType) {
		if (validationService.validateUser(user)) {
			if (user.getId() == null) {
				User validatedExternalProviderUser = transactionUtils.executeInTransactionWithResult(() -> {
					return findUsersWithMetadataValues(String.format(EXTERNAL_PROVIDER_ID_KEY, externalProviderType.name()),
							externalProviderToken);
				});

				if (validatedExternalProviderUser == null) {
					transactionUtils.executeInTransaction(() -> {
						user.setPassword(passwordEncoder.encode(PasswordGenerator.generatePassword()));
						user.setGuid(GuidGenerator.generate());
						user.setAccountStatus(AccountStatus.ACTIVE);
						user.getMetadataItems().put(String.format(EXTERNAL_PROVIDER_ID_KEY, externalProviderType.name()),
								externalProviderId);
						user.getMetadataItems().put(String.format(EXTERNAL_PROVIDER_TOKEN_KEY, externalProviderType.name()),
								externalProviderToken);
						userDao.persist(user);
					});

					emailService.sendWelcomeExternalEmail(user.getUsername(), user.getEmail());

					// Add notification to the new user.
					notificationService.addWelcomeNotificationAsync(user);

					return user;
				}
				else {
					throw new RestApiException(ResultCode.BAD_REQUEST,
							"This external provider is already used for another user.");
				}
			} else {
				throw new RestApiException(ResultCode.BAD_REQUEST,
						"ID has been provided to create endpoint -- This is not allowed");
			}
		} else {
			throw new ValidationErrorException("Validation error creating user");
		}
	}

	@Transactional
	public User updateUser(User user) {
		if (validationService.validateUser(user)) {
			User currentUser = getCurrentUser();
			merge(currentUser);
			mapper.map(user, currentUser);
			return currentUser;

		} else {
			throw new ValidationErrorException("Validation error updating user");
		}
	}

	@Transactional(readOnly = true)
	public ListItemsResult<UserDto> searchUsersResult(String userQuery, Integer offset, Integer count) {
		// By username, first name, lastName.
		// TODO: Need to escape the characters to prevent SQL injection here.
		userQuery = "%" + userQuery + "%";

		Query query = userDao.createNamedQuery("User.searchUser");
		query.setParameter("username", userQuery);
		query.setParameter("firstName", userQuery);
		query.setParameter("lastName", userQuery);

		return pagingService.getPagedItems(User.class, UserDto.class, offset, count, query);

		// Example with Criteria.
//		Criterion usernameRestriction = Restrictions.like("username", query, MatchMode.ANYWHERE);
//		Criterion firstNameRestriction = Restrictions.like("firstName", query, MatchMode.ANYWHERE);
//		Criterion lastNameRestriction = Restrictions.like("lastName", query, MatchMode.ANYWHERE);
//		Criterion orRestriction = Restrictions.or(usernameRestriction, firstNameRestriction, lastNameRestriction);
//
//		Integer total = (Integer) userDao.getSession().createCriteria(User.class).add(orRestriction)
//				.setProjection(Projections.rowCount()).uniqueResult();
//
//		List<User> listUsers = (List<User>) userDao.getSession().createCriteria(User.class).add(orRestriction)
//				.setFirstResult(offset).setMaxResults(count).list();
//
//		return new PagedList<User>(listUsers, total, offset);
	}

	public Asset updateAvatar(InputStream assetInputStream, long contentLength) {
		Asset assetToAdd = Asset.asset(AssetType.IMAGE);
		User user = getCurrentUser();

		String currentAvatarAssetGuid = null;

		if (user.getAvatarAsset() != null) {
			currentAvatarAssetGuid = user.getAvatarAsset().getGuid();
		}

		transactionUtils.executeInTransaction(() -> {
			// Need to reassign, as 'user' is final in this lambda
			// and we cannot change it
			User storedUser = user;
			storedUser.setAvatarAsset(assetToAdd);
		});

		datastoreService.saveAsset(assetInputStream, contentLength, assetToAdd.getGuid());

		if (currentAvatarAssetGuid != null) {
			datastoreService.deleteAsset(currentAvatarAssetGuid);
		}

		return assetToAdd;
	}

	public void deleteAvatar() {
		User user = getCurrentUser();

		// Delete current avatar from store, if it exists.
		if (user.getAvatarAsset() != null) {

			String avatarAsset = user.getAvatarAsset().getGuid();

			transactionUtils.executeInTransaction(() -> {
				// Need to reassign, as 'user' is final in this lambda
				// and we cannot change it
				User storedUser = user;
				// Delete current avatar from database
				storedUser.setAvatarAsset(null);
			});

			datastoreService.deleteAsset(avatarAsset);
		}
	}

	public Asset updateCover(InputStream assetInputStream, long contentLength) {
		Asset assetToAdd = Asset.asset(AssetType.IMAGE);
		User user = getCurrentUser();

		String currentCoverAssetGuid = null;

		if (user.getCoverAsset() != null) {
			currentCoverAssetGuid = user.getCoverAsset().getGuid();
		}

		transactionUtils.executeInTransaction(() -> {
			// Need to reassign, as 'user' is final in this lambda
		    // and we cannot change it
			User storedUser = user;
			storedUser.setCoverAsset(assetToAdd);
		});

		datastoreService.saveAsset(assetInputStream, contentLength, assetToAdd.getGuid());

		if (currentCoverAssetGuid != null) {
			datastoreService.deleteAsset(currentCoverAssetGuid);
		}

		return assetToAdd;
	}

	public void deleteCover() {
		User user = getCurrentUser();

		// Delete current cover from store, if it exists.
		if (user.getCoverAsset() != null) {

			String coverAssetGuid = user.getCoverAsset().getGuid();

			transactionUtils.executeInTransaction(() -> {
				User storedUser = user;
				// Delete current cover from database
				storedUser.setCoverAsset(null);
			});

			datastoreService.deleteAsset(coverAssetGuid);
		}
	}

	@Transactional
	public ListItemsResult<UserDto> getFollowingOrFollowersForUserResult(boolean shouldGetFollowers, Long userId, Integer offset, Integer count) {
		User requestedUser = (userId == null) ? getCurrentUser() : findUserById(userId);

		Query query = userDao.createQuery(
				"select f "
			  + "from User u "
			  + "join u." + (shouldGetFollowers ? "followers" : "following") + " f "
			  + "where u = :currentUser");
		query.setParameter("currentUser", requestedUser);

		return pagingService.getPagedItems(User.class, UserDto.class, offset, count, query);
	}

	public User follow(Long toFollowId) {
		User currentUser = getCurrentUser();

		Pair<User, Boolean> resultPair = transactionUtils.executeInTransactionWithResult(() -> {
			User toFollow = findUserById(toFollowId);

			if (toFollow != null) {
				Boolean isFollowed = false;

				// Users can't follow themselves.
				if (currentUser.equals(toFollow)) {
					throw new RestApiException(ResultCode.BAD_REQUEST, "You cannot follow yourself");
				}

				if (!currentUser.isFollowing(toFollow)) {
					User innerCurrentUser = findUserById(currentUser.getId());
					innerCurrentUser.getFollowing().add(toFollow);
					isFollowed = true;
				}

				return new Pair<User, Boolean>(toFollow, isFollowed);
			} else {
				throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + toFollowId + " not found");
			}
		});

		User followedUser = resultPair.getValue0();
		Boolean followed = resultPair.getValue1();

		// Add notification to the followed user if they have been followed.
		if (followed) {
			notificationService.addFollowNotificationAsync(followedUser, currentUser);

			// Add activity to each follower of the user.
			activityService.addFollowActivityAsync(currentUser, followedUser);
		}

		return resultPair.getValue0();
	}

	@Transactional
	public User unfollow(Long toUnfollowId) {
		User toUnfollow = findUserById(toUnfollowId);

		if (toUnfollow != null) {
			User currentUser = getCurrentUser();
			merge(currentUser);

			currentUser.getFollowing().remove(toUnfollow);

			return toUnfollow;
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + toUnfollowId + " not found");
		}
	}

	public User resetPassword(String email) {
		final String resetPasswordToken = GuidGenerator.generate();
		User user = transactionUtils.executeInTransactionWithResult(() -> {
			User innerUser = findByEmail(email);

			if (innerUser == null) {
				throw new EntityNotFoundException(ResultCode.USER_NOT_FOUND,
						"User with email: " + email + " not found");
			}

			innerUser.setAccountStatus(AccountStatus.RESET_PASSWORD);
			innerUser.getMetadataItems().put(RESET_PASSWORD_ACTIVATION_TOKEN, resetPasswordToken);
			innerUser.getMetadataItems().put(RESET_PASSWORD_ACTIVATION_TOKEN_DATE, System.currentTimeMillis() + "");
			return innerUser;
		});

		emailService.sendResetPasswordEmail(email, generateResetPasswordLink(resetPasswordToken));

		return user;
	}

	@Transactional
	public User completePasswordReset(String token, String newPassword) {

		User user = findUsersWithMetadataValues(RESET_PASSWORD_ACTIVATION_TOKEN, token);

		if (user == null) {
			throw new EntityNotFoundException(ResultCode.NOT_FOUND, "Could not find token " + token);
		}

		Long tokenDate = Long.parseLong(user.getMetadataItems().get(RESET_PASSWORD_ACTIVATION_TOKEN_DATE));
		Long now = System.currentTimeMillis();

		if ((now - tokenDate) > RESET_PASSWORD_TOKEN_EXPIRATION_MS) {
			throw new RestApiException(ResultCode.TOKEN_EXPIRED, "This token has expired.");
		}

		if (user.getAccountStatus() != AccountStatus.RESET_PASSWORD) {
			throw new RestApiException(ResultCode.USER_NOT_IN_EXPECTED_STATE, "Current user is not in the expected state " +
					AccountStatus.RESET_PASSWORD.name());
		}

		user.setAccountStatus(AccountStatus.ACTIVE);
		user.setPassword(passwordEncoder.encode(newPassword));

		// Logout from all devices
		userSessionService.logoutEverywhere(user);

		if (log.isDebugEnabled()) {
			log.debug("Successfully reset password for user " + user.getUsername());
		}

		return user;
	}

	@Transactional
	public User changePassword(String currentPassword, String newPassword) {

		User user = getCurrentUser();
		merge(user);

		// Check if the provided password matches the current password.
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new RestApiException(ResultCode.INCORRECT_PASSWORD, "The provided password was incorrect.");
		}

		user.setPassword(passwordEncoder.encode(newPassword));

		// Logout from all devices
        userSessionService.logoutEverywhere(user);

		if (log.isDebugEnabled()) {
			log.debug("Successfully changed password for user " + user.getUsername());
		}

		return user;
	}

	@Transactional
	public ListItemsResult<UserDto> getMostActiveUsersResult(Integer offset, Integer count) {
		Query query = userDao.createNamedQuery("User.getMostActiveUsers");

		return pagingService.getPagedItems(User.class, UserDto.class, offset, count, query);
	}

	@Transactional(readOnly = true)
	public User findUserById(Long id) {
		return userDao.find(id);
	}

	@Transactional(readOnly = true)
	public UserDetails findUserByUsername(String username) throws UsernameNotFoundException {

		UserDetails userDetails = (UserDetails) findByUsername(username);

		if (userDetails == null) {
			throw new UsernameNotFoundException("The user " + username + " was not found");
		}

		return userDetails;
	}

	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		Criteria criteria = userDao.getBaseCriteria();
		User user = (User) criteria.add(Restrictions.eq("username", username)).uniqueResult();
		return user;
	}

	@Transactional(readOnly = true)
	public User findByEmail(String email) {

		User user = (User) userDao.getBaseCriteria().add(Restrictions.eq("email", email)).uniqueResult();
		return user;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<User> findUsersByUsernameWithDifferentId(String username, Long userId) {
		Query query = userDao.createNamedQuery("User.findUsersWithUsername");
		query.setParameter("username", username);
		query.setParameter("userId", userId);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public List<User> findUsersByEmailWithDifferentID(String email, Long userId) {
		Query query = userDao.createNamedQuery("User.findUsersWithEmail");
		query.setParameter("email", email);
		query.setParameter("userId", userId);
		return query.list();
	}

	private User findUsersWithMetadataValues(String key, String value) {
		Query query = userDao.createNamedQuery("User.findUsersWithMetadataValues");
		query.setParameter("metadataKey", key);
		query.setParameter("metadataValue", value);
		return (User) query.uniqueResult();
	}

	/**
	 * Returns the base server link i.e. http://www.graffitab.com:port
	 *
	 * @return
	 */
	private String generateBaseLink() {
		return request.getScheme() + "://" + request.getServerName()
				+ ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
	}

	private String generateResetPasswordLink(String resetPasswordToken) {
		return generateBaseLink() + "/api/users/resetpassword/" + resetPasswordToken;
	}

	private String generateUserAccountActivationLink(String userToken) {
		String activationLink = generateBaseLink() + "/api/users/activate/" + userToken;
		return activationLink;
	}

	public void merge(User user) {
		user = userDao.merge(user);
	}
}

package com.graffitab.server.service.user;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.user.FullUserDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.dto.user.UserSocialFriendsContainerDto;
import com.graffitab.server.api.errors.EntityNotFoundException;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.api.errors.UserNotLoggedInException;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.asset.Asset.AssetType;
import com.graffitab.server.persistence.model.externalprovider.ExternalProvider;
import com.graffitab.server.persistence.model.externalprovider.ExternalProviderType;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.persistence.model.user.UserSocialFriendsContainer;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.ProxyUtilities;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.asset.AssetService;
import com.graffitab.server.service.asset.TransferableStream;
import com.graffitab.server.service.email.EmailService;
import com.graffitab.server.service.image.ImageUtilsService;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.social.SocialNetworksService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.util.GuidGenerator;
import com.graffitab.server.util.PasswordGenerator;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.javatuples.Pair;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

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
	private HttpServletRequest request;

	@Resource
	private PagingService pagingService;

	@Resource
	private ActivityService activityService;

	@Resource
	private SocialNetworksService socialNetworksService;

	@Resource
	private AssetService assetService;

	@Resource
	private ExternalProviderService externalProviderService;

	@Value("${registration.immediateActivation:false}")
	private Boolean immediateUserActivation;

	public static final String ACTIVATION_TOKEN_METADATA_KEY = "activationToken";
	public static final String ACTIVATION_TOKEN_DATE_METADATA_KEY = "activationTokenDate";
	public static final String RESET_PASSWORD_ACTIVATION_TOKEN = "resetPasswordToken";
	public static final String RESET_PASSWORD_ACTIVATION_TOKEN_DATE = "resetPasswordTokenDate";

	// 24 hours in milliseconds.
	public static final Long ACTIVATION_TOKEN_EXPIRATION_MS = 24 * 60 * 60 * 1000L;
	// 30 minutes
	public static final Long RESET_PASSWORD_TOKEN_EXPIRATION_MS = 30 * 60 * 1000L;

	private static ThreadLocal<User> threadLocalUserCache = new ThreadLocal<>();

	private enum UserImageAsset { COVER, AVATAR };

	@Transactional(readOnly = true)
	public User getUser(Long id) {
		User user = findUserById(id);

		if (user == null) {
			throw new EntityNotFoundException(ResultCode.USER_NOT_FOUND, "Could not find user with id " + id);
		}

		return user;
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

	public void invalidateUserCache() {
		threadLocalUserCache.remove();
		RunAsUser.clear();
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
	public User verifyExternalProvider(String externalId, ExternalProviderType externalProviderType) {
		User user = externalProviderService.findUserWithExternalProvider(externalProviderType, externalId);

		if (user == null) {
			throw new EntityNotFoundException(ResultCode.USER_NOT_FOUND,
					"Could not find user with externalId " + externalId);
		}

		// The user is effectively authenticated using ExternalProviderAuthenticationFilter
		return user;
	}

	public User activateUser(String token) {
		User user = transactionUtils.executeInTransactionWithResult(() -> {
			User innerUser = findUsersWithMetadataValues(ACTIVATION_TOKEN_METADATA_KEY, token);

			if (innerUser == null) {
				throw new EntityNotFoundException(ResultCode.TOKEN_NOT_FOUND, "Could not find token " + token);
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

	public User createUser(User user, Locale locale) {
		if (validationService.validateCreateUser(user, false)) {
			if (user.getId() == null) {
				String userToken = GuidGenerator.generate();

				transactionUtils.executeInTransaction(() -> {
					user.setPassword(passwordEncoder.encode(user.getPassword()));
					user.setGuid(GuidGenerator.generate());
					user.setCreatedOn(new DateTime());

					if (immediateUserActivation) {
						user.setAccountStatus(AccountStatus.ACTIVE);
					} else {
						user.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
					}

					user.getMetadataItems().put(ACTIVATION_TOKEN_METADATA_KEY, userToken);
					user.getMetadataItems().put(ACTIVATION_TOKEN_DATE_METADATA_KEY, System.currentTimeMillis() + "");
					userDao.persist(user);
				});

				emailService.sendWelcomeEmail(user.getUsername(), user.getEmail(),
						generateUserAccountActivationLink(userToken, locale.getLanguage()), locale);

				return user;
			} else {
				throw new RestApiException(ResultCode.INVALID_ID,
						"ID has been provided to create endpoint -- This is not allowed");
			}
		}
		return null;
	}

	public User createExternalUser(User user, final String externalUserId, String accessToken,
			ExternalProviderType externalProviderType, Locale locale) {
		// Generate random password for the external user, so that the validation is passed.
		user.setPassword(passwordEncoder.encode(PasswordGenerator.generatePassword()));

		if (validationService.validateCreateUser(user, false)) {
			if (user.getId() == null) {
				User validatedExternalProviderUser = transactionUtils.executeInTransactionWithResult(() -> {
					return externalProviderService.findUserWithExternalProvider(externalProviderType, externalUserId);
				});

				if (validatedExternalProviderUser == null) {
					// Check if access token is valid.
					if (socialNetworksService.isValidToken(accessToken, externalProviderType)) {
						// Register user.
						transactionUtils.executeInTransaction(() -> {
							user.setGuid(GuidGenerator.generate());
							user.setAccountStatus(AccountStatus.ACTIVE);
							user.setCreatedOn(new DateTime());
							user.getExternalProviders().add(ExternalProvider.provider(externalProviderType, externalUserId, accessToken));

							userDao.persist(user);
						});

						emailService.sendWelcomeExternalEmail(user.getUsername(), user.getEmail(), generateGetStartedLink(locale.getLanguage()), locale);

						// Add notification to the new user.
						notificationService.addWelcomeNotificationAsync(user);

						return user;
					}
					else {
						throw new RestApiException(ResultCode.INVALID_TOKEN,
								"The provided token is not valid.");
					}
				}
				else {
					throw new RestApiException(ResultCode.EXTERNAL_PROVIDER_ALREADY_LINKED_FOR_OTHER_USER,
							"This external provider is already used for another user.");
				}
			} else {
				throw new RestApiException(ResultCode.INVALID_ID,
						"ID has been provided to create endpoint -- This is not allowed");
			}
		}
		return null;
	}

	@Transactional
	public User editUser(User toEdit) {
		User currentUser = getCurrentUser();

		// Manually set the userId to the one of the current user for validation.
		toEdit.setId(currentUser.getId());

		if (validationService.validateEditInfo(toEdit)) {
			currentUser.setUpdatedOn(new DateTime());
			currentUser.setFirstName(toEdit.getFirstName());
			currentUser.setLastName(toEdit.getLastName());
			currentUser.setEmail(toEdit.getEmail());
			currentUser.setAbout(toEdit.getAbout());
			currentUser.setWebsite(toEdit.getWebsite());
			merge(currentUser);
			return currentUser;
		}
		return null;
	}

	@Transactional(readOnly = true)
	public ListItemsResult<UserDto> searchUsersResult(String userQuery, Integer offset, Integer limit) {
		// By username, first name, lastName.
		// TODO: Need to escape the characters to prevent SQL injection here.
		userQuery = "%" + userQuery + "%";

		Query query = userDao.createNamedQuery("User.searchUser");
		query.setParameter("username", userQuery);
		query.setParameter("firstName", userQuery);
		query.setParameter("lastName", userQuery);

		return pagingService.getPagedItems(User.class, UserDto.class, offset, limit, query);
	}

	public Asset addOrEditAvatar(TransferableStream transferable, long contentLength) {
		return addOrReplaceUserImageAsset(transferable, contentLength, UserImageAsset.AVATAR);
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
				storedUser.setUpdatedOn(new DateTime());
				merge(storedUser);
			});

			//TODO: in the background?
			datastoreService.deleteAsset(avatarAsset);
			datastoreService.deleteAsset(avatarAsset + ImageUtilsService.ASSET_THUMBNAIL_SUFFIX);
		}
	}

	public Asset addOrEditCover(TransferableStream transferable, long contentLength) {
		return addOrReplaceUserImageAsset(transferable, contentLength, UserImageAsset.COVER);
	}

	public Asset addOrReplaceUserImageAsset(TransferableStream transferable, Long contentLength, UserImageAsset userImageAsset) {
		Asset assetToAdd = Asset.asset(AssetType.IMAGE);
		String assetGuid = assetService.transferAssetFile(transferable, contentLength);
		assetToAdd.setGuid(assetGuid);

		Runnable deferredAssetProcessingRunnable = assetService.prepareAssetForDeferredProcessing(assetGuid);

		try {

			RunAsUser.set(getCurrentUser());

			// Create asset in PROCESSING state in DB
			transactionUtils.executeInTransaction(() -> {
				// Need to reassign, as 'user' is final in this lambda
				// and we cannot change it
				User user = findUserById(getCurrentUser().getId());
				switch (userImageAsset) {
					case AVATAR:
						if (user.getAvatarAsset() != null) {
							assetService.addPreviousAssetGuidMapping(assetGuid, user.getAvatarAsset().getGuid());
						}
						user.setAvatarAsset(assetToAdd);
						break;
					case COVER:
						if (user.getCoverAsset() != null) {
							assetService.addPreviousAssetGuidMapping(assetGuid, user.getCoverAsset().getGuid());
						}
						user.setCoverAsset(assetToAdd);
						break;
					default:
						log.warn("Image user asset type not recognized -- fix code!" + userImageAsset.name());

				}

				user.setUpdatedOn(new DateTime());
			});

			assetService.enqueueDeferredAssetProcessing(deferredAssetProcessingRunnable);

		} finally {
			RunAsUser.clear();
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
				storedUser.setUpdatedOn(new DateTime());
				merge(storedUser);
			});

			datastoreService.deleteAsset(coverAssetGuid);
			datastoreService.deleteAsset(coverAssetGuid + ImageUtilsService.ASSET_THUMBNAIL_SUFFIX);
		}
	}

	@Transactional(readOnly = true)
	public ListItemsResult<UserDto> getFollowingOrFollowersForUserResult(boolean shouldGetFollowers, Long userId, Integer offset, Integer limit) {
		User user = (userId == null) ? getCurrentUser() : findUserById(userId);

		if (user != null) {
			Query query = userDao.createNamedQuery("User." + (shouldGetFollowers ? "getFollowers" : "getFollowing"));
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(User.class, UserDto.class, offset, limit, query);
		}
		else {
			throw new EntityNotFoundException(ResultCode.USER_NOT_FOUND, "Could not find user with id " + userId);
		}
	}

	public User follow(Long toFollowId) {
		final User currentUser = getCurrentUser();

		Pair<User, Boolean> resultPair = transactionUtils.executeInTransactionWithResult(() -> {
			User toFollow = findUserById(toFollowId);

			if (toFollow != null) {
				Boolean isFollowed = false;

				// Users can't follow themselves.
				if (currentUser.equals(toFollow)) {
					throw new RestApiException(ResultCode.INVALID_FOLLOWEE, "You cannot follow yourself");
				}

				if (!currentUser.isFollowing(toFollow)) {
					User innerUser = findUserById(currentUser.getId());
					innerUser.getFollowing().add(toFollow);
					isFollowed = true;
				}

				return new Pair<User, Boolean>(toFollow, isFollowed);
			} else {
				throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + toFollowId + " not found");
			}
		});

		invalidateUserCache();

		User followedUser = resultPair.getValue0();
		Boolean followed = resultPair.getValue1();

		// Add notification to the followed user if they have been followed.
		if (followed) {
			notificationService.addFollowNotificationAsync(followedUser, currentUser);

			// Add activity to each follower of the user.
			activityService.addFollowActivityAsync(currentUser, followedUser);
		}

		return followedUser;
	}

	@Transactional
	public User unfollow(Long toUnfollowId) {
		User toUnfollow = findUserById(toUnfollowId);

		if (toUnfollow != null) {
			User currentUser = getCurrentUser();
			currentUser.getFollowing().remove(toUnfollow);
			merge(currentUser);

			return toUnfollow;
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + toUnfollowId + " not found");
		}
	}

	public User resetPassword(String email, Locale locale) {
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

		emailService.sendResetPasswordEmail(email, generateResetPasswordLink(resetPasswordToken, locale.getLanguage()), locale);

		return user;
	}

	@Transactional
	public User completePasswordReset(String token, String newPassword) {

		User user = findUsersWithMetadataValues(RESET_PASSWORD_ACTIVATION_TOKEN, token);

		if (user == null) {
			throw new EntityNotFoundException(ResultCode.TOKEN_NOT_FOUND, "Could not find token " + token);
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
		user.setFailedLogins(0);

		// Logout from all devices
		userSessionService.logoutEverywhere(user, false);

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
		user.setUpdatedOn(new DateTime());
		user.setFailedLogins(0);

		// Logout from all devices but this one
        userSessionService.logoutEverywhere(user, true);

		if (log.isDebugEnabled()) {
			log.debug("Successfully changed password for user " + user.getUsername());
		}

		return user;
	}

	@Transactional(readOnly = true)
	public ListItemsResult<UserDto> getMostActiveUsersResult(Integer offset, Integer limit) {
		Query query = userDao.createNamedQuery("User.getMostActiveUsers");

		return pagingService.getPagedItems(User.class, UserDto.class, offset, limit, query);
	}

	public ListItemsResult<UserSocialFriendsContainerDto> getSocialFriendsResult(Integer offset, Integer limit) {
		return socialNetworksService.getSocialFriendsResult(offset, limit);
	}

	public UserSocialFriendsContainer getSocialFriendsForProviderResult(ExternalProviderType type, Integer offset, Integer limit) {
		return socialNetworksService.getSocialFriendsForProviderResult(type, offset, limit);
	}

	public Asset importSocialAvatar(ExternalProviderType type) {
		return socialNetworksService.setAvatarFromExternalProvider(type);
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

	public User findUsersWithMetadataValues(String key, String value) {
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

	private String generateResetPasswordLink(String resetPasswordToken, String language) {
		return generateBaseLink() + "/resetpassword/" + resetPasswordToken + "?lang=" + language;
	}

	private String generateUserAccountActivationLink(String userToken, String language) {
		return generateBaseLink() + "/activate/" + userToken + "?lang=" + language;
	}

	private String generateGetStartedLink(String language) {
		return generateBaseLink() + "/getstarted?lang=" + language;
	}

	public void merge(User user) {
		userDao.merge(user);
	}

	public static void clearThreadLocalUserCache() {
		threadLocalUserCache.remove();
	}

	@Transactional
	public void processFollowedByCurrentUser(User user, UserDto userDto) {
		Query query = userDao.createNamedQuery("User.isFollowedByCurrentUser");
		query.setParameter("currentUser", getCurrentUser());
		query.setParameter("otherUser", user);

		Boolean followedByCurrentUser = query.uniqueResult() != null;
		userDto.setFollowedByCurrentUser(followedByCurrentUser);
	}

	@Transactional
	public void processStats(User user, FullUserDto userDto) {
		Query query = userDao.createNamedQuery("User.stats");
		query.setParameter("user", user);
		Object[] result = (Object[]) query.uniqueResult();

        Long graffitiCount = (Long) result[0];
        Integer followersCount = (Integer) result[1];
        Integer followingCount = (Integer) result[2];

        userDto.setFollowersCount(followersCount);
        userDto.setStreamablesCount(graffitiCount);
        userDto.setFollowingCount(followingCount);
	}

	@Transactional
	public void processLinkedAccounts(User user, FullUserDto userDto) {
		User currentUser = getCurrentUser();

		if (!user.equals(currentUser)) { // We only want to show the linked accounts if we're requesting the full profile of the logged in user.
			userDto.setExternalProviders(null);
		}
	}

    @Transactional
    public User findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains(("@"))) {
            return findByEmail(usernameOrEmail);
        } else {
            return findByUsername(usernameOrEmail);
        }
    }

    public void updateLoginAttempts(String usernameOrEmail) {

		Boolean userSuspended = transactionUtils.executeInTransactionWithResult(() -> {

			User user = findByUsernameOrEmail(usernameOrEmail);
			Integer failedLoginAttempts = user.getFailedLogins() == null ? 0 : user.getFailedLogins();

			if (failedLoginAttempts >= 5) {
				log.warn("User " + usernameOrEmail + " has failed 5 times to log in -- setting it in RESET_PASSWORD state");
				user.setAccountStatus(AccountStatus.RESET_PASSWORD);
				return true;
			}
			user.setFailedLogins(failedLoginAttempts + 1);
			return false;
		});

		if (userSuspended) {
			throw new RestApiException(ResultCode.MAXIMUM_LOGIN_ATTEMPTS,
					"User [" + usernameOrEmail + "] hit maximum login attempts");
		}

    }

    @Transactional
    public void resetLoginAttempts(String usernameOrEmail) {
        User user = findByUsernameOrEmail(usernameOrEmail);
        user.setFailedLogins(0);
    }
}

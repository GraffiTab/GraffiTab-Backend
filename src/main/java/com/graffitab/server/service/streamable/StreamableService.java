package com.graffitab.server.service.streamable;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.streamable.StreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableGraffitiDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.asset.Asset.AssetType;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.streamable.StreamableGraffiti;
import com.graffitab.server.service.PagingService;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.service.user.RunAsUser;
import com.graffitab.server.service.user.UserService;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class StreamableService {

	@Resource
	private UserService userService;

	@Resource
	private DatastoreService datastoreService;

	@Resource
	private NotificationService notificationService;

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private PagingService pagingService;

	@Resource
	private HibernateDaoImpl<Streamable, Long> streamableDao;

	@Resource
	private HibernateDaoImpl<User, Long> userDao;

	private ExecutorService executor = Executors.newFixedThreadPool(2);

	public Streamable createStreamableGraffiti(StreamableGraffitiDto streamableGraffitiDto, InputStream assetInputStream, long contentLength) {
		Asset assetToAdd = Asset.asset(AssetType.IMAGE);

		datastoreService.saveAsset(assetInputStream, contentLength, assetToAdd.getGuid());

		Streamable streamable = transactionUtils.executeInTransactionWithResult(() -> {
			User currentUser = userService.findUserById(userService.getCurrentUser().getId());
			Streamable streamableGraffiti = new StreamableGraffiti(streamableGraffitiDto.getLatitude(),
														   streamableGraffitiDto.getLongitude(),
														   streamableGraffitiDto.getRoll(),
														   streamableGraffitiDto.getYaw(),
														   streamableGraffitiDto.getPitch());
			streamableGraffiti.setAsset(assetToAdd);
			streamableGraffiti.setUser(currentUser);
			currentUser.getStreamables().add(streamableGraffiti);
			return streamableGraffiti;
		});

		addStreamableToOwnAndFollowersStream(streamable);

		return streamable;
	}

	@Transactional
	public Streamable like(Long toLikeId) {
		Streamable toLike = findStreamableById(toLikeId);

		if (toLike != null) {
			User currentUser = userService.getCurrentUser();

			if (!toLike.isLikedBy(currentUser)) {
				userService.merge(currentUser);
				toLike.getLikers().add(currentUser);

				if (!toLike.getUser().equals(currentUser)) {
					// Send notification.
					notificationService.addLikeNotification(toLike.getUser(), currentUser, toLike);
				}
			}

			return toLike;
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + toLikeId + " not found");
		}
	}

	@Transactional
	public Streamable unlike(Long toUnlikeId) {
		Streamable toUnlike = findStreamableById(toUnlikeId);

		if (toUnlike != null) {
			User currentUser = userService.getCurrentUser();
			userService.merge(currentUser);
			toUnlike.getLikers().remove(currentUser);

			return toUnlike;
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + toUnlikeId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<UserDto> getLikersResult(Long streamableId, Integer offset, Integer count) {
		Streamable streamable = findStreamableById(streamableId);

		if (streamable != null) {
			Query query = streamableDao.createQuery(
					"select u "
				  + "from Streamable s "
				  + "join s.likers u "
				  + "where s = :currentStreamable");
			query.setParameter("currentStreamable", streamable);

			return pagingService.getPagedItems(User.class, UserDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getUserStreamablesResult(Long userId, Integer offset, Integer count) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = userDao.createQuery(
					"select s "
				  + "from User u "
				  + "join u.streamables s "
				  + "where u = :currentUser "
				  + "order by s.date desc");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getNewestStreamablesResult(Integer offset, Integer count) {
		Query query = streamableDao.createQuery(
				"select s "
			  + "from Streamable s "
			  + "order by s.date desc");

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> getPopularStreamablesResult(Integer offset, Integer count) {
		Query query = streamableDao.createQuery(
				"select s "
			  + "from Streamable s "
			  + "left join s.likers l "
			  + "group by s.id "
			  + "order by count(l) desc");

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> getUserFeedResult(Long userId, Integer offset, Integer count) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = userDao.createQuery(
					"select f "
				  + "from User u "
				  + "join u.feed f "
				  + "where u = :currentUser "
				  + "order by f.date desc");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional
	public Streamable flag(Long streamableId) {
		Streamable streamable = findStreamableById(streamableId);

		if (streamable != null) {
			streamable.setIsFlagged(true);

			// TODO: Potentially send an email to support saying that a streamable has been flagged and include it's ID and asset link.

			return streamable;
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional
	public Streamable makePublicOrPrivate(Long streamableId, boolean isPrivate) {
		Streamable streamable = findStreamableById(streamableId);

		if (streamable != null) {
			User currentUser = userService.getCurrentUser();

			if (currentUser.equals(streamable.getUser())) {
				streamable.setIsPrivate(isPrivate);
			}
			else {
				throw new RestApiException(ResultCode.USER_NOT_OWNER, "The streamable with id " + streamableId + " cannot be changed by user with id " + currentUser.getId());
			}

			return streamable;
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getLikedStreamablesForUserResult(Long userId, Integer offset, Integer count) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = streamableDao.createQuery(
					"select s "
				  + "from Streamable s "
				  + "join s.likers u "
				  + "where u = :currentUser "
				  + "order by s.date desc");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getPrivateStreamablesResult(Integer offset, Integer count) {
		User currentUser = userService.getCurrentUser();

		Query query = userDao.createQuery(
				"select s "
			  + "from User u "
			  + "join u.streamables s "
			  + "where u = :currentUser and s.isPrivate = 'Y' "
			  + "order by s.date desc");
		query.setParameter("currentUser", currentUser);

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> searchStreamablesAtLocationResult(Double neLatitude, Double neLongitude, Double swLatitude, Double swLongitude) {
		Query query = streamableDao.createQuery(
				"select s "
			  + "from Streamable s "
			  + "where s.latitude is not null and s.longitude is not null " // Check that the streamable has a location.
			  + "and s.latitude <= :neLatitude and s.latitude >= :swLatitude " // Check that the streamable is inside the required GPS rectangle.
			  + "and s.longitude >= :neLongitude and s.longitude <= :swLongitude "
			  + "order by s.date desc");
		query.setParameter("neLatitude", neLatitude);
		query.setParameter("swLatitude", swLatitude);
		query.setParameter("neLongitude", neLongitude);
		query.setParameter("swLongitude", swLongitude);

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, 0, PagingService.PAGE_SIZE_MAX_VALUE, query);
	}

	@Transactional
	public Boolean hashtagExistsForStreamable(String hashtag, Streamable streamable) {
		Query query = streamableDao.createQuery(
				"select count(*) "
			  + "from Streamable s "
			  + "join s.hashtags h "
			  + "where s = :currentStreamable and :tag in elements(h)");
		query.setParameter("currentStreamable", streamable);
		query.setParameter("tag", hashtag);

		Long resultCount = (Long) query.uniqueResult();
		return resultCount > 0;
	}

	@Transactional(readOnly = true)
	public Streamable findStreamableById(Long id) {
		return streamableDao.find(id);
	}

	@SuppressWarnings("unchecked")
	private void addStreamableToOwnAndFollowersStream(Streamable streamable) {
		User currentUser = userService.getCurrentUser();
		executor.submit(() -> {

			if (log.isDebugEnabled()) {
				log.debug("About to add streamable " + streamable + " to followers of user " + currentUser);
			}

			try {
				RunAsUser.set(currentUser);

				// Get list of followers.
				List<Long> followeesIds = transactionUtils.executeInTransactionWithResult(() -> {
					Query query = userDao.createQuery(
							"select f.id "
						  + "from User u "
						  + "join u.followers f "
						  + "where u = :currentUser");
					query.setParameter("currentUser", currentUser);
					List<Long> ids = (List<Long>) query.list();
					return ids;
				});

				// We want to add the streamable to the user's feed as well.
				followeesIds.add(currentUser.getId());

				if (log.isDebugEnabled()) {
					log.debug("Adding streamable to " + followeesIds.size() + " followers");
				}

				// For each follower, add the item to their feed.
				followeesIds.forEach(userId -> {
					transactionUtils.executeInTransaction(() -> {
						Streamable innerStreamable = findStreamableById(streamable.getId());
						User follower = userService.findUserById(userId);
						follower.getFeed().add(innerStreamable);
					});
				});

				if (log.isDebugEnabled()) {
					log.debug("Finished adding streamable to followers' feed");
				}
			} catch (Throwable t) {
				log.error("Error updating followers feed", t);
			} finally {
				RunAsUser.clear();
			}
		});
	}
}

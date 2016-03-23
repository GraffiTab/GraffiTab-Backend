package com.graffitab.server.service.streamable;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.streamable.StreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableGraffitiDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.asset.Asset.AssetType;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.streamable.StreamableGraffiti;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.paging.PagingService;
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
	private ActivityService activityService;

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

		// Update the current user stream and the streams of all his followers.
		addStreamableToOwnAndFollowersStream(streamable);

		// Add activity to all followers.
		activityService.addCreateStreamableActivityAsync(streamable.getUser(), streamable);

		return streamable;
	}

	public Streamable like(Long toLikeId) {
		User currentUser = userService.getCurrentUser();

		Pair<Streamable, Boolean> resultPair = transactionUtils.executeInTransactionWithResult(() -> {
			Streamable innerStreamable = findStreamableById(toLikeId);

			if (innerStreamable != null) {
				Boolean liked = false;

				if (!innerStreamable.isLikedBy(currentUser)) {
					User innerUser = userService.findUserById(currentUser.getId());
					innerStreamable.getLikers().add(innerUser);
					liked = true;
				}

				return new Pair<Streamable, Boolean>(innerStreamable, liked);
			} else {
				throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + toLikeId + " not found");
			}
		});

		Streamable streamable = resultPair.getValue0();
		Boolean liked = resultPair.getValue1();

		// Add notification to the owner of the streamable.
		if (liked && !streamable.getUser().equals(currentUser)) {
			notificationService.addLikeNotificationAsync(streamable.getUser(), currentUser, streamable);
		}

		if (liked) {
			// Add activity to each follower of the user.
			activityService.addLikeActivityAsync(currentUser, streamable);
		}

		return streamable;
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
			Query query = streamableDao.createNamedQuery("User.getLikers");
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
			Query query = userDao.createNamedQuery("Streamable.getUserStreamables");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getNewestStreamablesResult(Integer offset, Integer count) {
		Query query = streamableDao.createNamedQuery("Streamable.getNewestStreamables");

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> getPopularStreamablesResult(Integer offset, Integer count) {
		Query query = streamableDao.createNamedQuery("Streamable.getPopularStreamables");

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> getUserFeedResult(Long userId, Integer offset, Integer count) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = userDao.createNamedQuery("Streamable.getUserFeed");
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
			Query query = streamableDao.createNamedQuery("Streamable.getLikedStreamables");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getPrivateStreamablesResult(Integer offset, Integer count) {
		User currentUser = userService.getCurrentUser();

		Query query = userDao.createNamedQuery("Streamable.getPrivateStreamables");
		query.setParameter("currentUser", currentUser);

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> searchStreamablesAtLocationResult(Double neLatitude, Double neLongitude, Double swLatitude, Double swLongitude) {
		Query query = streamableDao.createNamedQuery("Streamable.searchStreamablesAtLocation");
		query.setParameter("neLatitude", neLatitude);
		query.setParameter("swLatitude", swLatitude);
		query.setParameter("neLongitude", neLongitude);
		query.setParameter("swLongitude", swLongitude);

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, 0, PagingService.PAGE_SIZE_MAX_VALUE, query);
	}

	@Transactional
	public ListItemsResult<StreamableDto> searchStreamablesForHashtagResult(String hashtag, Integer offset, Integer count) {
		// Filter out special characters to prevent SQL injection.
		hashtag = hashtag.toLowerCase() + "%";

		Query query = streamableDao.createNamedQuery("Streamable.searchStreamablesForHashtag");
		query.setParameter("tag", hashtag);

		return pagingService.getPagedItems(Streamable.class, StreamableDto.class, offset, count, query);
	}

	@Transactional
	public ListItemsResult<String> searchHashtags(String hashtag, Integer offset, Integer count) {
		// Filter out special characters to prevent SQL injection.
		hashtag = hashtag.toLowerCase() + "%";

		Query query = streamableDao.createNamedQuery("Streamable.searchHashtags");
		query.setParameter("tag", hashtag);

		return pagingService.getPagedItems(String.class, String.class, offset, count, query);
	}

	@Transactional
	public Boolean hashtagExistsForStreamable(String hashtag, Streamable streamable) {
		Query query = streamableDao.createNamedQuery("Streamable.hashtagExistsForStreamable");
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
					Query query = userDao.createNamedQuery("User.getFollowerIds");
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

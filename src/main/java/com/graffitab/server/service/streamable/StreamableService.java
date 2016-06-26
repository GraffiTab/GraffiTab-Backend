package com.graffitab.server.service.streamable;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableGraffitiDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.errors.EntityNotFoundException;
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
import com.graffitab.server.service.asset.AssetService;
import com.graffitab.server.service.asset.TransferableStream;
import com.graffitab.server.service.email.EmailService;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.store.DatastoreService;
import com.graffitab.server.service.user.UserService;
import org.hibernate.Query;
import org.javatuples.Pair;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class StreamableService {

	@Resource
	private UserService userService;

	@Resource
	private EmailService emailService;

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

	@Resource
	private AssetService assetService;

	@Transactional(readOnly = true)
	public Streamable getStreamable(Long id) {
		Streamable streamable = findStreamableById(id);

		if (streamable == null) {
			throw new EntityNotFoundException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + id + " not found");
		}

		return streamable;
	}

	public Streamable createStreamableGraffiti(StreamableGraffitiDto streamableGraffitiDto, TransferableStream transferable, long contentLength) {
		Asset assetToAdd = addStreamableAsset(transferable, contentLength);

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

		// Add activity to all followers.
		activityService.addCreateStreamableActivityAsync(streamable.getUser(), streamable);

		return streamable;
	}

	public Streamable editStreamableGraffiti(Long streamableId, StreamableGraffitiDto streamableGraffitiDto,
											 TransferableStream transferable, long contentLength) {

		Asset assetToAdd = addStreamableAsset(transferable, contentLength);

		Pair<Streamable, String> resultPair = transactionUtils.executeInTransactionWithResult(() -> {
			Streamable streamable = findStreamableById(streamableId);
			if (streamable != null) {
				User currentUser = userService.getCurrentUser();
				if (streamable.getUser().equals(currentUser)) {
					String previousStreamableAssetGuid = streamable.getAsset().getGuid();
					StreamableGraffiti streamableGraffiti = (StreamableGraffiti) streamable;
					streamableGraffiti.setAsset(assetToAdd);
					streamableGraffiti.setLatitude(streamableGraffitiDto.getLatitude());
					streamableGraffiti.setLongitude(streamableGraffitiDto.getLongitude());
					streamableGraffiti.setRoll(streamableGraffitiDto.getRoll());
					streamableGraffiti.setYaw(streamableGraffitiDto.getYaw());
					streamableGraffiti.setPitch(streamableGraffitiDto.getPitch());
					streamableGraffiti.setUpdatedOn(new DateTime());
					return new Pair<>(streamable, previousStreamableAssetGuid);
				}
				else {
					throw new RestApiException(ResultCode.USER_NOT_OWNER, "The streamable with id " + streamableId +
							" cannot be changed by user with id " + currentUser.getId());
				}
			} else {
				throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId +
											" not found");
			}
		});

		Streamable streamable = resultPair.getValue0();
		String previousStreamableAssetGuid = resultPair.getValue1();

		if (previousStreamableAssetGuid != null) {
			assetService.addPreviousAssetGuidMapping(assetToAdd.getGuid(), previousStreamableAssetGuid);
		}
		return streamable;
	}

	private Asset addStreamableAsset(TransferableStream transferable, long contentLength) {
		Asset assetToAdd = Asset.asset(AssetType.IMAGE);
		String assetGuid = assetService.transferAssetFile(transferable, contentLength);
		assetToAdd.setGuid(assetGuid);
		return assetToAdd;
	}

	@Transactional
	public void deleteStreamable(Long streamableId) {
		Streamable streamable = findStreamableById(streamableId);

		if (streamable != null) {
			User currentUser = userService.getCurrentUser();

			if (currentUser.equals(streamable.getUser())) {
				streamable.setIsDeleted(true);
			}
			else {
				throw new RestApiException(ResultCode.USER_NOT_OWNER, "The streamable with id " + streamableId + " cannot be changed by user with id " + currentUser.getId());
			}
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
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

	@Transactional(readOnly = true)
	public ListItemsResult<UserDto> getLikersResult(Long streamableId, Integer offset, Integer limit) {
		Streamable streamable = findStreamableById(streamableId);

		if (streamable != null) {
			Query query = streamableDao.createNamedQuery("User.getLikers");
			query.setParameter("currentStreamable", streamable);

			return pagingService.getPagedItems(User.class, UserDto.class, offset, limit, query);
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> getUserStreamablesResult(Long userId, Integer offset, Integer limit) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = userDao.createNamedQuery("Streamable.getUserStreamables");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> getNewestStreamablesResult(Integer offset, Integer limit) {
		Query query = streamableDao.createNamedQuery("Streamable.getNewestStreamables");

		return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
	}

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> getPopularStreamablesResult(Integer offset, Integer limit) {
		Query query = streamableDao.createNamedQuery("Streamable.getPopularStreamables");

		return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
	}

	public Streamable flag(Long streamableId) {
		Pair<Streamable, Boolean> resultPair = transactionUtils.executeInTransactionWithResult(() -> {
			Streamable innerStreamable = findStreamableById(streamableId);

			if (innerStreamable != null) {
				Boolean flagged = false;

				if (!innerStreamable.getIsFlagged()) {
					innerStreamable.setIsFlagged(true);
					flagged = true;
				}

				return new Pair<Streamable, Boolean>(innerStreamable, flagged);
			} else {
				throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
			}
		});

		Streamable streamable = resultPair.getValue0();
		Boolean wasFlagged = resultPair.getValue1();

		if (wasFlagged) {
			emailService.sendFlagEmail(streamable.getId(), datastoreService.generateDownloadLink(streamable.getAsset().getGuid()));
		}

		return streamable;
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

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> getLikedStreamablesForUserResult(Long userId, Integer offset, Integer limit) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = streamableDao.createNamedQuery("Streamable.getLikedStreamables");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> getPrivateStreamablesResult(Integer offset, Integer limit) {
		User currentUser = userService.getCurrentUser();

		Query query = userDao.createNamedQuery("Streamable.getPrivateStreamables");
		query.setParameter("currentUser", currentUser);

		return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
	}

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> searchStreamablesAtLocationResult(Double neLatitude, Double neLongitude, Double swLatitude, Double swLongitude) {
		Query query = streamableDao.createNamedQuery("Streamable.searchStreamablesAtLocation");
		query.setParameter("neLatitude", neLatitude);
		query.setParameter("swLatitude", swLatitude);
		query.setParameter("neLongitude", neLongitude);
		query.setParameter("swLongitude", swLongitude);

		return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, 0, PagingService.PAGE_SIZE_MAX_VALUE, query);
	}

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> searchStreamablesForHashtagResult(String hashtag, Integer offset, Integer limit) {
		// Filter out special characters to prevent SQL injection.
		hashtag = hashtag.toLowerCase() + "%";

		Query query = streamableDao.createNamedQuery("Streamable.searchStreamablesForHashtag");
		query.setParameter("tag", hashtag);

		return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
	}

	@Transactional(readOnly = true)
	public ListItemsResult<String> searchHashtags(String hashtag, Integer offset, Integer limit) {
		// Filter out special characters to prevent SQL injection.
		hashtag = hashtag.toLowerCase() + "%";

		Query query = streamableDao.createNamedQuery("Streamable.searchHashtags");
		query.setParameter("tag", hashtag);

		return pagingService.getPagedItems(String.class, String.class, offset, limit, query);
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

	@Transactional
	public void processStreamableStats(Streamable streamable, FullStreamableDto streamableDto) {
		Query query = streamableDao.createNamedQuery("Streamable.stats");
		query.setParameter("currentUser", userService.getCurrentUser());
		query.setParameter("streamable", streamable);
		Object[] result = (Object[]) query.uniqueResult();

		streamableDto.setCommentsCount((Long) result[0]);
		streamableDto.setLikersCount((Integer) result[1]);

		Long likedByCurrentUserInt = (Long) result[2];
		streamableDto.setLikedByCurrentUser(likedByCurrentUserInt == 1);

	}
}

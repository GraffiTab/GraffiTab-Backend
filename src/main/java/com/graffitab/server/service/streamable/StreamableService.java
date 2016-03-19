package com.graffitab.server.service.streamable;

import java.io.InputStream;

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
import com.graffitab.server.service.user.UserService;

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

	public Streamable createStreamableGraffiti(StreamableGraffitiDto streamableGraffitiDto, InputStream assetInputStream, long contentLength) {
		Asset assetToAdd = Asset.asset(AssetType.IMAGE);

		datastoreService.saveAsset(assetInputStream, contentLength, assetToAdd.getGuid());

		Streamable streamable = transactionUtils.executeInTransactionWithResult(() -> {
			User currentUser = userService.getCurrentUser();
			Streamable streamableGraffiti = new StreamableGraffiti(streamableGraffitiDto.getLatitude(),
														   streamableGraffitiDto.getLongitude(),
														   streamableGraffitiDto.getRoll(),
														   streamableGraffitiDto.getYaw(),
														   streamableGraffitiDto.getPitch());
			streamableGraffiti.setAsset(assetToAdd);
			currentUser.getStreamables().add(streamableGraffiti);

			return streamableGraffiti;
		});

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

			return pagingService.getPagedItemsResult(User.class, UserDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<StreamableDto> getUserStreamables(Long userId, Integer offset, Integer count) {
		User user = userService.findUserById(userId);

		if (user != null) {
			Query query = userDao.createQuery(
					"select s "
				  + "from User u "
				  + "join u.streamables s "
				  + "where u = :currentUser");
			query.setParameter("currentUser", user);

			return pagingService.getPagedItemsResult(Streamable.class, StreamableDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.USER_NOT_FOUND, "User with id " + userId + " not found");
		}
	}

	@Transactional(readOnly = true)
	public Streamable findStreamableById(Long id) {
		return streamableDao.find(id);
	}
}

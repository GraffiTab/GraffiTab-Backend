package com.graffitab.server.service.notification;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.notification.NotificationComment;
import com.graffitab.server.persistence.model.notification.NotificationFollow;
import com.graffitab.server.persistence.model.notification.NotificationLike;
import com.graffitab.server.persistence.model.notification.NotificationWelcome;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.service.PagingService;
import com.graffitab.server.service.user.UserService;

@Service
public class NotificationService {

	private static final Logger log = LogManager.getLogger();

	@Resource
	private UserService userService;

	@Resource
	private PagingService pagingService;

	@Resource
	private HibernateDaoImpl<Notification, Long> notificationDao;

	@Resource
	private NotificationSenderService notificationSenderService;

	@Resource
	private OrikaMapper mapper;

	@Transactional
	public ListItemsResult<NotificationDto> getNotificationsResult(Integer offset, Integer count) {
		User currentUser = userService.getCurrentUser();

		Query query = notificationDao.createQuery(
				"select n "
			  + "from User u "
			  + "join u.notifications n "
			  + "where u = :currentUser");
		query.setParameter("currentUser", currentUser);

		return pagingService.getPagedItemsResult(Notification.class, NotificationDto.class, offset, count, query);
	}

	@Transactional
	public void addWelcomeNotification(User user) {
		Notification notification = new NotificationWelcome();
		user.getNotifications().add(notification);

		sendNotificationAsync(user, notification);
	}

	@Transactional
	public void addFollowNotification(User user, User follower) {
		Notification notification = new NotificationFollow(follower);
		user.getNotifications().add(notification);

		sendNotificationAsync(user, notification);
	}

	@Transactional
	public void addLikeNotification(User user, User liker, Streamable likedStreamable) {
		Notification notification = new NotificationLike(liker, likedStreamable);
		user.getNotifications().add(notification);

		sendNotificationAsync(user, notification);
	}

	@Transactional
	public void addCommentNotification(User user, User commenter, Streamable commentedStreamable, Comment comment) {
		Notification notification = new NotificationComment(commenter, commentedStreamable, comment);
		userService.merge(user);
		user.getNotifications().add(notification);
		sendNotificationAsync(user, notification);
	}

	private void sendNotificationAsync(User receiver, Notification notification) {
		log.debug("About to send push notification to user " + receiver.getUsername());
		try {
			notificationSenderService.sendNotification(receiver, notification);
		} catch (Throwable t) {
			log.error("Error sending push notification", t);
		}
	}
}

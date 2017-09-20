package com.graffitab.server.service.notification;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.PagedList;
import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.notification.NotificationComment;
import com.graffitab.server.persistence.model.notification.NotificationFollow;
import com.graffitab.server.persistence.model.notification.NotificationLike;
import com.graffitab.server.persistence.model.notification.NotificationMention;
import com.graffitab.server.persistence.model.notification.NotificationWelcome;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.user.UserService;
import lombok.extern.log4j.Log4j;
import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j
@Service
public class NotificationService {

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

	@Resource
	private TransactionUtils transactionUtils;

	private ExecutorService executor = Executors.newFixedThreadPool(2);

	public ListItemsResult<NotificationDto> getNotificationsResult(Integer offset, Integer limit) {
		// Get original notifications.
		PagedList<Notification> notifications = transactionUtils.executeInTransactionWithResult(() -> {
			User currentUser = userService.getCurrentUser();

			Query query = notificationDao.createNamedQuery("Notification.getNotifications");
			query.setParameter("currentUser", currentUser);

			return pagingService.getItems(query, offset, limit);
		});

		// Map list so that it can be returned later.
		ListItemsResult<NotificationDto> mappedList = pagingService.mapResults(notifications, NotificationDto.class);

		// Mark retrieved notifications as read.
		markNotificationsAsRead(notifications);

		// Return original list of unread notifications.
		return mappedList;
	}

	@Transactional(readOnly = true)
	public Long getUnreadNotificationsCount() {
		User currentUser = userService.getCurrentUser();

		Query query = notificationDao.createNamedQuery("Notification.getUnreadNotificationsCount");
		query.setParameter("currentUser", currentUser);

		return (Long) query.uniqueResult();
	}

	public void addWelcomeNotificationAsync(User user) {
		Notification notification = new NotificationWelcome();
		addNotificationToUser(user, notification);
	}

	public void addFollowNotificationAsync(User user, User follower) {
		Notification notification = new NotificationFollow(follower);
		addNotificationToUser(user, notification);
	}

	public void addLikeNotificationAsync(User user, User liker, Streamable likedStreamable) {
		Notification notification = new NotificationLike(liker, likedStreamable);
		addNotificationToUser(user, notification);
	}

	public void addCommentNotificationAsync(User user, User commenter, Streamable commentedStreamable, Comment comment) {
		Notification notification = new NotificationComment(commenter, commentedStreamable, comment);
		addNotificationToUser(user, notification);
	}

	public void addMentionNotificationAsync(User user, User mentioner, Streamable mentionedStreamable, Comment comment) {
		Notification notification = new NotificationMention(mentioner, mentionedStreamable, comment);
		addNotificationToUser(user, notification);
	}

	public void addMentionNotificationAsync(User user, User mentioner, Streamable mentionedStreamable) {
		Notification notification = new NotificationMention(mentioner, mentionedStreamable);
		addNotificationToUser(user, notification);
	}

	private void markNotificationsAsRead(List<Notification> notifications) {
		executor.submit(() -> {
			if (log.isDebugEnabled()) {
				log.debug("About to mark " + notifications.size() + " notifications as read");
			}

			notifications.forEach(notification -> {
				transactionUtils.executeInTransaction(() -> {
					notification.setIsRead(true);
					notificationDao.merge(notification);
				});
			});

			if (log.isDebugEnabled()) {
				log.debug("Finished marking notifications as unread");
			}
		});
	}

	private void addNotificationToUser(User receiver, Notification notification) {
		executor.submit(() -> {
			if (log.isDebugEnabled()) {
				log.debug("About to add notification " + notification + " to user " + receiver);
			}

			// Add notification to receiver.
			transactionUtils.executeInTransaction(() -> {
				User inner = userService.findUserById(receiver.getId());
				inner.getNotifications().add(notification);
			});

			// Send push notification to receiver.
			notificationSenderService.sendNotification(receiver, notification);

			if (log.isDebugEnabled()) {
				log.debug("Finished adding notification");
			}
		});
	}
}

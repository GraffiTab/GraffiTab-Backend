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
import com.graffitab.server.service.job.JobService;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.user.UserService;
import lombok.extern.log4j.Log4j;
import org.hibernate.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

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

	@Resource
	private JobService jobService;

    @Transactional(readOnly = true)
    public Notification findNotificationById(Long id) {
        return notificationDao.find(id);
    }

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

	public void addWelcomeNotification(User user,
                                       boolean synchronous) {
		Notification notification = new NotificationWelcome();
		addNotificationToUser(user, notification, synchronous);
	}

	public void addFollowNotification(User user,
                                      User follower,
                                      boolean synchronous) {
		Notification notification = new NotificationFollow(follower);
		addNotificationToUser(user, notification, synchronous);
	}

	public void addLikeNotification(User user,
                                    User liker,
                                    Streamable likedStreamable,
                                    boolean synchronous) {
		Notification notification = new NotificationLike(liker, likedStreamable);
		addNotificationToUser(user, notification, synchronous);
	}

	public void addCommentNotification(User user,
                                       User commenter,
                                       Streamable commentedStreamable,
                                       Comment comment,
                                       boolean synchronous) {
		Notification notification = new NotificationComment(commenter, commentedStreamable, comment);
		addNotificationToUser(user, notification, synchronous);
	}

	public void addMentionNotification(User user,
                                       User mentioner,
                                       Streamable mentionedStreamable,
                                       Comment comment,
                                       boolean synchronous) {
		Notification notification = new NotificationMention(mentioner, mentionedStreamable, comment);
		addNotificationToUser(user, notification, synchronous);
	}

	public void addMentionNotification(User user,
                                       User mentioner,
                                       Streamable mentionedStreamable,
                                       boolean synchronous) {
		Notification notification = new NotificationMention(mentioner, mentionedStreamable);
		addNotificationToUser(user, notification, synchronous);
	}

	private void addNotificationToUser(User receiver,
                                       Notification notification,
                                       boolean synchronous) {
		Runnable runnable = () -> {
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
        };
		if (!synchronous) {
            jobService.execute(runnable);
        } else {
		    runnable.run();
        }
	}

    private void markNotificationsAsRead(List<Notification> notifications) {
        if (unreadNotificationsExist(notifications)) {
            // Mark notifications as unread only if there are unread notifications in this list.
            jobService.execute(() -> {
                if (log.isDebugEnabled()) {
                    log.debug("About to check " + notifications.size() + " notifications");
                }

                int markedCount = 0;
                for (Notification notification : notifications) {
                    if (!notification.getIsRead()) {
                        // Process only unread notifications.
                        if (log.isDebugEnabled()) {
                            log.debug("Marking notification id=" + notification.getId() + " as read");
                        }
                        transactionUtils.executeInTransaction(() -> {
                            Notification inner = findNotificationById(notification.getId());
                            inner.setIsRead(true);
                        });
                        markedCount++;
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("Finished marking " + markedCount + " notifications as read");
                }
            });
        } else if (log.isDebugEnabled()) {
            log.debug("No unread notifications to mark. Nothing to do");
        }
    }

	private boolean unreadNotificationsExist(List<Notification> notifications) {
        for (Notification notification : notifications) {
            if (!notification.getIsRead())
                return true;
        }
        return false;
    }
	
	public void markAllNotificationsAsReadForCurrentUser() {
		//TODO:
		// Get current user
		// Update all notifications in DB (in the last month) to READ
		// return
	}

	public void markNotificationAsRead(Long notificationId) {
		//TODO:
		// update the notification to READ for the current user
	}
}

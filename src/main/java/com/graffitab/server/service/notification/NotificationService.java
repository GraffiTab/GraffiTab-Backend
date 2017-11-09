package com.graffitab.server.service.notification;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
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
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.hibernate.Query;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Service
@AllArgsConstructor
public class NotificationService {

    private UserService userService;
    private PagingService pagingService;
    private HibernateDaoImpl<Notification, Long> notificationDao;
    private NotificationSenderService notificationSenderService;
    private OrikaMapper mapper;
    private TransactionUtils transactionUtils;
    private JobService jobService;

    private static final DateTime ONE_MONTH_AGO = new DateTime().minusMonths(1);

    @Transactional
    public void markAllNotificationsAsReadForCurrentUser() {
        // No more than 10000 will be marked in the past month
        List<Notification> notifications = findNotificationsForCurrentUser(0, 10000);
        markGroupNotificationsAsRead(notifications);
    }

    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        if (notificationId == null) {
            throw new RestApiException(ResultCode.INVALID_ID,"Notification ID is null");
        }
        markSingleNotificationAsRead(findNotificationById(notificationId));
    }

    @Transactional(readOnly = true)
    public Notification findNotificationById(Long id) {
        return notificationDao.find(id);
    }

    public ListItemsResult<NotificationDto> getNotificationsResult(Integer offset, Integer limit) {
        // Get original notifications.
        PagedList<Notification> notifications = findNotificationsForCurrentUser(offset, limit);

        // Map list
        return pagingService.mapResults(notifications, NotificationDto.class);
    }

    public PagedList<Notification> findNotificationsForCurrentUser(Integer offset, Integer limit) {
        return transactionUtils.executeInTransactionWithResult(() -> {
            User currentUser = userService.getCurrentUser();
            Query query = notificationDao.createNamedQuery("Notification.getNotifications");
            query.setParameter("currentUser", currentUser);
            query.setParameter("createdOn", ONE_MONTH_AGO);
            return pagingService.getItems(query, offset, limit);
        });
    }

    @Transactional(readOnly = true)
    public Long getUnreadNotificationsCount() {
        User currentUser = userService.getCurrentUser();
        Query query = notificationDao.createNamedQuery("Notification.getUnreadNotificationsCount");
        query.setParameter("currentUser", currentUser);
        query.setParameter("createdOn", ONE_MONTH_AGO);
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
                log.debug("About to check " + notifications.size() + " notifications");
                List<Notification> markedNotifications = markGroupNotificationsAsRead(notifications);
                log.debug("Finished marking " + markedNotifications.size() + " notifications as read");
            });
        } else if (log.isDebugEnabled()) {
            log.debug("No unread notifications to mark. Nothing to do");
        }
    }

    private boolean unreadNotificationsExist(List<Notification> notifications) {
        return notifications.stream().anyMatch(notification -> !notification.getIsRead());
    }

    private List<Notification> markGroupNotificationsAsRead(List<Notification> notifications) {
        List<Notification> markedNotifications = notifications.stream()
                .filter(notification -> !notification.getIsRead())
                .map(this::markSingleNotificationAsRead)
                .collect(Collectors.toList());
        log.info("Marked " + markedNotifications.size() + " notifications as read");
        return markedNotifications;
    }

    private Notification markSingleNotificationAsRead(Notification notification) {
        return transactionUtils.executeInTransactionWithResult(() -> {
            Notification inner = findNotificationById(notification.getId());
            inner.setIsRead(true);
            return inner;
        });
    }
}

package com.graffitab.server.service;

import com.graffitab.server.GraffitabApplication;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.notification.NotificationWelcome;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.notification.NotificationSenderService;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.user.RunAsUser;
import com.graffitab.server.service.user.UserService;
import com.graffitab.server.test.api.TestDatabaseConfig;
import com.graffitab.server.util.GuidGenerator;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Created by david on 14/11/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestDatabaseConfig.class, GraffitabApplication.class})
@ActiveProfiles("unit-test")
public class NotificationServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private NotificationSenderService notificationSenderService;

    @Autowired
    private HibernateDaoImpl<User, Long> userDao;

    @Autowired
    private HibernateDaoImpl<Notification, Long> notificationDao;

    @Autowired
    private TransactionUtils transactionUtils;

    @Autowired
    private NotificationService notificationService;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        RunAsUser.clear();
    }

    @Test
    public void shouldMarkAllNotificationsAsReadForCurrentUser() {
        // Given
        User aUser = aUserExistsAsCurrentUserWithDisabledNotificationSending();

        // And some unread notifications exist
        insertFiveNotifications(aUser);
        validateUnread(5L);

        // when
        notificationService.markAllNotificationsAsReadForCurrentUser();

        // then
        noUnreadNotificationsExist();
    }

    @Test
    public void shouldMarkSingleNotificationAsRead() {
        // Given
        User aUser = aUserExistsAsCurrentUserWithDisabledNotificationSending();

        // And some notifications exist
        insertFiveNotifications(aUser);
        validateUnread(5L);

        // When
        List<Notification> notifications = notificationService.findNotificationsForCurrentUser(0, 1);
        notificationService.markNotificationAsRead(notifications.get(0).getId());

        // Then
        validateUnread(4L);
    }

    @Test
    public void unreadNotificationsForUserAreEmptyIfOlderThanOneMonth() {
        unreadNotificationsCountTest(new DateTime().minusMonths(2),0L);
    }

    @Test
    public void unreadNotificationsForUserAreNotEmptyIfNewerThanOneMonth() {
        unreadNotificationsCountTest(new DateTime().minusWeeks(2),1L);
    }

    private void unreadNotificationsCountTest(DateTime dateTime, Long expectedUnreadNotifications) {
        // Given
        User aUser = aUserExistsAsCurrentUserWithDisabledNotificationSending();
        insertNotificationWithFixedDate(aUser, dateTime);

        // When getting unread for the user
        Long unreadNotifications = notificationService.getUnreadNotificationsCount();

        // Then
        Assert.assertEquals(unreadNotifications, expectedUnreadNotifications);
    }

    private void insertFiveNotifications(User aUser) {
        notificationService.addWelcomeNotification(aUser, true);
        notificationService.addWelcomeNotification(aUser, true);
        notificationService.addWelcomeNotification(aUser, true);
        notificationService.addWelcomeNotification(aUser, true);
        notificationService.addWelcomeNotification(aUser, true);
    }

    private User aUserExistsAsCurrentUserWithDisabledNotificationSending() {
        User aUser = persistUser();
        RunAsUser.set(aUser);
        doNothing().when(notificationSenderService).sendNotification(any(), any());
        return aUser;
    }

    private void validateUnread(Long number) {
        assertThat(notificationService.getUnreadNotificationsCount().equals(number));
    }

    private void noUnreadNotificationsExist() {
        assertThat(notificationService.getUnreadNotificationsCount() == 0L);
    }

    private User persistUser() {
        User aUser = User.builder()
                .guid(GuidGenerator.generate())
                .username("john")
                .password("aPassword")
                .firstName("John")
                .lastName("Doe")
                .createdOn(new DateTime())
                .accountStatus(User.AccountStatus.ACTIVE)
                .isRecommendation(false)
                .notifications(new ArrayList<>())
                .email("john.doe@mailinator.com").build();
        return transactionUtils.executeInTransactionWithResult(() -> {
            userDao.persist(aUser);
            return aUser;
        });
    }

    private Notification insertNotificationWithFixedDate(User aUser, DateTime dateTime) {
        Notification aNotification = NotificationWelcome.builder().build();
        aNotification.setCreatedOn(dateTime);
        return transactionUtils.executeInTransactionWithResult(()-> {
            User theUser = userDao.find(aUser.getId());
            theUser.getNotifications().add(aNotification);
            return aNotification;
        });
    }

    private Notification insertNotificationAsMoreThanOneMonthAgo(User aUser) {
        return insertNotificationWithFixedDate(aUser, new DateTime().minusMonths(3));
    }
}



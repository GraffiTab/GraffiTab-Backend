package com.graffitab.server.service;

import com.graffitab.server.GraffitabApplication;
import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.user.UserTestHelper;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.user.RunAsUser;
import com.graffitab.server.service.user.UserService;
import com.graffitab.server.test.api.TestDatabaseConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import static org.mockito.Matchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestDatabaseConfig.class, GraffitabApplication.class})
@ActiveProfiles("unit-test")
public class NotificationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private Filter springSecurityFilterChain;

    @SpyBean
    private UserService userService;

    @InjectMocks
    @Autowired
    private UserTestHelper userTestHelper;

    @InjectMocks
    @Autowired
    private NotificationService notificationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).addFilters(springSecurityFilterChain).build();
        Mockito.doReturn("encoded").when(userService).encodePassword(anyString());
    }

    @Test
    public void shouldMarkSingleNotificationAsRead() throws Exception {
        //given

        User currentUser = userTestHelper.createUser();
        RunAsUser.set(currentUser);
       // Mockito.doReturn(currentUser).when(userService).getCurrentUser();
       // Mockito.doReturn().when(userService).findUserById(anyLong());

        addNotificationToUser(currentUser);
        Long notificationId = findInsertedNotificationId();

        mockMvc.perform(post("/notifications"+notificationId+"/read")
                .with(user(currentUser)))
                .andExpect(status().is(200))
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.result").value("OK"));

        RunAsUser.clear();

    }

    private void addNotificationToUser(User user) {
        notificationService.addWelcomeNotification(user, true);
    }

    private  Long findInsertedNotificationId() {
        ListItemsResult<NotificationDto> notificationsResult =  notificationService.getNotificationsResult(0, 1);
        return notificationsResult.getItems().get(0).getId();
    }
}

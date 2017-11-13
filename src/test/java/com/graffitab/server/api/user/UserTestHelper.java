package com.graffitab.server.api.user;

import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.user.UserService;
import com.graffitab.server.util.GuidGenerator;
import lombok.AllArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static junit.framework.TestCase.fail;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by david on 23/05/2017.
 */
@Component
@AllArgsConstructor
@ActiveProfiles("unit-test")
public class UserTestHelper {

    private HibernateDaoImpl<User, Long> userDao;
    private TransactionUtils transactionUtils;
    private UserService userService;

    private User fillTestUser() {
        User testUser = new User();
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@mailinator.com");
        testUser.setUsername("johnd");
        testUser.setPassword(userService.encodePassword("password"));
        testUser.setAccountStatus(User.AccountStatus.ACTIVE);
        testUser.setCreatedOn(new DateTime());
        testUser.setGuid(GuidGenerator.generate());
        return testUser;
    }

    public User createUser() {
        User u = transactionUtils.executeInTransactionWithResult(() -> {
            User testUser = fillTestUser();
            userDao.persist(testUser);
            return testUser;
        });
        return u;
    }

    public static void pollForAsset(MockMvc mockMvc, User loggedInUser, String assetGuid, Long timeoutInMillis) throws Exception {
        Long startTime = System.currentTimeMillis();
        String json = "";

        while (!json.contains("\"state\":\"COMPLETED\"") && !isTimeout(startTime, timeoutInMillis)) {
            MvcResult result = mockMvc.perform(get("/api/assets/" + assetGuid + "/progress")
                    .with(user(loggedInUser))
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json;charset=UTF-8")).andReturn();
            json = result.getResponse().getContentAsString();
            System.out.println("==> " + json);
            if (!json.contains("\"state\":\"PROCESSING\"") && !json.contains("\"state\":\"COMPLETED\"")) {
                fail("Response does not contain expected values of state: " + json);
            }
            Thread.sleep(1000);
        }

        if (!json.contains("\"state\":\"COMPLETED\"")) {
            fail("Response does not contain expected values of state: " + json);
        }
    }

    private static boolean isTimeout(Long startTime, Long timeoutMillis) {
        return System.currentTimeMillis() - startTime >= timeoutMillis;
    }
}

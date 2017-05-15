package com.graffitab.server.api.user;

import com.graffitab.server.persistence.model.user.User;
import org.springframework.http.MediaType;
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
public class UserTestHelper {

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

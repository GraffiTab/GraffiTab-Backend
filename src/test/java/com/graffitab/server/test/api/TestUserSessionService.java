package com.graffitab.server.test.api;

import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.redis.UserSessionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Created by david on 14/05/2017.
 */
@Profile("unit-test")
@Service
public class TestUserSessionService implements UserSessionService {
    @Override
    public void logoutEverywhere(User user, boolean keepCurrentSession) {
        //TODO:
    }
}

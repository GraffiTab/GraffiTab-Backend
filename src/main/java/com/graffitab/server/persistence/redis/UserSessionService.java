package com.graffitab.server.persistence.redis;

import com.graffitab.server.persistence.model.user.User;

/**
 * Created by david on 14/05/2017.
 */
public interface UserSessionService {
    void logoutEverywhere(User user, boolean keepCurrentSession);
}

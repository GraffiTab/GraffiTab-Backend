package com.graffitab.server.test.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

/**
 * Created by david on 14/05/2017.
 */
@Configuration
@Profile("unit-test")
@EnableSpringHttpSession
public class MapSessionConfig {
    @Bean
    public SessionRepository sessionRepository() {
        return new MapSessionRepository();
    }
}


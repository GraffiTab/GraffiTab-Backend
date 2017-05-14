package com.graffitab.server.config.spring;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Created by davidfernandez on 01/07/2016.
 */
@Log4j2
@Profile("main")
@EnableRedisHttpSession
public class RedisSessionConfig {

    @Value("${server.session.timeout}")
    private int maxInactiveIntervalInSeconds;

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Bean
    public JedisConnectionFactory connectionFactory() {

        log.info("Connected to Redis: host -> {}, port -> {}, password -> ****", redisHost, redisPort);

        // Check Redis sentinel as HA option
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        jedisConnectionFactory.setPort(redisPort);
        jedisConnectionFactory.setPassword(redisPassword);
        return jedisConnectionFactory;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        return serializer;
    }

    /**
     * This overrides the default entity created through auto-configuration so that a custom value
     * for the session expiration can be set up. It is ensured that this bean is instantiated first through
     * the @Primary annotation
     * @param redisConnectionFactory
     * @return
     */
    @Primary
    @Bean
    public RedisOperationsSessionRepository sessionRepository(RedisConnectionFactory redisConnectionFactory) {
        RedisOperationsSessionRepository sessionRepository = new RedisOperationsSessionRepository(redisConnectionFactory);
        sessionRepository.setDefaultMaxInactiveInterval(maxInactiveIntervalInSeconds);
        return sessionRepository;
    }
}

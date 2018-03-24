package com.graffitab.server.config.spring.security;

import com.graffitab.server.api.authentication.CommonAuthenticationEntryPoint;
import com.graffitab.server.api.authentication.ExternalProviderAuthenticationFilter;
import com.graffitab.server.api.authentication.JsonLoginAuthenticationFilter;
import com.graffitab.server.api.authentication.JsonLoginFailureHandler;
import com.graffitab.server.api.authentication.JsonResponseLoginSuccessHandler;
import com.graffitab.server.api.authentication.PersistedSessionSecurityContext;
import com.graffitab.server.api.authentication.ProtocolCheckingFilter;
import com.graffitab.server.api.authentication.SessionInvalidationFilter;
import com.graffitab.server.api.authentication.UsernamePasswordQueryParamsAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@Order(5) // one more than the latest block in GraffitabSecurityConfig
public class SecurityBeansConfig extends WebSecurityConfigurerAdapter {

	@Bean
	public JsonResponseLoginSuccessHandler storeSessionJsonLoginSuccessHandler() {
		return new JsonResponseLoginSuccessHandler(true);
	}

	@Bean
	public JsonResponseLoginSuccessHandler statelessJsonLoginSuccessHandler() {
		return new JsonResponseLoginSuccessHandler(false);
	}

	@Bean
    public JsonLoginAuthenticationFilter jsonAuthenticationFilter() throws Exception {
        JsonLoginAuthenticationFilter authFilter = new JsonLoginAuthenticationFilter();
        authFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/v1/login","POST"));
        authFilter.setAuthenticationManager(authenticationManager());

        // Custom success handler - send 200 OK
        authFilter.setAuthenticationSuccessHandler(storeSessionJsonLoginSuccessHandler());

        // Custom failure handler - send 401 unauthorized
        authFilter.setAuthenticationFailureHandler(jsonLoginFailureHandler());
        authFilter.setUsernameParameter("username");
        authFilter.setPasswordParameter("password");
        return authFilter;
    }

	@Bean
	public AuthenticationEntryPoint commonAuthenticationEntryPoint() {
		AuthenticationEntryPoint commonEntryPoint = new CommonAuthenticationEntryPoint();
		return commonEntryPoint;
	}

	@Bean
	public SessionInvalidationFilter invalidateSessionFilter() {
		return new SessionInvalidationFilter(commonAuthenticationEntryPoint());
	}

	@Bean
	public ExternalProviderAuthenticationFilter externalProviderAuthenticationFilter() throws Exception {
		ExternalProviderAuthenticationFilter externalProviderFilter =
												new ExternalProviderAuthenticationFilter();
		externalProviderFilter.setAuthenticationManager(authenticationManager());
		externalProviderFilter.setAuthenticationSuccessHandler(storeSessionJsonLoginSuccessHandler());
		externalProviderFilter.setAuthenticationFailureHandler(jsonLoginFailureHandler());
		return externalProviderFilter;
	}

	@Bean
	public JsonLoginFailureHandler jsonLoginFailureHandler() {
		return new JsonLoginFailureHandler();
	}

	@Bean
    public UsernamePasswordQueryParamsAuthenticationFilter usernamePasswordQueryParamsAuthenticationFilter() throws Exception {
		UsernamePasswordQueryParamsAuthenticationFilter authFilter = new UsernamePasswordQueryParamsAuthenticationFilter();
		authFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/v1/**"));
		authFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/v2/**"));
		authFilter.setAuthenticationManager(authenticationManager());

        // Custom success handler - send 200 OK
        authFilter.setAuthenticationSuccessHandler(statelessJsonLoginSuccessHandler());

        // Custom failure handler - send 401 unauthorized
        authFilter.setAuthenticationFailureHandler(jsonLoginFailureHandler());
        authFilter.setUsernameParameter("username");
        authFilter.setPasswordParameter("password");
        return authFilter;
    }

	@Bean
	public PersistedSessionSecurityContext securityContextRepository() {
		PersistedSessionSecurityContext persistedSessionSecurityContext = new PersistedSessionSecurityContext();
		persistedSessionSecurityContext.setAllowSessionCreation(false);
		return persistedSessionSecurityContext;
	}

	@Bean
	public ProtocolCheckingFilter protocolCheckingFilter() {
		return new ProtocolCheckingFilter();
	}
}

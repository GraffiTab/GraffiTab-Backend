package com.graffitab.server.config.spring.security;

import com.graffitab.server.api.authentication.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SpringBootWebSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import com.graffitab.server.service.GraffiTabUserDetailsService;

import lombok.extern.log4j.Log4j2;


@Configuration
@EnableWebSecurity
@EnableAutoConfiguration(exclude={SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import(SecurityBeansConfig.class)
@Log4j2
public class GraffitabSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(daoAuthenticationProvider());
	}

	@Bean
	public UserDetailsService graffiTabUserDetailsService() {
		return new GraffiTabUserDetailsService();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
	    authProvider.setUserDetailsService(userDetailsService());
	    authProvider.setPasswordEncoder(passwordEncoder());
	    return authProvider;
	}

	@Override
	protected UserDetailsService userDetailsService() {
		return graffiTabUserDetailsService();
	}

	@Override
    public void configure(WebSecurity web) throws Exception {
      web
        .ignoring()
           .antMatchers("/favicon.ico","/resources/**", "/public/**");
    }

	@Configuration
    @Order(1)
    public static class LoginEndpointWebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Autowired
		private JsonLoginAuthenticationFilter jsonAuthenticationFilter;

		@Autowired
		private ExternalProviderAuthenticationFilter externalProviderAuthenticationFilter;

		@Autowired
		private AuthenticationEntryPoint commonAuthenticationEntryPoint;

		@Autowired
		private ProtocolCheckingFilter protocolCheckingFilter;

        @Override
        protected void configure(HttpSecurity http) throws Exception {

        	// We allow anonymous access here (by not disabling it). This means that if a request matches
        	// and it is not authenticated (anonymous) we let it pass -- this is what we want for login
            http.csrf().disable()
                  .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/api/login", "/api/externalproviders/login")
                    .and()
                    .authorizeRequests()
                    .anyRequest()
                    .permitAll()
                    .and()
            	    .sessionManagement()
            	    	.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

            http.addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            http.addFilterBefore(externalProviderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
			http.addFilterBefore(protocolCheckingFilter, JsonLoginAuthenticationFilter.class);
            http.exceptionHandling().authenticationEntryPoint(commonAuthenticationEntryPoint);
        }
	}

	@Configuration
    @Order(2)
    public static class PublicEndpointsSecurityConfig extends WebSecurityConfigurerAdapter {

		@Autowired
		private JsonLoginAuthenticationFilter jsonAuthenticationFilter;

		@Autowired
		private AuthenticationEntryPoint commonAuthenticationEntryPoint;

		@Autowired
		private ProtocolCheckingFilter protocolCheckingFilter;

        @Override
        protected void configure(HttpSecurity http) throws Exception {

        	// We allow anonymous access here (by not disabling it). This means that if a request matches
        	// and it is not authenticated (anonymous) we let it pass -- this is what we want for login and
        	// register endpoints
            http.csrf().disable()
                  .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/api/users", "/api/users/resetpassword", "/api/users/externalproviders", "/api/feedback")
                    .antMatchers(HttpMethod.GET, "/api/users/activate/**", "/api/auth/**")
                    .antMatchers(HttpMethod.PUT, "/api/users/resetpassword/**")
                    .and()
                    .authorizeRequests()
                    .anyRequest()
                    .permitAll()
                    .and()
            	    .sessionManagement()
            	    .sessionCreationPolicy(SessionCreationPolicy.NEVER);

			http.addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            http.addFilterBefore(protocolCheckingFilter, JsonLoginAuthenticationFilter.class);
            http.exceptionHandling().authenticationEntryPoint(commonAuthenticationEntryPoint);

        }
	}

	@Configuration
    @Order(3)
    public static class SessionAndBasicAuthSecurityConfig extends WebSecurityConfigurerAdapter {

		@Value("${basicAuth.enabled:true}")
		private String basicAuthEnabled;

		@Autowired
		private JsonLoginAuthenticationFilter jsonAuthenticationFilter;

		@Autowired
		private UsernamePasswordQueryParamsAuthenticationFilter usernamePasswordQueryParamsAuthenticationFilter;

		@Autowired
		private SessionInvalidationFilter invalidateSessionFilter;

		@Autowired
		private ProtocolCheckingFilter protocolCheckingFilter;

		@Autowired
		private AuthenticationEntryPoint commonAuthenticationEntryPoint;

		@Autowired
		private PersistedSessionSecurityContext securityContextRepository;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .anonymous().disable()
                    .securityContext()
                    	.securityContextRepository(securityContextRepository)
                    .and()
                    .antMatcher("/api/**")
            	    .sessionManagement()
            	    	.sessionCreationPolicy(SessionCreationPolicy.NEVER)
            	    .and()
                    .authorizeRequests()
                        .anyRequest().hasAnyRole("ADMIN", "USER")
                    .and()
                    .logout()
                       .deleteCookies("JSESSIONID").invalidateHttpSession(true)
 				       .logoutUrl("/api/logout").logoutSuccessHandler(new OkResponseLogoutHandler());

            // Add the invalidation session filter after this check, as it could be creating a new session
            http.addFilterAfter(invalidateSessionFilter, SecurityContextPersistenceFilter.class);

            // Add the custom authentication filter before the regular one
            http.addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

			http.addFilterBefore(protocolCheckingFilter, JsonLoginAuthenticationFilter.class);

            Boolean basicAuthenticationEnabled = Boolean.parseBoolean(basicAuthEnabled);
            if (basicAuthenticationEnabled) {
            	if (log.isDebugEnabled()) {
            		log.debug("Basic and URL parameters Authentication will be enabled");
            	}
	            // Add the basic auth filter before the jsonLogin filter (check first)
	            http.addFilterBefore(new SessionPrecedenceBasicAuthFilter(authenticationManager(), commonAuthenticationEntryPoint),
	            		    JsonLoginAuthenticationFilter.class);

	            // Also add the username / password as query parameters check before basic authentication
	            http.addFilterBefore(usernamePasswordQueryParamsAuthenticationFilter, SessionPrecedenceBasicAuthFilter.class);
            }

            // Common entry points: 401 Unauthorized and access denied handlers
            http.exceptionHandling().authenticationEntryPoint(commonAuthenticationEntryPoint);
            http.exceptionHandling().accessDeniedHandler(new JsonAccessDeniedHandler());
        }
	}

	@Configuration
    @Order(4)
    public static class DefaultWebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Autowired
		private ProtocolCheckingFilter protocolCheckingFilter;

		@Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .antMatcher("/**")
                    .authorizeRequests().anyRequest().permitAll();
			http.addFilterBefore(protocolCheckingFilter, X509AuthenticationFilter.class);
        }
	}
}

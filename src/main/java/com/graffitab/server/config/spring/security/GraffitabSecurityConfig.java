package com.graffitab.server.config.spring.security;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.graffitab.server.api.authentication.CommonAuthenticationEntryPoint;
import com.graffitab.server.api.authentication.CustomFailureBasicAuthFilter;
import com.graffitab.server.api.authentication.JsonAccessDeniedHandler;
import com.graffitab.server.api.authentication.JsonLoginAuthenticationFilter;
import com.graffitab.server.api.authentication.OkResponseLogoutHandler;
import com.graffitab.server.service.GraffiTabUserDetailsService;


@Configuration
@EnableWebSecurity
@EnableAutoConfiguration(exclude={SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, SpringBootWebSecurityConfiguration.class})
@Import(SecurityBeansConfig.class)
public class GraffitabSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(daoAuthenticationProvider());
	}

//====  If multiple authentication providers are required ====
//	@Bean
//	public AuthenticationManager authenticationManager() {
//		List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
//		authenticationProviders.add(daoAuthenticationProvider());
//		authenticationProviders.add(externalIdAuthenticationProvider());
//
//		ProviderManager authManager = new ProviderManager(authenticationProviders);
//		authManager.setEraseCredentialsAfterAuthentication(false);
//		return authManager;
//	}

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

        @Override
        protected void configure(HttpSecurity http) throws Exception {

        	// We allow anonymous access here (by not disabling it). This means that if a request matches
        	// and it is not authenticated (anonymous) we let it pass -- this is what we want for login and
        	// register endpoints
            http.csrf().disable()
                  .requestMatchers()
                    .antMatchers(HttpMethod.POST, "/api/login", "/api/users", "/api/users/resetpassword", "/api/users/externalprovider/**")
                    .antMatchers(HttpMethod.GET, "/api/users/activate/**")
                    .antMatchers(HttpMethod.PUT, "/api/users/resetpasswordwithtoken/**")
                    .and()
                    .authorizeRequests()
                    .anyRequest()
                    .permitAll()
                    .and()
            	    .sessionManagement()
            	    	.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

            http.addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            http.exceptionHandling().authenticationEntryPoint(new CommonAuthenticationEntryPoint());

        }
	}

	@Configuration
    @Order(2)
    public static class SessionAndBasicAuthSecurityConfig extends WebSecurityConfigurerAdapter{

		@Autowired
		private JsonLoginAuthenticationFilter jsonAuthenticationFilter;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .anonymous().disable()
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

            // Add the custom filter before the regular one
            http.addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            AuthenticationEntryPoint commonEntryPoint = new CommonAuthenticationEntryPoint();
            // Add the basic auth filter before the jsonLogin filter (check first)
            http.addFilterBefore(new CustomFailureBasicAuthFilter(authenticationManager(), commonEntryPoint),
            		    JsonLoginAuthenticationFilter.class);

            // Common entry points: 401 Unauthorized and access denied handlers
            http.exceptionHandling().authenticationEntryPoint(commonEntryPoint);
            http.exceptionHandling().accessDeniedHandler(new JsonAccessDeniedHandler());
        }
	}

	@Configuration
    @Order(3)
    public static class DefaultWebSecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .antMatcher("/**")
                    .authorizeRequests().anyRequest().permitAll();
        }
	}
}

package com.graffitab.server.api.authentication;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.log4j.Log4j2;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Custom implementation of Basic Authentication filter, where the presence of a session will take precedence
 * and bypass the basic authentication header checks.
 *
 * @author david
 */
@Log4j2
public class SessionPrecedenceBasicAuthFilter extends BasicAuthenticationFilter {

	public SessionPrecedenceBasicAuthFilter(
			AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint) {
		super(authenticationManager, authenticationEntryPoint);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		HttpSession session = request.getSession(false);
		if (session != null) {
			// There is a session provided for this request, use it and bypass basic authentication
			if (log.isDebugEnabled()) {
				log.debug("Session detected for current request [{}], basic authentication header will be ignored",
						  session.getId());
			}
			chain.doFilter(request, response);
			return;
		} else {
			super.doFilterInternal(request, response, chain);
		}
	}
}

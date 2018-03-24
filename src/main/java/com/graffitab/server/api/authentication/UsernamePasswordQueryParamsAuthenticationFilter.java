package com.graffitab.server.api.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class UsernamePasswordQueryParamsAuthenticationFilter extends JsonLoginAuthenticationFilter {

	public UsernamePasswordQueryParamsAuthenticationFilter() {
		super();
		// Any request is valid to authenticate like this, not only POSTs
		this.setPostOnly(false);
		this.setAllowSessionCreation(false);
		this.setFilterProcessesUrl("/v1/**");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException {

		// This method is only invoked if 'requiresAuthentication()' returns true -- in that
		// case we are sure the parameters are present so we can attempt authentication
		String username = obtainUsername(request);
		return attemptAuthenticationIfActiveUser(username, request, response);
	}

	@Override
	protected String obtainUsername(HttpServletRequest request) {
		return request.getParameter("username");
	}

	@Override
	protected String obtainPassword(HttpServletRequest request) {
		return request.getParameter("password");
	}

	@Override
	protected boolean requiresAuthentication(HttpServletRequest request,
			HttpServletResponse response) {

		HttpSession session = request.getSession(false);
		if (session != null) {
			// There is a session provided for this request, use it and bypass parameter checking
			if (log.isDebugEnabled()) {
				log.debug("Session detected for current request [{}], URL parameters authentication will be ignored",
						  session.getId());
			}

			return false;
		} else {

			String username = obtainUsername(request);
			String password = obtainPassword(request);

			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				if (log.isDebugEnabled()) {
					log.debug("Request is to process Authentication using URL provided parameters, username [{}], " +
							", password [******]");
				}
			}

			return 	super.requiresAuthentication(request, response) &&
					StringUtils.hasText(username) && StringUtils.hasText(password);
		}
	}
}

package com.graffitab.server.api.authentication;

import com.graffitab.server.service.user.UserSessionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 *
 * Filter to ensure the incoming request has an allowed protocol (HTTP, HTTPS)
 *
 * A 403 error is sent in case the protocol is incorrect
 *
 * @author david
 *
 */

@Log4j2
public class ProtocolCheckingFilter extends OncePerRequestFilter {

	@Value("${protocol.httpsOnlyAllowed:true}")
	private Boolean httpsOnlyAllowed;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

		String protocol = getProtocolForRequest(request);
		String requestUri = request.getRequestURI();

		if (httpsOnlyAllowed && !"https".equals(protocol) && !requestUri.endsWith("status")) {
			log.warn("Only HTTPS requests are allowed -- failing request");
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String getProtocolForRequest(HttpServletRequest request) {
		String protocol = request.getHeader("X-Forwarded-Proto");

		if (protocol == null) {
			protocol = request.getScheme();
		}

		if (log.isTraceEnabled()) {
			log.trace("Read protocol / scheme is: {}", protocol);
		}

		return protocol;
	}
}

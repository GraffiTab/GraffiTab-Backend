package com.graffitab.server.api.authentication;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.graffitab.server.api.dto.user.ExternalProviderDto.ExternalProviderType;
import com.graffitab.server.api.errors.EntityNotFoundException;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.user.UserService;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExternalProviderAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	@Resource
	private UserService userService;

	@Resource(name = "delegateJacksonHttpMessageConverter")
	private MappingJackson2HttpMessageConverter jsonConverter;


	public ExternalProviderAuthenticationFilter() {
		super(new AntPathRequestMatcher("/api/externalproviders/login", "POST"));
	}

	//FIXME: repeated code from JsonLoginFilter
	private JSONObject getPayload(HttpServletRequest request) {
		try {
			if (request.getContentType().equals("application/json")) {
				String payload = IOUtils.toString(request.getInputStream());
				if (payload.length() > 0) {
					JSONObject json = new JSONObject(payload);
					return json;
				}
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException,
			IOException, ServletException {

		if (log.isDebugEnabled()) {
			log.debug("Attempting authentication using external provider...");
		}

		JSONObject json = getPayload(request);
		JSONObject baseObject = json.getJSONObject("externalProvider");

		if (baseObject != null) {
			String externalProviderId = baseObject.getString("externalId");
			String token = baseObject.getString("accessToken");
			ExternalProviderType externalProviderType = ExternalProviderType.valueOf(baseObject.getString("externalProviderType"));

			try {
				User user = userService.verifyExternalProvider(externalProviderId, token, externalProviderType);
				ExternalIdAuthenticationToken auth = new ExternalIdAuthenticationToken();
				auth.setAccessToken(token);
				auth.setAuthenticated(true);
				auth.setExternalId(externalProviderId);
				auth.setUser(user);
				return auth;
			} catch(EntityNotFoundException enfe) {
				if (log.isDebugEnabled()) {
					log.debug("User related to external provider not found", enfe);
				}
				throw new InternalAuthenticationServiceException("Invalid credentials provided");
			}
		} else {
			throw new InternalAuthenticationServiceException("Invalid authentication request");
		}
	}
}

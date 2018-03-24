package com.graffitab.server.api.authentication;

import com.graffitab.server.api.errors.EntityNotFoundException;
import com.graffitab.server.api.errors.ExternalProviderTokenInvalidException;
import com.graffitab.server.api.errors.LoginUserNotActiveException;
import com.graffitab.server.api.errors.MaximumLoginAttemptsException;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.model.externalprovider.ExternalProviderType;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.AuthenticationService;
import com.graffitab.server.service.social.SocialNetworksService;
import com.graffitab.server.service.user.ExternalProviderService;
import com.graffitab.server.service.user.UserService;

import org.json.JSONObject;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExternalProviderAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	@Resource
	private UserService userService;

	@Resource
	private ExternalProviderService externalProviderService;

	@Resource
	private SocialNetworksService socialNetworksService;

	@Resource(name = "delegateJacksonHttpMessageConverter")
	private MappingJackson2HttpMessageConverter jsonConverter;


	public ExternalProviderAuthenticationFilter() {
		super(new AntPathRequestMatcher("/v1/externalproviders/login", "POST"));
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException,
			IOException, ServletException {

		if (log.isDebugEnabled()) {
			log.debug("Attempting authentication using external provider...");
		}

		JSONObject json = AuthenticationService.getJsonPayload(request);

		if (json == null) {
			throw new InternalAuthenticationServiceException("Request content type is not JSON -- this is not allowed");
		}

		JSONObject baseObject = json.getJSONObject("externalProvider");

		if (baseObject != null) {
			String externalProviderId = baseObject.getString("externalId");
			String token = baseObject.getString("accessToken");
			ExternalProviderType externalProviderType = ExternalProviderType.valueOf(baseObject.getString("externalProviderType"));
			User user;

			// Check if a user exists with this external provider.
			try {
				user = userService.verifyExternalProvider(externalProviderId, externalProviderType);
			} catch(EntityNotFoundException enfe) {
				if (log.isDebugEnabled()) {
					log.debug("User related to external provider not found", enfe);
				}
				throw new InternalAuthenticationServiceException("Invalid credentials provided");
			}

			// Check if the provided token is valid.
			if (!socialNetworksService.isValidToken(token, externalProviderType)) {
				try {
					userService.updateLoginAttempts(user.getUsername());
				} catch(RestApiException rae) {
					String msg = "Maximum login attempts for user [" + user.getUsername() + "]";
					throw new MaximumLoginAttemptsException(msg, rae);
				}

				RestApiException failureCause = new RestApiException(ResultCode.INVALID_TOKEN, "The provided token is not valid.");
				throw new ExternalProviderTokenInvalidException(failureCause.getMessage(), failureCause);
			}

			if (user.getAccountStatus() == AccountStatus.ACTIVE) {
				externalProviderService.updateToken(user, externalProviderType, token);

				ExternalIdAuthenticationToken auth = new ExternalIdAuthenticationToken();
				auth.setAccessToken(token);
				auth.setAuthenticated(true);
				auth.setExternalId(externalProviderId);
				auth.setUser(user);
				return auth;
			} else {
				String msg = "Current user is not in the expected state [" +
							AccountStatus.ACTIVE.name() + "], it is " +
							user.getAccountStatus().name();
				RestApiException failureCause = new RestApiException(ResultCode.USER_NOT_IN_EXPECTED_STATE, msg);
				throw new LoginUserNotActiveException(msg, failureCause);
			}
		} else {
			throw new InternalAuthenticationServiceException("Invalid authentication request");
		}
	}
}

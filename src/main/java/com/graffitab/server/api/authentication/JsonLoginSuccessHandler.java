package com.graffitab.server.api.authentication;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.graffitab.server.api.dto.user.GetUserResult;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.User;

public class JsonLoginSuccessHandler implements AuthenticationSuccessHandler {

	@Resource(name = "delegateJacksonHttpMessageConverter")
	private MappingJackson2HttpMessageConverter jsonConverter;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {

		GetUserResult result = new GetUserResult();
		UserDto dto = OrikaMapper.get().map((User)authentication.getPrincipal(), UserDto.class);
		result.setUser(dto);

		//GetUserResult result = userApi.getUserByUsername((UserDetails)authentication.getPrincipal());
		//TODO: We should try to use it as singleton
		// ObjectMapper mapper = new ObjectMapper();

		response.setStatus(HttpStatus.OK.value());
		response.setContentType("application/json");
		jsonConverter.getObjectMapper().writeValue(response.getOutputStream(), result);

	}
}

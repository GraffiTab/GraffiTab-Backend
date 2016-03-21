package com.graffitab.server.api.mapper.user;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.user.UserService;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

@Component
public class UserMapper extends CustomMapper<User, UserDto> {

	@Resource
	private UserService userService;

	@Override
	public void mapAtoB(User a, UserDto b, MappingContext context) {
		super.mapAtoB(a, b, context);

		b.setFollowedByCurrentUser(userService.getCurrentUser().isFollowing(a));
	}
}

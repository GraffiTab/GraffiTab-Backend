package com.graffitab.server.api.mapper.user;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.graffitab.server.api.dto.user.FullUserDto;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.user.UserService;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

@Component
public class FullUserMapper extends CustomMapper<User, FullUserDto> {

	@Resource
	private UserService userService;

	@Override
	public void mapAtoB(User a, FullUserDto b, MappingContext context) {
		super.mapAtoB(a, b, context);

		b.setFollowersCount(0);
		b.setFollowingCount(0);
		b.setStreamablesCount(0);
	}
}

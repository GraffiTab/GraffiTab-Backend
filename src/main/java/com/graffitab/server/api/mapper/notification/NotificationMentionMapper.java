package com.graffitab.server.api.mapper.notification;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.persistence.model.notification.NotificationMention;
import com.graffitab.server.service.user.UserService;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

@Component
public class NotificationMentionMapper extends CustomMapper<NotificationMention, NotificationDto> {

	@Resource
	private UserService userService;

	@Override
	public void mapAtoB(NotificationMention a, NotificationDto b, MappingContext context) {
		super.mapAtoB(a, b, context);

		b.getMentioner().setFollowedByCurrentUser(userService.getCurrentUser().isFollowing(a.getMentioner()));
	}
}

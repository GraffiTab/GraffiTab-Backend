package com.graffitab.server.api.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.graffitab.server.api.dto.activity.ActivityDto;
import com.graffitab.server.api.dto.asset.AssetDto;
import com.graffitab.server.api.dto.comment.CommentDto;
import com.graffitab.server.api.dto.location.LocationDto;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableDto;
import com.graffitab.server.api.dto.user.FullUserDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.mapper.user.FullUserMapper;
import com.graffitab.server.api.mapper.user.UserMapper;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.Location;
import com.graffitab.server.persistence.model.activity.ActivityComment;
import com.graffitab.server.persistence.model.activity.ActivityCreateStreamable;
import com.graffitab.server.persistence.model.activity.ActivityFollow;
import com.graffitab.server.persistence.model.activity.ActivityLike;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.notification.NotificationComment;
import com.graffitab.server.persistence.model.notification.NotificationFollow;
import com.graffitab.server.persistence.model.notification.NotificationLike;
import com.graffitab.server.persistence.model.notification.NotificationMention;
import com.graffitab.server.persistence.model.notification.NotificationWelcome;
import com.graffitab.server.persistence.model.streamable.StreamableGraffiti;
import com.graffitab.server.persistence.model.user.User;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

@Component
public class OrikaMapper {

	@Resource
	private AssetMapper assetMapper;

	@Resource
	private UserMapper userMapper;

	@Resource
	private FullUserMapper fullUserMapper;

	@Resource
	private FullStreamableMapper fullStreamableMapper;

	private MapperFactory mapperFactory;
	private MapperFacade mapperFacade;

	private MapperFactory getMapperFactory() {
		if (mapperFactory == null) {
			mapperFactory = new DefaultMapperFactory.Builder().build();

			registerMappings();
		}

		return mapperFactory;
	}

	private MapperFacade getMapperFacade() {
		if (mapperFacade == null) {
			mapperFacade = getMapperFactory().getMapperFacade();
		}

		return mapperFacade;
	}

	private void registerMappings() {
		// By default there automatic mapping is enabled, not need to register every single class
		// Map user DTOs.
		mapperFactory.classMap(User.class, UserDto.class)
		.byDefault()
		.customize(userMapper)
	    .register();

		mapperFactory.classMap(User.class, FullUserDto.class)
		.use(User.class, UserDto.class)
		.byDefault()
		.customize(fullUserMapper)
	    .register();

		// Map asset DTOs.
		mapperFactory.classMap(Asset.class, AssetDto.class)
		.byDefault()
		.customize(assetMapper)
		.register();

		// Map notification DTOs.
		mapperFactory.classMap(NotificationWelcome.class, NotificationDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(NotificationFollow.class, NotificationDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(NotificationLike.class, NotificationDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(NotificationComment.class, NotificationDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(NotificationMention.class, NotificationDto.class)
		.byDefault()
	    .register();

		// Map activity DTOs.
		mapperFactory.classMap(ActivityCreateStreamable.class, ActivityDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(ActivityFollow.class, ActivityDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(ActivityLike.class, ActivityDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(ActivityComment.class, ActivityDto.class)
		.byDefault()
	    .register();

		// Map streamable DTOs.
		mapperFactory.classMap(StreamableGraffiti.class, StreamableDto.class)
		.byDefault()
	    .register();

		mapperFactory.classMap(StreamableGraffiti.class, FullStreamableDto.class)
		.use(StreamableGraffiti.class, StreamableDto.class)
		.byDefault()
		.customize(fullStreamableMapper)
	    .register();

		// Map comment DTOs.
		mapperFactory.classMap(Comment.class, CommentDto.class)
		.byDefault()
		.register();

		// Map comment DTOs.
		mapperFactory.classMap(Location.class, LocationDto.class)
		.byDefault()
		.register();
	}

	public <T,K> T map(K source, Class<T> destinationClass) {
	   return getMapperFacade().map(source, destinationClass);
	}

	public <T,K> void map(T source, K destination) {
		getMapperFacade().map(source, destination);
	}

	public <T> List<T> mapList(List<?> sourceList, Class<T> destinationListElementClass) {
		List<T> mappedList = new ArrayList<>(sourceList.size());
		MapperFacade mapperFacade = getMapperFacade();

		for (Object sourceElement : sourceList) {
			mappedList.add(mapperFacade.map(sourceElement, destinationListElementClass));
		}

		return mappedList;
	}
}

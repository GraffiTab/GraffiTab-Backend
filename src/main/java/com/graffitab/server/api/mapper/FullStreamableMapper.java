package com.graffitab.server.api.mapper;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.persistence.model.streamable.StreamableGraffiti;
import com.graffitab.server.service.StreamableService;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;

@Component
public class FullStreamableMapper extends CustomMapper<StreamableGraffiti, FullStreamableDto> {

	@Resource
	private StreamableService streamableService;

	@Override
	public void mapAtoB(StreamableGraffiti a, FullStreamableDto b, MappingContext context) {
		super.mapAtoB(a, b, context);

		b.setLikersCount(0);
		b.setCommentsCount(0);
	}
}
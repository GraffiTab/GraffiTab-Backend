package com.graffitab.server.api.mapper;

import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.streamable.StreamableGraffiti;
import com.graffitab.server.service.streamable.StreamableStatsService;
import lombok.AllArgsConstructor;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FullStreamableMapper extends CustomMapper<StreamableGraffiti, FullStreamableDto> {

	private StreamableStatsService streamableStatsService;

	@Override
	public void mapAtoB(StreamableGraffiti streamableGraffiti, FullStreamableDto fullStreamableDto, MappingContext context) {
		processStats(streamableGraffiti, fullStreamableDto);
	}

	public void processStats(Streamable streamableGraffiti, FullStreamableDto streamableDto) {
		// likersCount
		// commentsCount
		// isLikedByCurrentUser
		streamableStatsService.processStreamableStats(streamableGraffiti, streamableDto);
	}
}

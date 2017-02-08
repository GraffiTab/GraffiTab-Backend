package com.graffitab.server.api.dto.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.user.UserDto;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class CommentDto {

	private Long id;
	private FullStreamableDto streamable;
	private UserDto user;
	private String text;
	private String createdOn;
	private String updatedOn;
}

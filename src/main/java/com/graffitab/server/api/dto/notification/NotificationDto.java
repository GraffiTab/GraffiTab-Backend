package com.graffitab.server.api.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.comment.CommentDto;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.user.UserDto;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class NotificationDto {
	private Long id;
	private Boolean isRead;

	@JsonProperty("date")
	private String createdOn;

	@JsonProperty("type")
	private String notificationType;

	private UserDto follower;
	private UserDto liker;
	private FullStreamableDto likedStreamable;
	private UserDto commenter;
	private FullStreamableDto commentedStreamable;
	private CommentDto comment;
	private UserDto mentioner;
	private FullStreamableDto mentionedStreamable;
	private CommentDto mentionedComment;
}

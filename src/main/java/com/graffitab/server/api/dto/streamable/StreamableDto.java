package com.graffitab.server.api.dto.streamable;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.asset.AssetDto;
import com.graffitab.server.api.dto.user.UserDto;

@Data
@JsonInclude(Include.NON_NULL)
public class StreamableDto {

	private Long id;
	private UserDto user;
	private String createdOn;
	private String updatedOn;

	@JsonProperty(value = "type")
	private String streamableType;

	private Boolean isPrivate;
	private Boolean isFlagged;
	private AssetDto asset;
	private Double latitude;
	private Double longitude;
	private Double roll;
	private Double yaw;
	private Double pitch;

	private String text;
}

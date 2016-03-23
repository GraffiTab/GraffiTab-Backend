package com.graffitab.server.api.dto.activity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.user.UserDto;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ActivityContainerDto {

	private UserDto user;
	private String date;

	@JsonProperty("type")
	private String activityType;

	private List<ActivityDto> activities;
}

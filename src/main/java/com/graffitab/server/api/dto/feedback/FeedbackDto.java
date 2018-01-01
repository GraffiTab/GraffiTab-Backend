package com.graffitab.server.api.dto.feedback;

import lombok.Data;

@Data
public class FeedbackDto {

	private String name;
	private String email;
	private String text;
	private String subject;
}

package com.graffitab.server.service.email;

import lombok.Getter;

@Getter
public enum EmailType {
	ACTIVATION("welcome.htm"), RESET_PASSWORD("password_reset.htm"), FEEDBACK("feedback.htm");
	private String templateName;
	private EmailType(String templateName) {
		this.templateName = templateName;
	}
}
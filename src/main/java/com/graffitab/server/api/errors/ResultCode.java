package com.graffitab.server.api.errors;

public enum ResultCode {

	OK(200),
	NOT_FOUND(404),
	TOKEN_EXPIRED(406),
	USER_NOT_FOUND(404),
	STREAMABLE_NOT_FOUND(404),
	COMMENT_NOT_FOUND(404),
	BAD_REQUEST(400),
	EMPTY_MANDATORY_FIELD(400),
	INVALID_USERNAME(400),
	INVALID_EMAIL(400),
	USERNAME_ALREADY_IN_USE(400),
	EMAIL_ALREADY_IN_USE(400),
	USER_NOT_LOGGED_IN(401),
	USER_NOT_OWNER(401),
	USER_NOT_IN_EXPECTED_STATE(406),
	INCORRECT_PASSWORD(403),
	ALREADY_EXISTS(422),
	GENERAL_ERROR(500);

	private Integer statusCode;

	public Integer getStatusCode() {
		return statusCode;
	}

	public String getStatusName() {
		return this.name();
	}

	private ResultCode(Integer statusCode) {
		this.statusCode = statusCode;
	}
}

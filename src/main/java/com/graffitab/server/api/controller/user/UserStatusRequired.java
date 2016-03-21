package com.graffitab.server.api.controller.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.graffitab.server.persistence.model.user.User.AccountStatus;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UserStatusRequired {
	public AccountStatus[] value() default AccountStatus.ACTIVE;
}

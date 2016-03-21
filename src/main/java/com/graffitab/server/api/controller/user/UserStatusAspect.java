package com.graffitab.server.api.controller.user;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.user.UserService;

import lombok.extern.log4j.Log4j2;

@Aspect
@Component
@Log4j2
public class UserStatusAspect {

	@Resource
	private UserService userService;

	@Before("execution(* *(..)) && @annotation(UserStatusRequired)")
	public void checkUserStatus(JoinPoint joinPoint) {

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
	    Method method = signature.getMethod();

	    UserStatusRequired userStatusAnnotation = method.getAnnotation(UserStatusRequired.class);

		if (log.isDebugEnabled()) {
			log.debug("Checking user status is required for " + joinPoint.getStaticPart().toLongString());
		}

		User user = userService.getCurrentUser();
		List<AccountStatus> requiredStatuses = Arrays.asList(userStatusAnnotation.value());

		boolean userNotInRequiredStatus = requiredStatuses.stream().noneMatch( status -> status == user.getAccountStatus());


		if (userNotInRequiredStatus) {
			throw new RestApiException(ResultCode.USER_NOT_IN_EXPECTED_STATE, "Current user is not in the expected state " +
									   userStatusAnnotation.value());
		}
	}
}

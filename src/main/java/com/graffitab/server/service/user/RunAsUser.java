package com.graffitab.server.service.user;

import com.graffitab.server.persistence.model.user.User;

/**
 *
 * @author david
 *
 */
public class RunAsUser {

	private static ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

	public static User get() {
		return userThreadLocal.get();
	}

	public static void set(User user) {
		userThreadLocal.set(user);
	}

	public static void clear() {
		userThreadLocal.set(null);
	}
}

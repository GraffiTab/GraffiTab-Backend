package com.graffitab.server.config.web;

import lombok.extern.log4j.Log4j2;

/**
 *
 * Thread local to store the single JSON property this request will extract
 *
 * @author david
 *
 */
@Log4j2
public class RunWithJsonProperty {

	private static final ThreadLocal<String> JSON_PROPERTY_NAME = new ThreadLocal<>();

	public static String get() {
		return JSON_PROPERTY_NAME.get();
	}

	public static void set(String jsonProperty) {
		if (JSON_PROPERTY_NAME.get() != null) {
			throw new RuntimeException("Already running with property");
		}

		if (log.isDebugEnabled()) {
			log.debug("Setting property to extract in this request as: {}", jsonProperty);
		}

		JSON_PROPERTY_NAME.set(jsonProperty);
	}

	public static void reset() {
		JSON_PROPERTY_NAME.set(null);
	}
}

package com.graffitab.server.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Service
public class AuthenticationService {

	public static JSONObject getJsonPayload(HttpServletRequest request) {
		try {
			if (request.getContentType().contains("application/json")) {
				String payload = IOUtils.toString(request.getInputStream());
				if (payload.length() > 0) {
					return new JSONObject(payload);
				}
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String generateBasicAuthToken(String username, String password) {
		String authenticationString = username + ":" + password;
		byte[] encodedBytes = Base64.encodeBase64(authenticationString.getBytes());
		String authenticationToken = new String(encodedBytes);
		return authenticationToken;
	}
}

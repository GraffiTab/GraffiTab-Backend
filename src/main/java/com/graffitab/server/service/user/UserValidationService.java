package com.graffitab.server.service.user;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.model.user.User;

@Service
public class UserValidationService {

	@Resource
	private UserService userService;

	@Transactional
	public Boolean validateUser(User user) {

		boolean validationResult = false;
		String errorText = "";
		
		if ( StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getEmail()) ||
			 StringUtils.isEmpty(user.getFirstName()) || StringUtils.isEmpty(user.getLastName()) ||
			 StringUtils.isEmpty(user.getPassword())) {

			validationResult = false;
			errorText = "Empty mandatory field." ;
			
		} else if (!user.getUsername().matches("[A-Za-z0-9-_]+") ||
				    user.getUsername().length() < 3 ||
				    user.getUsername().length() > 25){
			
			validationResult = false;
			errorText = "Invalid username";
			
		} else if(!user.getEmail().matches("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")){
			
			validationResult = false;
			errorText = "Invalid email";
			
		} else {

			if (isUsernameTaken(user.getUsername(), user.getId())  ){
				
				validationResult = false;
				errorText="Username already in use.";
				
			} else if(isEmailTaken(user.getEmail(), user.getId())){
				
				validationResult = false;
				errorText="Email already in use.";
				
			} else { 
				
				validationResult = true;
				
			}
		}
		
		if (!validationResult) {
			
			throw new RestApiException(ResultCode.BAD_REQUEST, errorText);
			
		}
		
		return validationResult;
	}

	public Boolean validateEditInfo(String firstname, String lastname, String website, String about) {
		boolean validationResult = false;

		if (StringUtils.isEmpty(firstname) || StringUtils.isEmpty(lastname)) {
			validationResult = false;
		}
		else {
			validationResult = true;
		}

		return validationResult;
	}

	private Boolean isUsernameTaken(String username, Long userId) {
		if ( userId != null){
			return !userService.findUsersByUsernameWithDifferentId(username, userId).isEmpty();
		} else {
			return userService.findByUsername(username) != null;
		}
	}

	private Boolean isEmailTaken(String email, Long userId) {
		if ( userId != null){
			return !userService.findUsersByEmailWithDifferentID(email, userId).isEmpty();
		} else {
			return userService.findByEmail(email) != null;
		}
	}
}

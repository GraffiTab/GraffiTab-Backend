package com.graffitab.server.api.controller.user;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.controller.BaseApiController;
import com.graffitab.server.api.dto.ActionCompletedResult;
import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.user.ExternalUserDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.dto.user.UserProfileDto;
import com.graffitab.server.api.dto.user.result.CreateUserResult;
import com.graffitab.server.api.dto.user.result.GetUserProfileResult;
import com.graffitab.server.api.dto.user.result.GetUserResult;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.api.util.UploadUtils;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.User.AccountStatus;
import com.graffitab.server.service.user.UserService;
import com.graffitab.server.util.GuidGenerator;

@RestController
@RequestMapping("/api/users")
public class UserApiController extends BaseApiController {

	@Resource
	private UserService userService;

	@Resource
	private OrikaMapper mapper;

	@Resource
	private UploadUtils uploadUtils;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserResult getUser(@PathVariable("id") Long id) {
		GetUserResult getUserResult = new GetUserResult();
		User user = userService.findUserById(id);
		getUserResult.setUser(mapper.map(user, UserDto.class));
		return getUserResult;
	}

	@RequestMapping(value = "/username/{username}", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserResult getUserByUsername(@PathVariable("username") String username) {
		GetUserResult getUserResult = new GetUserResult();
		User user = (User) userService.getUserByUsername(username);
		getUserResult.setUser(mapper.map(user, UserDto.class));
		return getUserResult;
	}

	@RequestMapping(value = {"/externalprovider"}, method = RequestMethod.POST, consumes={"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public GetUserResult createExternalUser(@RequestBody ExternalUserDto externalUserDto) {
		GetUserResult createExternalUserResult = new GetUserResult();
		User user = userService.createExternalUser(mapper.map(externalUserDto.getUser(), User.class), externalUserDto.getExternalId(), externalUserDto.getAccessToken(), externalUserDto.getExternalProviderType());
		createExternalUserResult.setUser(mapper.map(user, UserDto.class));
		return createExternalUserResult;
	}

	@RequestMapping(value = {""}, method = RequestMethod.POST, consumes={"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public CreateUserResult createUser(@JsonProperty("user") UserDto userDto) {
		CreateUserResult createUserResult = new CreateUserResult();
		String userToken = GuidGenerator.generate();
		User user = userService.createUser(mapper.map(userDto, User.class), userToken);
		createUserResult.setUser(mapper.map(user, UserDto.class));
		createUserResult.setToken(user.getMetadataItems().get(UserService.ACTIVATION_TOKEN_METADATA_KEY));
		return createUserResult;
	}

	@RequestMapping(value = "/activate/{token}", method = RequestMethod.GET)
	@Transactional
	public ActionCompletedResult activateAccount(@PathVariable("token") String token) {
		ActionCompletedResult activateUserResult = new ActionCompletedResult();
		userService.activateUser(token);
		return activateUserResult;
	}

	@RequestMapping(value = "/resetpassword", method = RequestMethod.POST)
	public ActionCompletedResult resetPassword(@JsonProperty(value = "email") String email) {
		ActionCompletedResult resetPasswordResult = new ActionCompletedResult();
		userService.resetPassword(email);
		return resetPasswordResult;
	}

	@RequestMapping(value = "/resetpassword/{token}", method = RequestMethod.PUT)
	public ActionCompletedResult completePasswordReset(@PathVariable(value = "token") String token,
													   @JsonProperty(value = "password") String password) {
		ActionCompletedResult resetPasswordResult = new ActionCompletedResult();
		userService.completePasswordReset(token, password);
		return resetPasswordResult;
	}

	@RequestMapping(value = {"/{id}/profile"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserProfileResult getUserProfile(@PathVariable("id") Long id) {
		GetUserProfileResult userProfileResult = new GetUserProfileResult();
		User user = userService.getUserProfile(id);
		userProfileResult.setUser(mapper.map(user, UserProfileDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = {"/{id}/follow"}, method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserProfileResult follow(@PathVariable("id") Long userId) {
		GetUserProfileResult userProfileResult = new GetUserProfileResult();
		User toFollow = userService.follow(userId);
		userProfileResult.setUser(mapper.map(toFollow, UserProfileDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = {"/{id}/unfollow"}, method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserProfileResult unFollow(@PathVariable("id") Long userId) {
		GetUserProfileResult userProfileResult = new GetUserProfileResult();
		User toUnfollow = userService.unfollow(userId);
		userProfileResult.setUser(mapper.map(toUnfollow, UserProfileDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = {"/{id}/followers"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowers(@PathVariable("id") Long userId,
												 @RequestParam(value="offset", required = false) Integer offset,
												 @RequestParam(value="count", required = false) Integer count) {
		return userService.getFollowingOrFollowersResultForUser(true, userId, offset, count);
	}

	@RequestMapping(value = {"/{id}/following"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowing(@PathVariable("id") Long userId,
												 @RequestParam(value="offset", required = false) Integer offset,
												 @RequestParam(value="count", required = false) Integer count) {
		return userService.getFollowingOrFollowersResultForUser(false, userId, offset, count);
	}

	//TODO: * fullProfile /api/users/me
	//TODO: Most active users -> /api/users/mostactive page by page
	//TODO: getSocialFriends -> /api/users/socialfriends page by page
}

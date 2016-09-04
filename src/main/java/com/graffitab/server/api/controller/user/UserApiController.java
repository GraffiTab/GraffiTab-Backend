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
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.user.ExternalUserDto;
import com.graffitab.server.api.dto.user.FullUserDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.dto.user.result.GetFullUserResult;
import com.graffitab.server.api.dto.user.result.GetUserResult;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.streamable.StreamableService;
import com.graffitab.server.service.user.UserService;

import java.util.Locale;

@RestController
@RequestMapping("/api/users")
public class UserApiController extends BaseApiController {

	@Resource
	private UserService userService;

	@Resource
	private StreamableService streamableService;

	@Resource
	private OrikaMapper mapper;

	@Resource
	private TransactionUtils transactionUtils;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserResult getUser(@PathVariable("id") Long id) {
		GetUserResult getUserResult = new GetUserResult();
		User user = userService.getUser(id);
		getUserResult.setUser(mapper.map(user, UserDto.class));
		return getUserResult;
	}

	@RequestMapping(value = {"/{id}/profile"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult getUserProfile(@PathVariable("id") Long id) {
		GetFullUserResult getFullUserResult = new GetFullUserResult();
		User user = userService.getUser(id);
		getFullUserResult.setUser(mapper.map(user, FullUserDto.class));
		return getFullUserResult;
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

	@RequestMapping(value = "/username/{username}/profile", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult getUserProfileByUsername(@PathVariable("username") String username) {
		GetFullUserResult getFullUserResult = new GetFullUserResult();
		User user = (User) userService.getUserByUsername(username);
		getFullUserResult.setUser(mapper.map(user, FullUserDto.class));
		return getFullUserResult;
	}

	@RequestMapping(value = {"/externalproviders"}, method = RequestMethod.POST, consumes={"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	public ActionCompletedResult createExternalUser(@RequestBody ExternalUserDto externalUserDto, Locale locale) {
		userService.createExternalUser(mapper.map(externalUserDto.getUser(), User.class),
				externalUserDto.getExternalId(), externalUserDto.getAccessToken(),
				externalUserDto.getExternalProviderType(), locale);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {""}, method = RequestMethod.POST, consumes={"application/json"})
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public ActionCompletedResult createUser(@JsonProperty("user") UserDto userDto, Locale locale) {
		userService.createUser(mapper.map(userDto, User.class), locale);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/activate/{token}", method = RequestMethod.GET)
	public ActionCompletedResult activateAccount(@PathVariable("token") String token) {
		userService.activateUser(token);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/resetpassword", method = RequestMethod.POST)
	public ActionCompletedResult resetPassword(@JsonProperty(value = "email") String email, Locale locale) {
		userService.resetPassword(email, locale);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/resetpassword/{token}", method = RequestMethod.PUT)
	@Transactional
	public ActionCompletedResult completePasswordReset(
			@PathVariable(value = "token") String token,
			@JsonProperty(value = "password") String password) {
		userService.completePasswordReset(token, password);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/{id}/followers"}, method = RequestMethod.POST)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult follow(@PathVariable("id") Long userId) {
		GetFullUserResult userProfileResult = new GetFullUserResult();
		User toFollow = userService.follow(userId);
		transactionUtils.executeInTransaction(() -> {
			User resultUser = userService.findUserById(toFollow.getId());
			userProfileResult.setUser(mapper.map(resultUser, FullUserDto.class));
		});
		return userProfileResult;
	}

	@RequestMapping(value = {"/{id}/followers"}, method = RequestMethod.DELETE)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult unFollow(@PathVariable("id") Long userId) {
		GetFullUserResult userProfileResult = new GetFullUserResult();
		User toUnfollow = userService.unfollow(userId);
		transactionUtils.executeInTransaction(() -> {
			User resultUser = userService.findUserById(toUnfollow.getId());
			userProfileResult.setUser(mapper.map(resultUser, FullUserDto.class));
		});
		return userProfileResult;
	}

	@RequestMapping(value = {"/{id}/followers"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowers(
			@PathVariable("id") Long userId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.getFollowingOrFollowersForUserResult(true, userId, offset, limit);
	}

	@RequestMapping(value = {"/{id}/following"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowing(
			@PathVariable("id") Long userId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.getFollowingOrFollowersForUserResult(false, userId, offset, limit);
	}

	@RequestMapping(value = {"/{id}/streamables"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getStreamables(
			@PathVariable("id") Long userId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getUserStreamablesResult(userId, offset, limit);
	}

	@RequestMapping(value = {"/{id}/streamables/location"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> searchStreamablesAtLocation(
			@PathVariable("id") Long userId,
			@RequestParam(value="neLatitude", required = true) Double neLatitude,
			@RequestParam(value="neLongitude", required = true) Double neLongitude,
			@RequestParam(value="swLatitude", required = true) Double swLatitude,
			@RequestParam(value="swLongitude", required = true) Double swLongitude) {
		return streamableService.searchUserStreamablesAtLocationResult(userId, neLatitude, neLongitude, swLatitude, swLongitude);
	}

	@RequestMapping(value = {"/mostactive"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getMostActiveUsers(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.getMostActiveUsersResult(offset, limit);
	}

	@RequestMapping(value = {"/{id}/liked"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getLikedStreamablesForUser(
			@PathVariable("id") Long userId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getLikedStreamablesForUserResult(userId, offset, limit);
	}

	@RequestMapping(value = {"/search"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> searchUsers(
			@RequestParam(value="query", required = true) String query,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.searchUsersResult(query, offset, limit);
	}
}

package com.graffitab.server.api.controller.user;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.ActionCompletedResult;
import com.graffitab.server.api.dto.CountResult;
import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.activity.ActivityContainerDto;
import com.graffitab.server.api.dto.asset.AssetDto;
import com.graffitab.server.api.dto.asset.result.CreateAssetResult;
import com.graffitab.server.api.dto.device.DeviceDto;
import com.graffitab.server.api.dto.location.LocationDto;
import com.graffitab.server.api.dto.location.result.CreateLocationResult;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableGraffitiDto;
import com.graffitab.server.api.dto.streamable.result.CreateStreamableResult;
import com.graffitab.server.api.dto.streamable.result.GetFullStreamableResult;
import com.graffitab.server.api.dto.user.ChangePasswordDto;
import com.graffitab.server.api.dto.user.ExternalProviderDto;
import com.graffitab.server.api.dto.user.ExternalProviderDto.ExternalProviderType;
import com.graffitab.server.api.dto.user.FullUserDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.dto.user.result.GetFullUserResult;
import com.graffitab.server.api.dto.user.result.GetUserResult;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.Location;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.streamable.StreamableService;
import com.graffitab.server.service.user.DeviceService;
import com.graffitab.server.service.user.LocationService;
import com.graffitab.server.service.user.UserService;

@RestController
@RequestMapping("/api/users/me")
public class MeApiController {

	@Resource
	private UserService userService;

	@Resource
	private StreamableService streamableService;

	@Resource
	private NotificationService notificationService;

	@Resource
	private ActivityService activityService;

	@Resource
	private DeviceService deviceService;

	@Resource
	private LocationService locationService;

	@Resource
	private OrikaMapper mapper;

	@RequestMapping(value = "", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserResult getMe() {
		GetUserResult getUserResult = new GetUserResult();
		User user = userService.getCurrentUser();
		getUserResult.setUser(mapper.map(user, UserDto.class));
		return getUserResult;
	}

	@RequestMapping(value = {"/profile"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult getProfile() {
		GetFullUserResult userProfileResult = new GetFullUserResult();
		User user = userService.getUserProfile(userService.getCurrentUser().getId());
		userProfileResult.setUser(mapper.map(user, FullUserDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = {"/notifications"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<NotificationDto> getNotifications(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return notificationService.getNotificationsResult(offset, count);
	}

	@RequestMapping(value = {"/notifications/unreadcount"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CountResult getUnreadNotifications() {
		CountResult countResult = new CountResult();
		Long count = notificationService.getUnreadNotificationsCount();
		countResult.setCount(count);
		return countResult;
	}

	@RequestMapping(value = {"/activity/followers"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<ActivityContainerDto> getFollowersActivity(
			@RequestParam(value="numberOfItemsInGroup", required = false) Integer numberOfItemsInGroup,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return activityService.getFollowersActivityResult(numberOfItemsInGroup, offset, count);
	}

	@RequestMapping(value = {""}, method = RequestMethod.POST, consumes={"application/json"})
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetUserResult update(@JsonProperty("user") UserDto userDto) {
		GetUserResult updateUserResult = new GetUserResult();
		User user = userService.updateUser(mapper.map(userDto, User.class));
		updateUserResult.setUser(mapper.map(user, UserDto.class));
		return updateUserResult;
	}

	@RequestMapping(value = {"/avatar"}, method = RequestMethod.POST)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateAssetResult updateAvatar(HttpServletRequest request) throws IOException {
		CreateAssetResult editAvatarResult = new CreateAssetResult();
		Asset asset = userService.updateAvatar(request.getInputStream(), request.getContentLengthLong());
		editAvatarResult.setAsset(mapper.map(asset, AssetDto.class));
		return editAvatarResult;
	}

	@RequestMapping(value = {"/avatar"}, method = RequestMethod.DELETE)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteAvatar() throws IOException {
		userService.deleteAvatar();
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/cover"}, method = RequestMethod.POST)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateAssetResult updateCover(HttpServletRequest request) throws IOException {
		CreateAssetResult editcoverResult = new CreateAssetResult();
		Asset asset = userService.updateCover(request.getInputStream(), request.getContentLengthLong());
		editcoverResult.setAsset(mapper.map(asset, AssetDto.class));
		return editcoverResult;
	}

	@RequestMapping(value = {"/cover"}, method = RequestMethod.DELETE)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteCover() throws IOException {
		userService.deleteCover();
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/locations", method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateLocationResult createLocation(@JsonProperty("location") LocationDto locationDto) {
		CreateLocationResult createLocationResult = new CreateLocationResult();
		Location location = locationService.createLocation(locationDto.getAddress(), locationDto.getLatitude(), locationDto.getLongitude());
		createLocationResult.setLocation(mapper.map(location, LocationDto.class));
		return createLocationResult;
	}

	@RequestMapping(value = "/locations/{id}", method = RequestMethod.PUT)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateLocationResult editLocation(
			@PathVariable("id") Long locationId,
			@JsonProperty("location") LocationDto locationDto) {
		CreateLocationResult createLocationResult = new CreateLocationResult();
		Location location = locationService.editLocation(locationId, locationDto.getAddress(), locationDto.getLatitude(), locationDto.getLongitude());
		createLocationResult.setLocation(mapper.map(location, LocationDto.class));
		return createLocationResult;
	}

	@RequestMapping(value = "/locations/{id}", method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteLocation(@PathVariable("id") Long locationId) {
		locationService.deleteLocation(locationId);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/locations"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<LocationDto> getLocations(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return locationService.getLocationsResult(offset, count);
	}

	@RequestMapping(value = "/devices", method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult registerDevice(@JsonProperty("device") DeviceDto deviceDto) {
		deviceService.registerDevice(deviceDto.getToken(), deviceDto.getOsType());
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/devices", method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult unregisterDevice(@JsonProperty("device") DeviceDto deviceDto) {
		deviceService.unregisterDevice(deviceDto.getToken(), deviceDto.getOsType());
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/externalproviders", method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult linkExternalProvider(@JsonProperty("externalProvider") ExternalProviderDto externalProviderDto) {
		userService.linkExternalProvider(externalProviderDto.getExternalId(), externalProviderDto.getAccessToken(), externalProviderDto.getExternalProviderType());
		return new ActionCompletedResult();
	}

	@RequestMapping(value = "/externalproviders", method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult unlinkExternalProvider(@JsonProperty("externalProviderType") ExternalProviderType externalProviderType) {
		userService.unlinkExternalProvider(externalProviderType);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/changepassword"}, method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
		ActionCompletedResult getUserResult = new ActionCompletedResult();
		userService.changePassword(changePasswordDto.getCurrentPassword(), changePasswordDto.getNewPassword());
		return getUserResult;
	}

	@RequestMapping(value = {"/followers"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowers(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return userService.getFollowingOrFollowersForUserResult(true, null, offset, count);
	}

	@RequestMapping(value = {"/following"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowing(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return userService.getFollowingOrFollowersForUserResult(false, null, offset, count);
	}

	@RequestMapping(value = "/streamables/graffiti", method = RequestMethod.POST)
	@ResponseBody
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateStreamableResult createGraffiti(
			@RequestPart("properties") StreamableGraffitiDto streamableDto,
			@RequestPart("file") @NotNull @NotBlank MultipartFile file) {
		try {
			CreateStreamableResult addStreamableResult = new CreateStreamableResult();
			Streamable streamable = streamableService.createStreamableGraffiti(streamableDto, file.getInputStream(), file.getSize());
			addStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
			return addStreamableResult;
		} catch (IOException e) {
			throw new RestApiException(ResultCode.BAD_REQUEST,
					"File stream could not be read.");
		}
	}

	@RequestMapping(value = {"/streamables"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<StreamableDto> getStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return streamableService.getUserStreamablesResult(userService.getCurrentUser().getId(), offset, count);
	}

	@RequestMapping(value = {"/streamables/{id}/private"}, method = RequestMethod.POST)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult makePublic(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable streamable = streamableService.makePublicOrPrivate(streamableId, true);
		getFullStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
		return getFullStreamableResult;
	}

	@RequestMapping(value = {"/streamables/{id}/private"}, method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult makePrivate(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable streamable = streamableService.makePublicOrPrivate(streamableId, false);
		getFullStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
		return getFullStreamableResult;
	}

	@RequestMapping(value = {"/streamables/private"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<StreamableDto> getPrivateStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return streamableService.getPrivateStreamablesResult(offset, count);
	}

	@RequestMapping(value = {"/feed"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<StreamableDto> getFeed(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return streamableService.getUserFeedResult(userService.getCurrentUser().getId(), offset, count);
	}

	@RequestMapping(value = {"/liked"}, method = RequestMethod.GET)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<StreamableDto> getLikedStreamablesForUser(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="count", required = false) Integer count) {
		return streamableService.getLikedStreamablesForUserResult(userService.getCurrentUser().getId(), offset, count);
	}
}

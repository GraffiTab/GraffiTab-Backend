package com.graffitab.server.api.controller.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.dto.ActionCompletedResult;
import com.graffitab.server.api.dto.CountResult;
import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.activity.ActivityContainerDto;
import com.graffitab.server.api.dto.asset.AssetDto;
import com.graffitab.server.api.dto.asset.result.AssetResult;
import com.graffitab.server.api.dto.device.DeviceDto;
import com.graffitab.server.api.dto.externalprovider.ExternalProviderDto;
import com.graffitab.server.api.dto.location.LocationDto;
import com.graffitab.server.api.dto.location.result.CreateLocationResult;
import com.graffitab.server.api.dto.notification.NotificationDto;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.streamable.StreamableGraffitiDto;
import com.graffitab.server.api.dto.streamable.result.CreateStreamableResult;
import com.graffitab.server.api.dto.streamable.result.GetFullStreamableResult;
import com.graffitab.server.api.dto.user.ChangePasswordDto;
import com.graffitab.server.api.dto.user.FullUserDto;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.dto.user.UserSocialFriendsContainerDto;
import com.graffitab.server.api.dto.user.result.GetFullUserResult;
import com.graffitab.server.api.dto.user.result.GetUserResult;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.Location;
import com.graffitab.server.persistence.model.asset.Asset;
import com.graffitab.server.persistence.model.externalprovider.ExternalProviderType;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.persistence.model.user.UserSocialFriendsContainer;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.asset.MultipartFileTransferableStream;
import com.graffitab.server.service.asset.TransferableStream;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.streamable.StreamableService;
import com.graffitab.server.service.user.DeviceService;
import com.graffitab.server.service.user.ExternalProviderService;
import com.graffitab.server.service.user.LocationService;
import com.graffitab.server.service.user.UserService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@Log4j2
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
	private ExternalProviderService externalProviderService;

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
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult getProfile() {
		GetFullUserResult userProfileResult = new GetFullUserResult();
		User user = userService.getUser(userService.getCurrentUser().getId());
		userProfileResult.setUser(mapper.map(user, FullUserDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = {"/notifications"}, method = RequestMethod.GET)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<NotificationDto> getNotifications(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return notificationService.getNotificationsResult(offset, limit);
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

	@RequestMapping(value = {""}, method = RequestMethod.PUT, consumes={"application/json"})
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult edit(@JsonProperty("user") UserDto userDto) {
		GetFullUserResult updateUserResult = new GetFullUserResult();
		User user = userService.editUser(mapper.map(userDto, User.class));
		updateUserResult.setUser(mapper.map(user, FullUserDto.class));
		return updateUserResult;
	}

	@RequestMapping(value = {"/avatar"}, method = RequestMethod.POST)
	@ResponseBody
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public AssetResult editAvatar(@RequestPart("file") @NotNull @NotBlank MultipartFile file) throws IOException {
		String contentType = file.getContentType();
		if (!contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE) && !contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
			throw new RestApiException(ResultCode.UNSUPPORTED_FILE_TYPE,
					"The file type '" + contentType + "' is not supported.");
		}
		try {
			AssetResult editAvatarResult = new AssetResult();
			TransferableStream transferableStream = new MultipartFileTransferableStream(file);
			Asset asset = userService.addOrEditAvatar(transferableStream, file.getSize());
			editAvatarResult.setAsset(mapper.map(asset, AssetDto.class));
			return editAvatarResult;
		} catch (Exception e) {
			log.error("File stream could not be read.", e);
			throw new RestApiException(ResultCode.STREAM_COULD_NOT_BE_READ,
					"File stream could not be read.");
		}
	}

	@RequestMapping(value = {"/avatar"}, method = RequestMethod.DELETE)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteAvatar() throws IOException {
		userService.deleteAvatar();
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/cover"}, method = RequestMethod.POST)
	@ResponseBody
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public AssetResult editCover(@RequestPart("file") @NotNull @NotBlank MultipartFile file) throws IOException {
		String contentType = file.getContentType();
		if (!contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE) && !contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
			throw new RestApiException(ResultCode.UNSUPPORTED_FILE_TYPE,
					"The file type '" + contentType + "' is not supported.");
		}
		try {
			AssetResult editcoverResult = new AssetResult();
			TransferableStream transferableStream = new MultipartFileTransferableStream(file);
			Asset asset = userService.addOrEditCover(transferableStream, file.getSize());
			editcoverResult.setAsset(mapper.map(asset, AssetDto.class));
			return editcoverResult;
		} catch (Exception e) {
			log.error("File stream could not be read.", e);
			throw new RestApiException(ResultCode.STREAM_COULD_NOT_BE_READ,
					"File stream could not be read.");
		}
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
			@RequestParam(value="limit", required = false) Integer limit) {
		return locationService.getLocationsResult(offset, limit);
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
	public GetFullUserResult linkExternalProvider(@JsonProperty("externalProvider") ExternalProviderDto externalProviderDto) {
		GetFullUserResult userProfileResult = new GetFullUserResult();
		User user = externalProviderService.linkExternalProvider(externalProviderDto.getExternalProviderType(), externalProviderDto.getExternalUserId(), externalProviderDto.getAccessToken());
		userProfileResult.setUser(mapper.map(user, FullUserDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = "/externalproviders", method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullUserResult unlinkExternalProvider(@JsonProperty("type") ExternalProviderType externalProviderType) {
		GetFullUserResult userProfileResult = new GetFullUserResult();
		User user = externalProviderService.unlinkExternalProvider(externalProviderType);
		userProfileResult.setUser(mapper.map(user, FullUserDto.class));
		return userProfileResult;
	}

	@RequestMapping(value = {"/changepassword"}, method = RequestMethod.PUT)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult changePassword(@RequestBody ChangePasswordDto changePasswordDto) {
		userService.changePassword(changePasswordDto.getCurrentPassword(), changePasswordDto.getNewPassword());
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/followers"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowers(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.getFollowingOrFollowersForUserResult(true, null, offset, limit);
	}

	@RequestMapping(value = {"/followers/activity"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<ActivityContainerDto> getFollowersActivity(
			@RequestParam(value="numberOfItemsInGroup", required = false) Integer numberOfItemsInGroup,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return activityService.getFollowersActivityResult(numberOfItemsInGroup, offset, limit);
	}

	@RequestMapping(value = {"/following"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getFollowing(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.getFollowingOrFollowersForUserResult(false, null, offset, limit);
	}

	@RequestMapping(value = "/streamables/graffiti/import",method = RequestMethod.POST)
	@ResponseBody
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateStreamableResult importGraffiti(@RequestBody StreamableGraffitiDto streamableDto) {
		CreateStreamableResult addStreamableResult = new CreateStreamableResult();
		Streamable streamable;

		if (streamableDto.getAsset() != null && StringUtils.hasText(streamableDto.getAsset().getLink())) {
			// From external resource
			streamable = streamableService.createStreamableGraffitiFromExternalResource(streamableDto);
			addStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
			return addStreamableResult;
		} else {
			throw new RestApiException(ResultCode.ASSET_NOT_FOUND, "Asset url has not been provided to import into streamable");
		}
	}

	@RequestMapping(value = "/streamables/graffiti", method = RequestMethod.POST)
	@ResponseBody
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateStreamableResult createGraffiti(
			@RequestPart("properties") StreamableGraffitiDto streamableDto,
			@RequestPart("file") MultipartFile file) {

		TransferableStream transferableStream;
		CreateStreamableResult addStreamableResult = new CreateStreamableResult();

		try {

			Streamable streamable;

			// Uploaded image
			String contentType = file.getContentType();
			if (!contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE) &&
					!contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
				throw new RestApiException(ResultCode.UNSUPPORTED_FILE_TYPE,
						"The file type '" + contentType + "' is not supported.");
			}
			transferableStream = new MultipartFileTransferableStream(file);
			streamable = streamableService.createStreamableGraffiti(streamableDto, transferableStream, file.getSize());

			addStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
			return addStreamableResult;

		} catch (Exception e) {
			throw new RestApiException(ResultCode.STREAM_COULD_NOT_BE_READ, "File   stream could not be read.");
		}
	}

	@RequestMapping(value = "/streamables/graffiti/{id}", method = RequestMethod.POST)
	@ResponseBody
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateStreamableResult editGraffiti(
			@PathVariable("id") Long streamableId,
			@RequestPart("properties") StreamableGraffitiDto streamableDto,
			@RequestPart("file") @NotNull @NotBlank MultipartFile file) {
		String contentType = file.getContentType();
		if (!contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE) && !contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
			throw new RestApiException(ResultCode.UNSUPPORTED_FILE_TYPE,
					"The file type '" + contentType + "' is not supported.");
		}

		try {
			CreateStreamableResult addStreamableResult = new CreateStreamableResult();
			TransferableStream transferableStream = new MultipartFileTransferableStream(file);
			Streamable streamable = streamableService.editStreamableGraffiti(streamableId, streamableDto, transferableStream,
									file.getSize());
			addStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
			return addStreamableResult;
		} catch (Exception e) {
			throw new RestApiException(ResultCode.STREAM_COULD_NOT_BE_READ,
					"File stream could not be read.");
		}
	}

	@RequestMapping(value = {"/streamables/{id}"}, method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteStreamable(@PathVariable("id") Long streamableId) {
		streamableService.deleteStreamable(streamableId);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/streamables"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getUserStreamablesResult(userService.getCurrentUser().getId(), offset, limit);
	}

	@RequestMapping(value = {"/streamables/{id}/private"}, method = RequestMethod.PUT)
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
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getPrivateStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getPrivateStreamablesResult(offset, limit);
	}

	@RequestMapping(value = {"/feed"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getFeed(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return activityService.getUserFeedResult(offset, limit);
	}

	@RequestMapping(value = {"/liked"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getLikedStreamablesForUser(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getLikedStreamablesForUserResult(userService.getCurrentUser().getId(), offset, limit);
	}

	@RequestMapping(value = {"/social/friends"}, method = RequestMethod.GET)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserSocialFriendsContainerDto> getSocialFriends(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return userService.getSocialFriendsResult(offset, limit);
	}

	@RequestMapping(value = {"/social/{type}/friends"}, method = RequestMethod.GET)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public UserSocialFriendsContainerDto getSocialFriends(
			@PathVariable("type") ExternalProviderType type,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		UserSocialFriendsContainer container = userService.getSocialFriendsForProviderResult(type, offset, limit);
		return mapper.map(container, UserSocialFriendsContainerDto.class);
	}

	@RequestMapping(value = {"/social/{type}/avatar"}, method = RequestMethod.PUT)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public AssetResult importSocialAvatar(@PathVariable("type") ExternalProviderType type) {
		AssetResult createAssetResult = new AssetResult();
		Asset asset = userService.importSocialAvatar(type);
		createAssetResult.setAsset(mapper.map(asset, AssetDto.class));
		return createAssetResult;
	}

	@RequestMapping(value = {"/mentions"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getMentions(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getUserMentionsResult(userService.getCurrentUser().getId(), offset, limit);
	}

	@RequestMapping(value = {"/whotofollow"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> whoToFollow(@RequestParam(value="offset", required = false) Integer offset,
												@RequestParam(value="limit", required = false) Integer limit) {
		return userService.findRecommendedUsersToFollow(offset, limit);
	}

	@RequestMapping(value = {"/notifications/readall"}, method = RequestMethod.PUT)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public  ActionCompletedResult readAllNotifications() {
		CountResult countResult = new CountResult();
		Long count = notificationService.getUnreadNotificationsCount();
		countResult.setCount(count);
		return new ActionCompletedResult();
	}
}

package com.graffitab.server.api.controller.streamable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graffitab.server.api.controller.user.UserStatusRequired;
import com.graffitab.server.api.dto.ActionCompletedResult;
import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.comment.CommentDto;
import com.graffitab.server.api.dto.comment.result.CreateCommentResult;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.dto.streamable.result.GetFullStreamableResult;
import com.graffitab.server.api.dto.user.UserDto;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User.AccountStatus;
import com.graffitab.server.service.streamable.CommentService;
import com.graffitab.server.service.streamable.StreamableService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/v1/streamables")
@Api(value="Posts")
public class StreamableApiController {

	@Resource
	private StreamableService streamableService;

	@Resource
	private CommentService commentService;

	@Resource
	private OrikaMapper mapper;

    @ApiOperation(value = "Post Info")
	@RequestMapping(value = {"/{id}"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult getStreamable(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable streamable = streamableService.getStreamable(streamableId);
		getFullStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
		return getFullStreamableResult;
	}

    @ApiOperation(value = "Like")
	@RequestMapping(value = {"/{id}/likes"}, method = RequestMethod.POST, produces = {"application/json"})
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult like(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable toLike = streamableService.like(streamableId);
		getFullStreamableResult.setStreamable(mapper.map(toLike, FullStreamableDto.class));
		return getFullStreamableResult;
	}

    @ApiOperation(value = "Unlike")
	@RequestMapping(value = {"/{id}/likes"}, method = RequestMethod.DELETE, produces = {"application/json"})
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult unlike(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable toUnlike = streamableService.unlike(streamableId);
		getFullStreamableResult.setStreamable(mapper.map(toUnlike, FullStreamableDto.class));
		return getFullStreamableResult;
	}

    @ApiOperation(value = "Likers")
	@RequestMapping(value = {"/{id}/likes"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getLikers(
			@PathVariable("id") Long streamableId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getLikersResult(streamableId, offset, limit);
	}

    @ApiOperation(value = "Post Comment")
	@CrossOrigin(origins = {"https://graffitab.com", "https://dev.graffitab.com"})
	@RequestMapping(value = {"/{id}/comments"}, method = RequestMethod.POST, produces = {"application/json"})
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateCommentResult postComment(
			@PathVariable("id") Long streamableId,
			@JsonProperty("comment") CommentDto commentDto) {
		CreateCommentResult createCommentResult = new CreateCommentResult();
		Comment comment = commentService.postComment(streamableId, commentDto.getText());
		createCommentResult.setComment(mapper.map(comment, CommentDto.class));
		return createCommentResult;
	}

    @ApiOperation(value = "Delete Comment")
	@RequestMapping(value = {"/{id}/comments/{commentId}"}, method = RequestMethod.DELETE, produces = {"application/json"})
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteComment(
			@PathVariable("id") Long streamableId,
			@PathVariable("commentId") Long commentId) {
		commentService.deleteComment(streamableId, commentId);
		return new ActionCompletedResult();
	}

    @ApiOperation(value = "Edit Comment")
	@RequestMapping(value = {"/{id}/comments/{commentId}"}, method = RequestMethod.PUT, produces = {"application/json"})
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateCommentResult editComment(
			@PathVariable("id") Long streamableId,
			@PathVariable("commentId") Long commentId,
			@JsonProperty("comment") CommentDto commentDto) {
		CreateCommentResult createCommentResult = new CreateCommentResult();
		Comment comment = commentService.editComment(streamableId, commentId, commentDto.getText());
		createCommentResult.setComment(mapper.map(comment, CommentDto.class));
		return createCommentResult;
	}

    @ApiOperation(value = "Comments")
	@RequestMapping(value = {"/{id}/comments"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<CommentDto> getComments(
			@PathVariable("id") Long streamableId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return commentService.getCommentsResult(streamableId, offset, limit);
	}

    @ApiOperation(value = "Newest Posts")
	@RequestMapping(value = {"/newest"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getNewestStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getNewestStreamablesResult(offset, limit);
	}

    @ApiOperation(value = "Popular Posts")
	@RequestMapping(value = {"/popular"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getPopularStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getPopularStreamablesResult(offset, limit);
	}

    @ApiOperation(value = "Flag")
	@RequestMapping(value = {"/{id}/flag"}, method = RequestMethod.PUT, produces = {"application/json"})
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult flag(@PathVariable("id") Long streamableId, Locale locale) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable streamable = streamableService.flag(streamableId, locale);
		getFullStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
		return getFullStreamableResult;
	}

    @ApiOperation(value = "Search by Location")
	@RequestMapping(value = {"/search/location"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> searchStreamablesAtLocation(
			@RequestParam(value="latitude", required = true) Double latitude,
			@RequestParam(value="longitude", required = true) Double longitude,
			@RequestParam(value="radius", required = true) int radius) {
		return streamableService.searchStreamablesAtLocationResult(latitude, longitude, radius);
	}

    @ApiOperation(value = "Search by Hashtag")
	@RequestMapping(value = {"/search/hashtag"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> searchStreamablesForHashtag(
			@RequestParam(value="query", required = true) String query,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.searchStreamablesForHashtagResult(query, offset, limit);
	}

    @ApiOperation(value = "Search Hashtags")
	@RequestMapping(value = {"/search/hashtags"}, method = RequestMethod.GET, produces = {"application/json"})
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<String> searchHashtags(
			@RequestParam(value="query", required = true) String query,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.searchHashtags(query, offset, limit);
	}
}

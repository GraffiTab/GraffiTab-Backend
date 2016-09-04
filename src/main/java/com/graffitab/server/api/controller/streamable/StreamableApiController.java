package com.graffitab.server.api.controller.streamable;

import javax.annotation.Resource;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

import java.util.Locale;

@RestController
@RequestMapping("/api/streamables")
public class StreamableApiController {

	@Resource
	private StreamableService streamableService;

	@Resource
	private CommentService commentService;

	@Resource
	private OrikaMapper mapper;

	@RequestMapping(value = {"/{id}"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult getStreamable(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable streamable = streamableService.getStreamable(streamableId);
		getFullStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
		return getFullStreamableResult;
	}

	@RequestMapping(value = {"/{id}/likes"}, method = RequestMethod.POST)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult like(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable toLike = streamableService.like(streamableId);
		getFullStreamableResult.setStreamable(mapper.map(toLike, FullStreamableDto.class));
		return getFullStreamableResult;
	}

	@RequestMapping(value = {"/{id}/likes"}, method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult unlike(@PathVariable("id") Long streamableId) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable toUnlike = streamableService.unlike(streamableId);
		getFullStreamableResult.setStreamable(mapper.map(toUnlike, FullStreamableDto.class));
		return getFullStreamableResult;
	}

	@RequestMapping(value = {"/{id}/likes"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<UserDto> getLikers(
			@PathVariable("id") Long streamableId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getLikersResult(streamableId, offset, limit);
	}

	@RequestMapping(value = {"/{id}/comments"}, method = RequestMethod.POST)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public CreateCommentResult postComment(
			@PathVariable("id") Long streamableId,
			@JsonProperty("comment") CommentDto commentDto) {
		CreateCommentResult createCommentResult = new CreateCommentResult();
		Comment comment = commentService.postComment(streamableId, commentDto.getText());
		createCommentResult.setComment(mapper.map(comment, CommentDto.class));
		return createCommentResult;
	}

	@RequestMapping(value = {"/{id}/comments/{commentId}"}, method = RequestMethod.DELETE)
	@Transactional
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ActionCompletedResult deleteComment(
			@PathVariable("id") Long streamableId,
			@PathVariable("commentId") Long commentId) {
		commentService.deleteComment(streamableId, commentId);
		return new ActionCompletedResult();
	}

	@RequestMapping(value = {"/{id}/comments/{commentId}"}, method = RequestMethod.PUT)
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

	@RequestMapping(value = {"/{id}/comments"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<CommentDto> getComments(
			@PathVariable("id") Long streamableId,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return commentService.getCommentsResult(streamableId, offset, limit);
	}

	@RequestMapping(value = {"/newest"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getNewestStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getNewestStreamablesResult(offset, limit);
	}

	@RequestMapping(value = {"/popular"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> getPopularStreamables(
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.getPopularStreamablesResult(offset, limit);
	}

	@RequestMapping(value = {"/{id}/flag"}, method = RequestMethod.PUT)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public GetFullStreamableResult flag(@PathVariable("id") Long streamableId, Locale locale) {
		GetFullStreamableResult getFullStreamableResult = new GetFullStreamableResult();
		Streamable streamable = streamableService.flag(streamableId, locale);
		getFullStreamableResult.setStreamable(mapper.map(streamable, FullStreamableDto.class));
		return getFullStreamableResult;
	}

	@RequestMapping(value = {"/search/location"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> searchStreamablesAtLocation(
			@RequestParam(value="neLatitude", required = true) Double neLatitude,
			@RequestParam(value="neLongitude", required = true) Double neLongitude,
			@RequestParam(value="swLatitude", required = true) Double swLatitude,
			@RequestParam(value="swLongitude", required = true) Double swLongitude) {
		return streamableService.searchStreamablesAtLocationResult(neLatitude, neLongitude, swLatitude, swLongitude);
	}

	@RequestMapping(value = {"/search/hashtag"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<FullStreamableDto> searchStreamablesForHashtag(
			@RequestParam(value="query", required = true) String query,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.searchStreamablesForHashtagResult(query, offset, limit);
	}

	@RequestMapping(value = {"/search/hashtags"}, method = RequestMethod.GET)
	@Transactional(readOnly = true)
	@UserStatusRequired(value = AccountStatus.ACTIVE)
	public ListItemsResult<String> searchHashtags(
			@RequestParam(value="query", required = true) String query,
			@RequestParam(value="offset", required = false) Integer offset,
			@RequestParam(value="limit", required = false) Integer limit) {
		return streamableService.searchHashtags(query, offset, limit);
	}
}

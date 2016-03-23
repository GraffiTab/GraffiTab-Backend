package com.graffitab.server.service.streamable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.javatuples.Pair;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.comment.CommentDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.user.RunAsUser;
import com.graffitab.server.service.user.UserService;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class CommentService {

	private static final Pattern HASH_PATTERN = Pattern.compile("#(\\w+|\\W+)");
	private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+|\\W+)");

	@Resource
	private UserService userService;

	@Resource
	private StreamableService streamableService;

	@Resource
	private ActivityService activityService;

	@Resource
	private NotificationService notificationService;

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private PagingService pagingService;

	@Resource
	private HibernateDaoImpl<Streamable, Long> streamableDao;

	@Resource
	private HibernateDaoImpl<Comment, Long> commentDao;

	private ExecutorService executor = Executors.newFixedThreadPool(2);

	public Comment postComment(Long streamableId, String text) {
		Pair<Streamable, Comment> resultPair = transactionUtils.executeInTransactionWithResult(() -> {
			Streamable streamable = streamableService.findStreamableById(streamableId);

			if (streamable != null) {
				User currentUser = userService.getCurrentUser();

				Comment comment = Comment.comment();
				comment.setUser(currentUser);
				comment.setText(text);
				streamable.getComments().add(comment);

				return new Pair<Streamable, Comment>(streamable, comment);
			} else {
				throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
			}
		});

		Streamable streamable = resultPair.getValue0();
		Comment comment = resultPair.getValue1();

		// Add notification to the owner of the streamable.
		if (!streamable.getUser().equals(comment.getUser())) {
			notificationService.addCommentNotificationAsync(streamable.getUser(), comment.getUser(), streamable, comment);
		}

		// Process comment for hashtags and mentions.
		parseCommentForSpecialSymbols(comment, streamable);

		// Add activity to each follower of the user.
		activityService.addCommentActivityAsync(comment.getUser(), streamable, comment);

		return comment;
	}

	@Transactional
	public void deleteComment(Long streamableId, Long commentId) {
		Streamable streamable = streamableService.findStreamableById(streamableId);

		if (streamable != null) {
			Comment toDelete = findCommentById(commentId);

			if (toDelete != null) {
				// TODO: This will fail, as there is a link between the notifications and activities.
				streamable.getComments().remove(toDelete);
			}
			else {
				throw new RestApiException(ResultCode.COMMENT_NOT_FOUND, "Comment with id " + commentId + " not found");
			}
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional
	public Comment editComment(Long streamableId, Long commentId, String newText) {
		Streamable streamable = streamableService.findStreamableById(streamableId);

		if (streamable != null) {
			Comment toEdit = findCommentById(commentId);

			if (toEdit != null) {
				User currentUser = userService.getCurrentUser();

				if (currentUser.equals(toEdit.getUser())) {
					toEdit.setText(newText);
					toEdit.setEditDate(new DateTime());
					return toEdit;
				}
				else {
					throw new RestApiException(ResultCode.USER_NOT_OWNER, "The comment with id " + commentId + " cannot be edited by user with id " + currentUser.getId());
				}
			}
			else {
				throw new RestApiException(ResultCode.COMMENT_NOT_FOUND, "Comment with id " + commentId + " not found");
			}
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional
	public ListItemsResult<CommentDto> getCommentsResult(Long streamableId, Integer offset, Integer count) {
		Streamable streamable = streamableService.findStreamableById(streamableId);

		if (streamable != null) {
			Query query = streamableDao.createNamedQuery("Comment.getComments");
			query.setParameter("currentStreamable", streamable);

			return pagingService.getPagedItems(Comment.class, CommentDto.class, offset, count, query);
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
	}

	@Transactional(readOnly = true)
	public Comment findCommentById(Long id) {
		return commentDao.find(id);
	}

	private void parseCommentForSpecialSymbols(Comment comment, Streamable streamable) {
		User currentUser = userService.getCurrentUser();
		executor.submit(() -> {

			if (log.isDebugEnabled()) {
				log.debug("About to parse comment " + comment);
			}

			try {
				RunAsUser.set(currentUser);

				Matcher mentionMatcher = MENTION_PATTERN.matcher(comment.getText());
				Matcher hashtagMatcher = HASH_PATTERN.matcher(comment.getText());

		    	// Parse for mentions.
		    	while(mentionMatcher.find()) {
		    		transactionUtils.executeInTransaction(() -> {
		    			String match = mentionMatcher.group(1);

						User foundUser = userService.findByUsername(match);
						if (foundUser != null) {
							if (!foundUser.equals(currentUser)) { // User can mention himself without notifications.
								notificationService.addMentionNotificationAsync(foundUser, currentUser, streamable);
							}
						}
						else if (log.isDebugEnabled()) {
							log.debug("Non-existing user found '" + match + "'");
						}
					});
		    	}

		    	// Parse for hashtags.
		    	while (hashtagMatcher.find()) {
		    		transactionUtils.executeInTransaction(() -> {
		    			String match = hashtagMatcher.group(1);

		    			if (!streamableService.hashtagExistsForStreamable(match, streamable)) {
		    				Streamable inner = streamableService.findStreamableById(streamable.getId());
			    			inner.getHashtags().add(match);
		    			}
		    		});
		    	}

		    	if (log.isDebugEnabled()) {
		    		log.debug("Finished parsing comment");
		    	}
			} catch (Throwable t) {
				log.error("Error parsing comment", t);
			} finally {
				RunAsUser.clear();
			}
		});
	}
}

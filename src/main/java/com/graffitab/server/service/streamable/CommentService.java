package com.graffitab.server.service.streamable;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.comment.CommentDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.service.PagingService;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.user.UserService;

@Service
public class CommentService {

	@Resource
	private UserService userService;

	@Resource
	private StreamableService streamableService;

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

	public Comment postComment(Long streamableId, String text) {
		Streamable checked = transactionUtils.executeInTransactionWithResult(() -> {
			return streamableService.findStreamableById(streamableId);
		});

		if (checked != null) {
			Comment comment = transactionUtils.executeInTransactionWithResult(() -> {
				Streamable streamable = streamableService.findStreamableById(streamableId);
				User currentUser = userService.getCurrentUser();

				Comment newComment = Comment.comment();
				newComment.setUser(currentUser);
				newComment.setText(text);
				streamable.getComments().add(newComment);

				if (!streamable.getUser().equals(currentUser)) {
					// Send notification.
					notificationService.addCommentNotification(streamable.getUser(), currentUser, streamable, newComment);
				}

				return newComment;
			});

			return comment;
		} else {
			throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
		}
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
			Query query = streamableDao.createQuery(
					"select c "
				  + "from Streamable s "
				  + "join s.comments c "
				  + "where s = :currentStreamable");
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
}

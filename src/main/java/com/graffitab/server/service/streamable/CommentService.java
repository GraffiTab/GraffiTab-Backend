package com.graffitab.server.service.streamable;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.comment.CommentDto;
import com.graffitab.server.api.errors.RestApiException;
import com.graffitab.server.api.errors.ResultCode;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.ActivityService;
import com.graffitab.server.service.TextUtilsService;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.notification.NotificationService;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.user.UserService;

import org.hibernate.Query;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class CommentService {

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

    @Resource
    private TextUtilsService textUtilsService;

    public Comment postComment(Long streamableId, String text) {
        textUtilsService.validateText(text);
        Comment resultPair = transactionUtils.executeInTransactionWithResult(() -> {
            Streamable streamable = streamableService.findStreamableById(streamableId);
            if (streamable != null) {
                if (log.isDebugEnabled()) {
                    log.debug("The text of the comment to be persisted is: " + text);
                }
                User currentUser = userService.findUserById(userService.getCurrentUser().getId());
                Comment comment = Comment.comment();
                comment.setUser(currentUser);
                comment.setText(text);
                comment.setStreamable(streamable);
                streamable.getComments().add(comment);
//                return new Pair<>(streamable, comment);
                return comment;
            } else {
                throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
            }
        });

//        Streamable streamable = resultPair.getValue0();
//        Comment comment = resultPair.getValue1();
//
//        // Add notification to the owner of the streamable.
//        if (!streamable.getUser().equals(comment.getUser())) {
//            notificationService.addCommentNotification(streamable.getUser(), comment.getUser(), streamable, comment, false);
//        }
//
//        // Process comment for hashtags and mentions.
//        textUtilsService.parseCommentForSpecialSymbols(comment, streamable);
//
//        // Add activity to each follower of the user.
//        activityService.addCommentActivityAsync(comment.getUser(), streamable, comment);

        return resultPair;
    }

    @Transactional
    public void deleteComment(Long streamableId, Long commentId) {
        Streamable streamable = streamableService.findStreamableById(streamableId);

        if (streamable != null) {
            Comment toDelete = findCommentById(commentId);

            if (toDelete != null) {
                User currentUser = userService.getCurrentUser();

                if (currentUser.equals(toDelete.getUser())) {
                    toDelete.setIsDeleted(true);
                } else {
                    throw new RestApiException(ResultCode.USER_NOT_OWNER, "The comment with id " + commentId + " cannot be edited by user with id " + currentUser.getId());
                }
            } else {
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
                    toEdit.setUpdatedOn(new DateTime());
                    return toEdit;
                } else {
                    throw new RestApiException(ResultCode.USER_NOT_OWNER, "The comment with id " + commentId + " cannot be edited by user with id " + currentUser.getId());
                }
            } else {
                throw new RestApiException(ResultCode.COMMENT_NOT_FOUND, "Comment with id " + commentId + " not found");
            }
        } else {
            throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
        }
    }

    @Transactional(readOnly = true)
    public ListItemsResult<CommentDto> getCommentsResult(Long streamableId, Integer offset, Integer limit) {
        Streamable streamable = streamableService.findStreamableById(streamableId);

        if (streamable != null) {
            Query query = streamableDao.createNamedQuery("Comment.getComments");
            query.setParameter("currentStreamable", streamable);

            return pagingService.getPagedItems(Comment.class, CommentDto.class, offset, limit, query);
        } else {
            throw new RestApiException(ResultCode.STREAMABLE_NOT_FOUND, "Streamable with id " + streamableId + " not found");
        }
    }

    @Transactional(readOnly = true)
    public Comment findCommentById(Long id) {
        return commentDao.find(id);
    }

}

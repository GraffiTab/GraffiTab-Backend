package com.graffitab.server.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.activity.Activity;
import com.graffitab.server.persistence.model.activity.ActivityComment;
import com.graffitab.server.persistence.model.activity.ActivityCreateStreamable;
import com.graffitab.server.persistence.model.activity.ActivityFollow;
import com.graffitab.server.persistence.model.activity.ActivityLike;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.user.UserService;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class ActivityService {

	@Resource
	private UserService userService;

	@Resource
	private PagingService pagingService;

	@Resource
	private OrikaMapper mapper;

	@Resource
	private TransactionUtils transactionUtils;

	private ExecutorService executor = Executors.newFixedThreadPool(2);

	public void addCreateStreamableActivityAsync(User creator, Streamable createdStreamable) {
		Activity activity = new ActivityCreateStreamable(creator, createdStreamable);
		addUserActivityToFollowersAsync(creator, activity);
	}

	public void addFollowActivityAsync(User follower, User followed) {
		Activity activity = new ActivityFollow(followed, follower);
		addUserActivityToFollowersAsync(follower, activity);
	}

	public void addLikeActivityAsync(User liker, Streamable likedStreamable) {
		Activity activity = new ActivityLike(liker, likedStreamable);
		addUserActivityToFollowersAsync(liker, activity);
	}

	public void addCommentActivityAsync(User commenter, Streamable commentedStreamable, Comment comment) {
		Activity activity = new ActivityComment(commenter, commentedStreamable, comment);
		addUserActivityToFollowersAsync(commenter, activity);
	}

	private void addUserActivityToFollowersAsync(User user, Activity activity) {
		executor.execute(() -> {
			if (log.isDebugEnabled()) {
				log.debug("About to add activity " + activity + " to followers of user " + user);
			}

			// Find all followers for the given user.
			List<Long> followersIds = userService.getUserFollowersIds(user);

			// Add the user activity to each follower.
			followersIds.forEach(userId -> {
				transactionUtils.executeInTransaction(() -> {
					User follower = userService.findUserById(userId);
					follower.getActivity().add(activity);
				});
			});

			if (log.isDebugEnabled()) {
				log.debug("Finished adding activity");
			}
		});
	}
}

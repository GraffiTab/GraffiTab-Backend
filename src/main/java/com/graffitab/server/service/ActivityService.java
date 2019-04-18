package com.graffitab.server.service;

import com.graffitab.server.api.dto.ListItemsResult;
import com.graffitab.server.api.dto.activity.ActivityContainerDto;
import com.graffitab.server.api.dto.streamable.FullStreamableDto;
import com.graffitab.server.api.mapper.OrikaMapper;
import com.graffitab.server.persistence.dao.HibernateDaoImpl;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.activity.Activity;
import com.graffitab.server.persistence.model.activity.ActivityComment;
import com.graffitab.server.persistence.model.activity.ActivityCreateStreamable;
import com.graffitab.server.persistence.model.activity.ActivityFollow;
import com.graffitab.server.persistence.model.activity.ActivityLike;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.job.JobService;
import com.graffitab.server.service.paging.ActivityPagingService;
import com.graffitab.server.service.paging.PagingService;
import com.graffitab.server.service.user.UserService;

import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class ActivityService {

	@Resource
	private UserService userService;

	@Resource
	private ActivityPagingService activityPagingService;

	@Resource
	private HibernateDaoImpl<Activity, Long> activityDao;

	@Resource
	private OrikaMapper mapper;

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private PagingService pagingService;

	@Autowired
	private HttpServletRequest request;

	@Resource
	private JobService jobService;

	@Transactional(readOnly = true)
	public ListItemsResult<FullStreamableDto> getUserFeedResult(Integer offset, Integer limit) {
		User currentUser = userService.getCurrentUser();

		Query query = activityDao.createNamedQuery("Activity.getUserFeed");
		query.setParameter("currentUser", currentUser);

		return pagingService.getPagedItems(Streamable.class, FullStreamableDto.class, offset, limit, query);
	}

	@Transactional(readOnly = true)
	public ListItemsResult<ActivityContainerDto> getFollowersActivityResult(Integer numberOfItemsInGroup, Integer offset, Integer limit) {
		User currentUser = userService.getCurrentUser();

		Query query = activityDao.createNamedQuery("Activity.getFollowersActivity");
		query.setParameter("currentUser", currentUser);

		return activityPagingService.getPagedItems(Activity.class, ActivityContainerDto.class, numberOfItemsInGroup, offset, limit, query);
	}

	public void addCreateStreamableActivityAsync(User currentUser, Streamable createdStreamable) {
		Activity activity = new ActivityCreateStreamable(createdStreamable);
		addUserActivityToFollowersAsync(currentUser, activity);
	}

	public void addFollowActivityAsync(User currentUser, User followed) {
		Activity activity = new ActivityFollow(followed);
		addUserActivityToFollowersAsync(currentUser, activity);
	}

	public void addLikeActivityAsync(User currentUser, Streamable likedStreamable) {
		Activity activity = new ActivityLike(likedStreamable);
		addUserActivityToFollowersAsync(currentUser, activity);
	}

	public void addCommentActivityAsync(User currentUser, Streamable commentedStreamable, Comment comment) {
		Activity activity = new ActivityComment(commentedStreamable, comment);
		addUserActivityToFollowersAsync(currentUser, activity);
	}

	private void addUserActivityToFollowersAsync(User currentUser, Activity activity) {
		String userAgent = request.getHeader("User-Agent");

		// Is client behind something
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
		    ipAddress = request.getRemoteAddr();
		}

		activity.setUserAgent(userAgent);
		activity.setIpAddress(ipAddress);

		jobService.execute(() -> {
			if (log.isDebugEnabled()) {
				log.debug("About to add activity " + activity + " to user " + currentUser);
			}

			// Add the user activity to each follower.
			transactionUtils.executeInTransaction(() -> {
				User inner = userService.findUserById(currentUser.getId());
				inner.getActivity().add(activity);
			});

			if (log.isDebugEnabled()) {
				log.debug("Finished adding activity");
			}
		});
	}

	@Transactional
	public void removeAll() {
		activityDao.createQuery("delete from Activity").executeUpdate();
	}
}

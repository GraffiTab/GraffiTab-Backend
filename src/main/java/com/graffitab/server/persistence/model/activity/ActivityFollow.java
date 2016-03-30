package com.graffitab.server.persistence.model.activity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

import com.graffitab.server.persistence.model.user.User;

@Getter
@Setter
@Entity
@DiscriminatorValue("FOLLOW")
public class ActivityFollow extends Activity {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "followed_user_id")
	private User followed;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "follower_id")
	private User follower;

	public ActivityFollow() {
		super(ActivityType.FOLLOW);
	}

	public ActivityFollow(User followed, User follower) {
		super(ActivityType.FOLLOW);

		this.followed = followed;
		this.follower = follower;
	}

	@Override
	public boolean isSameActivity(Activity other) {
		if (!isSameTypeOfActivity(other)) {
			return false;
		}

		ActivityFollow activityFollow = (ActivityFollow) other;
		return activityFollow.getFollowed().equals(this.followed);
	}

	@Override
	public User getActivityUser() {
		return this.follower;
	}
}

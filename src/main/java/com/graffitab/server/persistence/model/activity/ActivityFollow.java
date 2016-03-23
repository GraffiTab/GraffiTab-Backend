package com.graffitab.server.persistence.model.activity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.graffitab.server.persistence.model.user.User;

import lombok.Getter;
import lombok.Setter;

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
}

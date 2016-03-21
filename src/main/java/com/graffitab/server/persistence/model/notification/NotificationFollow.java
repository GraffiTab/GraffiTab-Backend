package com.graffitab.server.persistence.model.notification;

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
public class NotificationFollow extends Notification {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "follower_id")
	private User follower;

	public NotificationFollow() {
		super(NotificationType.FOLLOW);
	}

	public NotificationFollow(User follower) {
		super(NotificationType.FOLLOW);

		this.follower = follower;
	}
}

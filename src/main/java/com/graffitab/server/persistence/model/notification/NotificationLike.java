package com.graffitab.server.persistence.model.notification;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("LIKE")
public class NotificationLike extends Notification {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "liker_id")
	private User liker;

	@OneToOne(targetEntity = Streamable.class)
	@JoinColumn(name = "liked_item_id")
	private Streamable likedStreamable;

	public NotificationLike() {
		super(NotificationType.LIKE);
	}

	public NotificationLike(User liker, Streamable likedStreamable) {
		super(NotificationType.LIKE);

		this.liker = liker;
		this.likedStreamable = likedStreamable;
	}
}

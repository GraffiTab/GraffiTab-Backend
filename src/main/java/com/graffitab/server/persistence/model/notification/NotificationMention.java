package com.graffitab.server.persistence.model.notification;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.graffitab.server.persistence.model.User;
import com.graffitab.server.persistence.model.streamable.Streamable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("MENTION")
public class NotificationMention extends Notification {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "mentioner_id")
	private User mentioner;

	@OneToOne(targetEntity = Streamable.class)
	@JoinColumn(name = "mentioned_item_id")
	private Streamable mentionedStreamable;

	public NotificationMention() {
		super(NotificationType.MENTION);
	}

	public NotificationMention(User mentioner, Streamable mentionedStreamable) {
		super(NotificationType.MENTION);

		this.mentioner = mentioner;
		this.mentionedStreamable = mentionedStreamable;
	}
}

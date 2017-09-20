package com.graffitab.server.persistence.model.notification;

import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Getter
@Setter
@Entity
//TODO: create a new class for MENTION_STREAMABLE
@DiscriminatorValue("MENTION")
public class NotificationMention extends Notification {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "mentioner_id")
	private User mentioner;

	@OneToOne(targetEntity = Streamable.class)
	@JoinColumn(name = "mentioned_item_id")
	private Streamable mentionedStreamable;

	@OneToOne(targetEntity = Comment.class)
	@JoinColumn(name = "mentioned_comment_id")
	private Comment mentionedComment;

	public NotificationMention() {
		super(NotificationType.MENTION);
	}

	public NotificationMention(User mentioner, Streamable mentionedStreamable, Comment comment) {
		super(NotificationType.MENTION);

		this.mentioner = mentioner;
		this.mentionedStreamable = mentionedStreamable;
		this.mentionedComment = comment;
	}

	public NotificationMention(User mentioner, Streamable mentionedStreamable) {
		super(NotificationType.MENTION);

		this.mentioner = mentioner;
		this.mentionedStreamable = mentionedStreamable;
	}
}

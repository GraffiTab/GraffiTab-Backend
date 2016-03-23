package com.graffitab.server.persistence.model.activity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("COMMENT")
public class ActivityComment extends Activity {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = User.class)
	@JoinColumn(name = "commenter_id")
	private User commenter;

	@OneToOne(targetEntity = Streamable.class)
	@JoinColumn(name = "commented_item_id")
	private Streamable commentedStreamable;

	@OneToOne(targetEntity = Comment.class)
	@JoinColumn(name = "comment_id")
	private Comment comment;

	public ActivityComment() {
		super(ActivityType.COMMENT);
	}

	public ActivityComment(User commenter, Streamable commentedStreamable, Comment comment) {
		super(ActivityType.LIKE);

		this.commenter = commenter;
		this.commentedStreamable = commentedStreamable;
		this.comment = comment;
	}
}

package com.graffitab.server.persistence.model.activity;

import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.streamable.Streamable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("COMMENT")
public class ActivityComment extends Activity {

	private static final long serialVersionUID = 1L;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "commented_item_id")
	private Streamable commentedStreamable;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id")
	private Comment comment;

	public ActivityComment() {
		super(ActivityType.COMMENT);
	}

	public ActivityComment(Streamable commentedStreamable, Comment comment) {
		super(ActivityType.COMMENT);

		this.commentedStreamable = commentedStreamable;
		this.comment = comment;
	}

	@Override
	public boolean isSameActivity(Activity other) {
		if (!isSameTypeOfActivity(other)) {
			return false;
		}

		ActivityComment activityComment = (ActivityComment) other;
		return activityComment.getCommentedStreamable().equals(this.commentedStreamable);
	}
}

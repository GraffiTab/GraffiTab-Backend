package com.graffitab.server.persistence.model.activity;

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
@DiscriminatorValue("LIKE")
public class ActivityLike extends Activity {

	private static final long serialVersionUID = 1L;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "liked_item_id")
	private Streamable likedStreamable;

	public ActivityLike() {
		super(ActivityType.LIKE);
	}

	public ActivityLike(Streamable likedStreamable) {
		super(ActivityType.LIKE);

		this.likedStreamable = likedStreamable;
	}

	@Override
	public boolean isSameActivity(Activity other) {
		if (!isSameTypeOfActivity(other)) {
			return false;
		}

		ActivityLike activityLike = (ActivityLike) other;
		return activityLike.getLikedStreamable().equals(this.likedStreamable);
	}
}

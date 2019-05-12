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
@DiscriminatorValue("CREATE_STREAMABLE")
public class ActivityCreateStreamable extends Activity {

	private static final long serialVersionUID = 1L;

	@OneToOne(targetEntity = Streamable.class, fetch = FetchType.LAZY)
	@JoinColumn(name = "created_item_id")
	private Streamable createdStreamable;

	public ActivityCreateStreamable() {
		super(ActivityType.CREATE_STREAMABLE);
	}

	public ActivityCreateStreamable(Streamable createdStreamable) {
		super(ActivityType.CREATE_STREAMABLE);

		this.createdStreamable = createdStreamable;
	}

	@Override
	public boolean isSameActivity(Activity other) {
		ActivityCreateStreamable activityCreateStreamable = (ActivityCreateStreamable) other;
		return activityCreateStreamable.getCreatedStreamable().equals(this.createdStreamable);
	}
}

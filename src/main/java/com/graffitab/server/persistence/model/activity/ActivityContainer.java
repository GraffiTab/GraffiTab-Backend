package com.graffitab.server.persistence.model.activity;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.graffitab.server.persistence.model.activity.Activity.ActivityType;
import com.graffitab.server.persistence.model.user.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class ActivityContainer {

	private ActivityType activityType;
	private DateTime date;
	private User user;
	private List<Activity> activities = new ArrayList<>();
}

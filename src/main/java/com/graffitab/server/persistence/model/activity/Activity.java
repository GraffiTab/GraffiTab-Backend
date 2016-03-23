package com.graffitab.server.persistence.model.activity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.joda.time.DateTime;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@NamedQueries({
	@NamedQuery(
		name = "Activity.getFollowersActivity",
		query = "select a "
			  + "from User u "
			  + "join u.activity a "
			  + "where u = :currentUser "
			  + "order by a.date desc"
	)
})

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "activity")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "activity_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Activity implements Identifiable<Long> {

	private static final long serialVersionUID = 1L;

	public enum ActivityType {
		COMMENT, CREATE_STREAMABLE, FOLLOW, LIKE
	}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_type", nullable = false, insertable = false, updatable = false)
	private ActivityType activityType;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "date", nullable = false)
	private DateTime date;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public Activity() {

	}

	public Activity(ActivityType activityType) {
		this.activityType = activityType;
		this.date = new DateTime();
	}

	public boolean isSameTypeOfActivity(Activity other) {
		return this.activityType == other.activityType && getActivityUser().equals(other.getActivityUser());
	}

	public abstract boolean isSameActivity(Activity other);
	public abstract User getActivityUser();
}

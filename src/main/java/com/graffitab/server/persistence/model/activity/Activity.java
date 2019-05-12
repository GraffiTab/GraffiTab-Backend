package com.graffitab.server.persistence.model.activity;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@NamedQueries({
	@NamedQuery(
		name = "Activity.getFollowersActivity",
		query = "select a "
			  + "from User u "
			  + "join u.activity a "
			  + "left join a.createdStreamable s "
			  + "where u in (select following "
			  			  + "from User currentUser "
			  			  + "join currentUser.following following "
			  			  + "where currentUser = :currentUser) "
			  + "and (s is null or (s.isDeleted = 'N' " // Enforce rules for hidden items.
			  			  + "and s.isPrivate = 'N')) "
			  + "order by a.createdOn desc"
	),
	@NamedQuery(
		name = "Activity.getUserFeed",
		query = "select s " // Return the streamable only for now.
			  + "from User u "
			  + "join u.activity a "
			  + "left join a.createdStreamable s "
			  + "where (u in (select following "
			  			  + "from User currentUser "
			  			  + "join currentUser.following following "
			  			  + "where currentUser = :currentUser) "
			  + "or u = :currentUser) " // We would like the user's items to appear in their feed too.
			  + "and s is not null " // We only allow streamables in the feed for now.
			  			  + "and s.isDeleted = 'N' " // Enforce rules for hidden items.
			  			  + "and (s.isPrivate = 'N' or u = :currentUser) " // If the post is private, we want the its creator to be able to see it.
			  + "order by a.createdOn desc"
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", updatable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "activity_type", nullable = false, insertable = false, updatable = false)
	private ActivityType activityType;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "created_on", nullable = false)
	private DateTime createdOn;

	@Column(name = "ip_address")
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

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
		this.createdOn = new DateTime();
	}

	public boolean isSameTypeOfActivity(Activity other) {
		return this.activityType == other.activityType && user.equals(other.user);
	}

	public abstract boolean isSameActivity(Activity other);
}

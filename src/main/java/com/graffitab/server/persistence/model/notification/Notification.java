package com.graffitab.server.persistence.model.notification;

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.util.BooleanToStringConverter;
import com.graffitab.server.persistence.util.DateTimeToLongConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

//TODO: return only the notifications in the last month or so
@NamedQueries({
	@NamedQuery(
		name = "Notification.getNotifications",
		query = "select n "
			  + "from User u "
			  + "join u.notifications n "
			  + "where u = :currentUser " +
				"and n.createdOn > :createdOn "
			  + "order by n.createdOn desc"
	),
	@NamedQuery(
		name = "Notification.getUnreadNotificationsCount",
		query = "select count(n) "
			  + "from User u "
			  + "join u.notifications n "
			  + "where u = :currentUser and n.createdOn > :createdOn and n.isRead = 'N'"
	)
})

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "notification")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "notification_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Notification implements Identifiable<Long> {

	private static final long serialVersionUID = 1L;

	public enum NotificationType {
		COMMENT, FOLLOW, LIKE, MENTION, WELCOME
	}

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Convert(converter = BooleanToStringConverter.class)
	@Column(name = "is_read", nullable = false)
	private Boolean isRead;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, insertable = false, updatable = false)
	private NotificationType notificationType;

	@Convert(converter = DateTimeToLongConverter.class)
	@Column(name = "created_on", nullable = false)
	private DateTime createdOn;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public Notification() {

	}

	public Notification(NotificationType notificationType) {
		this.notificationType = notificationType;
		this.createdOn = new DateTime();
		this.isRead = false;
	}
}

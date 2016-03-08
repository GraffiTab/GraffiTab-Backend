package com.graffitab.server.persistence.model.notification;

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

import com.graffitab.server.persistence.dao.Identifiable;
import com.graffitab.server.persistence.util.BooleanToStringConverter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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

	@Column(name = "date", nullable = false)
	private Long date;

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
		this.isRead = false;
		this.notificationType = notificationType;
		this.date = System.currentTimeMillis();
	}
}

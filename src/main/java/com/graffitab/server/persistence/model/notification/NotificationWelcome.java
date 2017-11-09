package com.graffitab.server.persistence.model.notification;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@Builder
@DiscriminatorValue("WELCOME")
public class NotificationWelcome extends Notification {

	private static final long serialVersionUID = 1L;

	public NotificationWelcome() {
		super(NotificationType.WELCOME);
	}
}

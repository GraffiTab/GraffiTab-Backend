package com.graffitab.server.service.notification;

import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.user.User;

public interface NotificationSenderService {

	void sendNotification(User user, Notification notification);
}

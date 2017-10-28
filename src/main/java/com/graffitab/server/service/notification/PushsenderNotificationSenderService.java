package com.graffitab.server.service.notification;

import com.devsu.push.sender.service.sync.SyncAndroidPushService;
import com.graffitab.server.persistence.model.Comment;
import com.graffitab.server.persistence.model.Device;
import com.graffitab.server.persistence.model.Device.OSType;
import com.graffitab.server.persistence.model.notification.Notification;
import com.graffitab.server.persistence.model.notification.NotificationComment;
import com.graffitab.server.persistence.model.notification.NotificationFollow;
import com.graffitab.server.persistence.model.notification.NotificationLike;
import com.graffitab.server.persistence.model.notification.NotificationMention;
import com.graffitab.server.persistence.model.streamable.Streamable;
import com.graffitab.server.persistence.model.user.User;
import com.graffitab.server.service.ProxyUtilities;
import com.graffitab.server.service.TransactionUtils;
import com.graffitab.server.service.user.UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class PushsenderNotificationSenderService implements NotificationSenderService {

	private static Logger log = LogManager.getLogger();

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private UserService userService;

	@Value("${pn.apns.env:dev}")
	private String pnApnsEnv;

    // We are already running in a background thread here, so we can use the Sync versions of these.
	private SyncAndroidPushService androidService;
	private GraffitabSyncApplePushService appleService;

	private String PN_APNS_DEV_PASSWORD_ENVVAR_NAME = "PN_APNS_DEV_PASSWORD";
	private String PN_APNS_PROD_PASSWORD_ENVVAR_NAME = "PN_APNS_PROD_PASSWORD";
	private String PN_GCM_SENDER_KEY_ENVVAR_NAME = "PN_GCM_SENDER_KEY";

	@PostConstruct
	public void setup() throws IOException {

		Boolean isProduction = "prod".equals(pnApnsEnv);

		log.info("Using [" + (isProduction ? "production" : "development") + "]");

		String pnApnsCertificatePasswordEnvVarName =
			isProduction ? PN_APNS_PROD_PASSWORD_ENVVAR_NAME : PN_APNS_DEV_PASSWORD_ENVVAR_NAME;

		String apnsCertificatePassword = System.getenv(pnApnsCertificatePasswordEnvVarName);
		String gcmKey = System.getenv(PN_GCM_SENDER_KEY_ENVVAR_NAME);
		log.debug("Setting up GCM API key: {}", gcmKey);

		if (StringUtils.hasText(apnsCertificatePassword) && StringUtils.hasText(gcmKey)) {
			androidService = new SyncAndroidPushService(gcmKey);
			try {
				ClassPathResource resource = new ClassPathResource("certificates/APNS_Certificate_" + (isProduction ? "Prod" : "Dev") + ".p12");
				appleService = new GraffitabSyncApplePushService(resource.getInputStream(), apnsCertificatePassword, isProduction);
			} catch (IOException e) {
				log.error("Error reading APNS certificate", e);
			}
		} else {
			log.warn("apnsCertificatePassword and gcmKey are missing -- push notifications won't work");
		}
	}

	@Override
	public void sendNotification(User user, Notification notification) {
		if (log.isDebugEnabled()) {
			log.debug("About to send push notification to user " + user.getUsername());
		}

		try {
			// Build PN content.
			String title = "GraffiTab";
			String content = buildContentForNotification(notification);
			Map<String, String> metadata = buildMetadataMapForNotification(notification);

			List<Device> devices = transactionUtils.executeInTransactionWithResult(() -> {
				User innerUser = userService.findUserById(user.getId());
				ProxyUtilities.initializeCollection(innerUser.getDevices());
				return innerUser.getDevices();
			});

			// Send PN to each of the user's devices.
			for (Device device : devices) {
				if (device.getOsType() == OSType.ANDROID) {
					if (!androidService.sendPush(title, content, metadata, device.getToken())) {
					    handlePushNotificationError(user, device);
                    }
				}
				else if (device.getOsType() == OSType.IOS) {
					if (!appleService.sendPush(title, content, metadata, device.getToken())) {
                        handlePushNotificationError(user, device);
                    }
				}
			}
		} catch (Exception e) {
			String msg = "Error sending push notification";
			log.error(msg, e);
		}
	}

	private void handlePushNotificationError(User user, Device device) {
        log.error("Failed to send push notification to device id=" + device.getId() +
                ", type=" + device.getOsType() + ". Unregistering device");
        transactionUtils.executeInTransaction(() -> {
            User innerUser = userService.findUserById(user.getId());
            innerUser.getDevices().remove(device);
        });
        if (log.isDebugEnabled()) {
            log.debug("Unregistered device id=" + device.getId());
        }
    }

	// Notification content

	private Map<String, String> buildMetadataMapForNotification(Notification notification) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("type", notification.getNotificationType().name());

		switch (notification.getNotificationType()) {
			case COMMENT: {
				NotificationComment typedNotification = ((NotificationComment) notification);
				User user = typedNotification.getCommenter();
				Comment comment = typedNotification.getComment();
				Streamable streamable = typedNotification.getCommentedStreamable();

				metadata.put("commenterId", user.getId() + "");
				metadata.put("commenterName", user.getFirstName() + " " + user.getLastName());
				metadata.put("commentId", comment.getId() + "");
				metadata.put("commentedStreamableId", streamable.getId() + "");
				break;
			}
			case LIKE: {
				NotificationLike typedNotification = ((NotificationLike) notification);
				User user = typedNotification.getLiker();
				Streamable streamable = typedNotification.getLikedStreamable();

				metadata.put("likerId", user.getId() + "");
				metadata.put("likerName", user.getFirstName() + " " + user.getLastName());
				metadata.put("likedStreamableId", streamable.getId() + "");
				break;
			}
			case FOLLOW: {
				NotificationFollow typedNotification = ((NotificationFollow) notification);
				User user = typedNotification.getFollower();

				metadata.put("followerId", user.getId() + "");
				metadata.put("followerName", user.getFirstName() + " " + user.getLastName());
				break;
			}
			case MENTION: {
				NotificationMention typedNotification = ((NotificationMention) notification);
				User user = typedNotification.getMentioner();
				Comment comment = typedNotification.getMentionedComment();
				Streamable streamable = typedNotification.getMentionedStreamable();

				//TODO: refactor Notification mention as abstract for post / comment
				if (comment != null) {
					metadata.put("mentionedCommentId", comment.getId() + "");
				} else {
					metadata.put("mentionedPostId", streamable.getId() + "");
				}

				metadata.put("mentionerId", user.getId() + "");
				metadata.put("mentionerName", user.getFirstName() + " " + user.getLastName());
				metadata.put("mentionedStreamableId", streamable.getId() + "");
				break;
			}
			default:
				break;
		}
		return metadata;
	}

	private String buildContentForNotification(Notification notification) {
		// TODO: For now hardcode the messages, but attempt localization later on.
		switch (notification.getNotificationType()) {
			case COMMENT: {
				NotificationComment typedNotification = ((NotificationComment) notification);
				User user = typedNotification.getCommenter();
				return user.getFirstName() + " " + user.getLastName() + " commented on your post";
			}
			case LIKE: {
				NotificationLike typedNotification = ((NotificationLike) notification);
				User user = typedNotification.getLiker();
				return user.getFirstName() + " " + user.getLastName() + " liked your post";
			}
			case FOLLOW: {
				NotificationFollow typedNotification = ((NotificationFollow) notification);
				User user = typedNotification.getFollower();
				return user.getFirstName() + " " + user.getLastName() + " started following you";
			}
			case MENTION: {
				NotificationMention typedNotification = ((NotificationMention) notification);
				User user = typedNotification.getMentioner();
				Comment comment = typedNotification.getMentionedComment();
				String notificationText =  (comment != null && comment.getText() != null) ?
											" mentioned you in a comment" :
											" mentioned you in a post";
				return user.getFirstName() + " " + user.getLastName() + notificationText;
			}
			default:
				return "Welcome to GraffiTab!";
		}
	}
}

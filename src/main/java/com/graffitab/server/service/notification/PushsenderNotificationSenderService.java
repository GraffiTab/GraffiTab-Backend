package com.graffitab.server.service.notification;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.devsu.push.sender.service.async.AsyncAndroidPushService;
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

@Service
public class PushsenderNotificationSenderService implements NotificationSenderService {

	private static Logger log = LogManager.getLogger();

	@Resource
	private TransactionUtils transactionUtils;

	@Resource
	private UserService userService;

	@Value("${pn.apns.env:dev}")
	private String pnApnsEnv;

	private AsyncAndroidPushService androidService;
	private GraffitabAsyncApplePushService appleService;

	private String PN_APNS_DEV_PASSWORD_ENVVAR_NAME = "PN_APNS_DEV_PASSWORD";
	private String PN_APNS_PROD_PASSWORD_ENVVAR_NAME = "PN_APNS_PROD_PASSWORD";
	private String PN_GCM_SENDER_KEY_ENVVAR_NAME = "PN_GCM_SENDER_KEY";

	@PostConstruct
	public void setup() throws IOException {

		Boolean isProduction = "prod".equals(pnApnsEnv);

		log.info("Using [" + (isProduction ? "production" : "development") + "] Push Sender notification services");

		String pnApnsCertificatePasswordEnvVarName =
			isProduction ? PN_APNS_PROD_PASSWORD_ENVVAR_NAME : PN_APNS_DEV_PASSWORD_ENVVAR_NAME;

		String apnsCertificatePassword = System.getenv(pnApnsCertificatePasswordEnvVarName);
		String gcmKey = System.getenv(PN_GCM_SENDER_KEY_ENVVAR_NAME);
		log.debug("Setting up Push Sender with GCM API key: {}", gcmKey);

		if (StringUtils.hasText(apnsCertificatePassword) && StringUtils.hasText(gcmKey)) {
			androidService = new AsyncAndroidPushService(gcmKey);
			try {
				ClassPathResource resource = new ClassPathResource("certificates/APNS_Certificate_" + (isProduction ? "Prod" : "Dev") + ".p12");
				appleService = new GraffitabAsyncApplePushService(resource.getInputStream(), apnsCertificatePassword, isProduction);
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
			log.debug("[PUSHSENDER] About to send push notification to user " +
						user.getUsername() + " through PushSender");
		}

		try {
			// Build PN content.
			String title = "GraffiTab";
			String content = buildContentForNotification(notification);
			Map<String, String> metadata = buildMetadataMapForNotification(notification);

			List<Device> devices = transactionUtils.executeInTransactionWithResult(() -> {
				User innerUser = userService.findUserById(user.getId());
				ProxyUtilities.initializeObjectWithOneLevelCollections(innerUser.getDevices());
				return innerUser.getDevices();
			});

			// Send PN to each of the user's devices.
			for (Device device : devices) {
				if (device.getOsType() == OSType.ANDROID) {
					androidService.sendPush(title, content, metadata, device.getToken());
				}
				else if (device.getOsType() == OSType.IOS) {
					appleService.sendPush(title, content, metadata, device.getToken());
				}
			}
		} catch (Exception e) {
			String msg = "Error sending push notification through Pushsender";
			log.error(msg, e);
			throw new NotificationSenderException(msg, e);
		}
	}

	private Map<String, String> buildMetadataMapForNotification(Notification notification) {
		Map<String, String> metadata = new HashMap<String, String>();
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

				metadata.put("mentionerId", user.getId() + "");
				metadata.put("mentionerName", user.getFirstName() + " " + user.getLastName());
				metadata.put("mentionedCommentId", comment.getId() + "");
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
				Comment comment = typedNotification.getComment();
				return user.getFirstName() + " " + user.getLastName() + " commented on your graffiti: " + comment.getText();
			}
			case LIKE: {
				NotificationLike typedNotification = ((NotificationLike) notification);
				User user = typedNotification.getLiker();
				return user.getFirstName() + " " + user.getLastName() + " liked your graffiti";
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
				return user.getFirstName() + " " + user.getLastName() + " mentioned you in a comment: " + comment.getText();
			}
			default:
				return "Welcome to GraffiTab!";
		}
	}
}

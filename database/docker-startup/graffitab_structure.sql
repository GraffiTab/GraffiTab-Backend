# ************************************************************
# Sequel Pro SQL dump
# Version 4499
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.6.21)
# Database: digi269_graffitab
# Generation Time: 2015-10-07 11:11:16 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE graffitab;
USE graffitab;

# Dump of table activity
# ------------------------------------------------------------

DROP TABLE IF EXISTS `activity`;

CREATE TABLE `activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `date` bigint(20) NOT NULL,
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_activity_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table activityComment
# ------------------------------------------------------------

DROP TABLE IF EXISTS `activityComment`;

CREATE TABLE `activityComment` (
  `id` bigint(20) NOT NULL,
  `commenterId` bigint(20) NOT NULL,
  `commentedItemId` bigint(20) NOT NULL,
  `commentId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_activityComment_on_activity` FOREIGN KEY (`id`) REFERENCES `activity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table activityCreateStreamable
# ------------------------------------------------------------

DROP TABLE IF EXISTS `activityCreateStreamable`;

CREATE TABLE `activityCreateStreamable` (
  `id` bigint(20) NOT NULL,
  `createdItemId` bigint(20) NOT NULL,
  `creatorId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_activityCreateStreamable_on_activity` FOREIGN KEY (`id`) REFERENCES `activity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table activityFollow
# ------------------------------------------------------------

DROP TABLE IF EXISTS `activityFollow`;

CREATE TABLE `activityFollow` (
  `id` bigint(20) NOT NULL,
  `followedUserId` bigint(20) NOT NULL,
  `followerId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_activityFollow_on_activity` FOREIGN KEY (`id`) REFERENCES `activity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table activityLike
# ------------------------------------------------------------

DROP TABLE IF EXISTS `activityLike`;

CREATE TABLE `activityLike` (
  `id` bigint(20) NOT NULL,
  `likedItemId` bigint(20) NOT NULL,
  `likerId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_activityLike_on_activity` FOREIGN KEY (`id`) REFERENCES `activity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table avatar
# ------------------------------------------------------------

DROP TABLE IF EXISTS `avatar`;

CREATE TABLE `avatar` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `image` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_avatar_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table comment
# ------------------------------------------------------------

DROP TABLE IF EXISTS `comment`;

CREATE TABLE `comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `itemId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  `text` varchar(5000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `date` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `itemId` (`itemId`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`itemId`),
  CONSTRAINT `del_comment_on_item` FOREIGN KEY (`itemId`) REFERENCES `streamable` (`id`) ON DELETE CASCADE,
  CONSTRAINT `del_comment_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table conversation
# ------------------------------------------------------------

DROP TABLE IF EXISTS `conversation`;

CREATE TABLE `conversation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `imageId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table conversationImage
# ------------------------------------------------------------

DROP TABLE IF EXISTS `conversationImage`;

CREATE TABLE `conversationImage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `conversationId` bigint(20) NOT NULL,
  `image` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `conversationId` (`conversationId`),
  KEY `id` (`id`,`conversationId`),
  CONSTRAINT `del_conversationImage_on_conversation` FOREIGN KEY (`conversationId`) REFERENCES `conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table conversationMessage
# ------------------------------------------------------------

DROP TABLE IF EXISTS `conversationMessage`;

CREATE TABLE `conversationMessage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `conversationId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  `text` varchar(10000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `date` bigint(20) NOT NULL,
  `state` varchar(11) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `conversationId` (`conversationId`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`conversationId`),
  CONSTRAINT `del_message_on_conversation` FOREIGN KEY (`conversationId`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `del_message_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table conversationMessageSeen
# ------------------------------------------------------------

DROP TABLE IF EXISTS `conversationMessageSeen`;

CREATE TABLE `conversationMessageSeen` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `messageId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  `date` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `messageId` (`messageId`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`messageId`),
  CONSTRAINT `del_conversationmessageseen_on_message` FOREIGN KEY (`messageId`) REFERENCES `digi269_digigraff`.`conversationMessage` (`id`) ON DELETE CASCADE,
  CONSTRAINT `del_conversationmessageseen_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table conversationUser
# ------------------------------------------------------------

DROP TABLE IF EXISTS `conversationUser`;

CREATE TABLE `conversationUser` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `conversationId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `conversationId` (`conversationId`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`conversationId`),
  CONSTRAINT `del_conversationuser_on_user` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table cover
# ------------------------------------------------------------

DROP TABLE IF EXISTS `cover`;

CREATE TABLE `cover` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `image` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_cover_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table device
# ------------------------------------------------------------

DROP TABLE IF EXISTS `device`;

CREATE TABLE `device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `token` varchar(5000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `os` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_device_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table following
# ------------------------------------------------------------

DROP TABLE IF EXISTS `following`;

CREATE TABLE `following` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `followingId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `followingId` (`followingId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_following_on_person_1` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE,
  CONSTRAINT `del_following_on_person_2` FOREIGN KEY (`followingId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table graffiti
# ------------------------------------------------------------

DROP TABLE IF EXISTS `graffiti`;

CREATE TABLE `graffiti` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tagId` bigint(11) NOT NULL,
  `image` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tagId` (`tagId`),
  KEY `id` (`id`,`tagId`),
  CONSTRAINT `del_graffiti_on_tag` FOREIGN KEY (`tagId`) REFERENCES `streamable` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table hashtag
# ------------------------------------------------------------

DROP TABLE IF EXISTS `hashtag`;

CREATE TABLE `hashtag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `itemId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `itemId` (`itemId`),
  KEY `tag` (`tag`),
  KEY `id` (`id`,`itemId`),
  CONSTRAINT `del_hashtag_on_item` FOREIGN KEY (`itemId`) REFERENCES `streamable` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table homeStream
# ------------------------------------------------------------

DROP TABLE IF EXISTS `homeStream`;

CREATE TABLE `homeStream` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `itemId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `itemId` (`itemId`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_homeStream_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='This table corresponds to the streams for all users. This means that all content that the user creates, along with all content generated by the user''s followees, will be inserted here. This can be used for showing a Stream or Home page.';



# Dump of table likes
# ------------------------------------------------------------

DROP TABLE IF EXISTS `likes`;

CREATE TABLE `likes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `itemId` bigint(20) NOT NULL,
  `userId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `itemId` (`itemId`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`itemId`),
  CONSTRAINT `del_like_on_item` FOREIGN KEY (`itemId`) REFERENCES `streamable` (`id`) ON DELETE CASCADE,
  CONSTRAINT `del_like_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table location
# ------------------------------------------------------------

DROP TABLE IF EXISTS `location`;

CREATE TABLE `location` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `address` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_location_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table notification
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notification`;

CREATE TABLE `notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `isRead` int(11) NOT NULL DEFAULT '0',
  `date` bigint(20) NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_notification_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table notificationComment
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notificationComment`;

CREATE TABLE `notificationComment` (
  `id` bigint(20) NOT NULL,
  `commenterId` bigint(20) NOT NULL,
  `commentedItemId` bigint(20) NOT NULL,
  `commentId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_notificationComment_on_notification` FOREIGN KEY (`id`) REFERENCES `notification` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table notificationFollow
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notificationFollow`;

CREATE TABLE `notificationFollow` (
  `id` bigint(20) NOT NULL,
  `followerId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_notificationFollow_on_notification` FOREIGN KEY (`id`) REFERENCES `notification` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table notificationLike
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notificationLike`;

CREATE TABLE `notificationLike` (
  `id` bigint(20) NOT NULL,
  `likedItemId` bigint(20) NOT NULL,
  `likerId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_notificationLike_on_notification` FOREIGN KEY (`id`) REFERENCES `notification` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table notificationMention
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notificationMention`;

CREATE TABLE `notificationMention` (
  `id` bigint(20) NOT NULL,
  `mentionedItemId` bigint(20) NOT NULL,
  `mentionerId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_notificationMention_on_notification` FOREIGN KEY (`id`) REFERENCES `notification` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table notificationWelcome
# ------------------------------------------------------------

DROP TABLE IF EXISTS `notificationWelcome`;

CREATE TABLE `notificationWelcome` (
  `id` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_notificationWelcome_on_notification` FOREIGN KEY (`id`) REFERENCES `notification` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table person
# ------------------------------------------------------------

DROP TABLE IF EXISTS `person`;

CREATE TABLE `person` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `externalId` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `password` blob NOT NULL,
  `firstname` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `lastname` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `website` varchar(140) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `about` varchar(5000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatarId` bigint(20) DEFAULT NULL,
  `coverId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table session
# ------------------------------------------------------------

DROP TABLE IF EXISTS `session`;

CREATE TABLE `session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `sessionId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`),
  KEY `id` (`id`,`userId`),
  KEY `sessionId` (`sessionId`),
  KEY `userId_2` (`userId`,`sessionId`),
  CONSTRAINT `del_session_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table streamable
# ------------------------------------------------------------

DROP TABLE IF EXISTS `streamable`;

CREATE TABLE `streamable` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `date` bigint(20) NOT NULL,
  `type` varchar(11) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  `isPrivate` int(11) NOT NULL DEFAULT '0',
  `isFlagged` int(11) NOT NULL DEFAULT '0',
  `width` int(11) NOT NULL,
  `height` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `del_streamable_on_person` (`userId`),
  KEY `id` (`id`,`userId`),
  CONSTRAINT `del_streamable_on_person` FOREIGN KEY (`userId`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='This table corresponds to the stream for one user instance. This means that all media that that the user creates will be stored in here and can be used for displaying the user''s profile.';



# Dump of table streamableTag
# ------------------------------------------------------------

DROP TABLE IF EXISTS `streamableTag`;

CREATE TABLE `streamableTag` (
  `id` bigint(20) NOT NULL,
  `graffitiId` bigint(20) NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_streamableTag_on_streamable` FOREIGN KEY (`id`) REFERENCES `streamable` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table streamableVideo
# ------------------------------------------------------------

DROP TABLE IF EXISTS `streamableVideo`;

CREATE TABLE `streamableVideo` (
  `id` bigint(20) NOT NULL,
  `videoId` bigint(20) NOT NULL,
  KEY `id` (`id`),
  CONSTRAINT `del_streamableVideo_on_stream` FOREIGN KEY (`id`) REFERENCES `streamable` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table thumbnail
# ------------------------------------------------------------

DROP TABLE IF EXISTS `thumbnail`;

CREATE TABLE `thumbnail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `graffitiId` bigint(20) NOT NULL,
  `image` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `graffitiId` (`graffitiId`),
  KEY `id` (`id`,`graffitiId`),
  CONSTRAINT `del_thumbnail_on_graffiti` FOREIGN KEY (`graffitiId`) REFERENCES `graffiti` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



# Dump of table video
# ------------------------------------------------------------

DROP TABLE IF EXISTS `video`;

CREATE TABLE `video` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `videoId` bigint(11) NOT NULL,
  `video` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `videoId` (`videoId`),
  KEY `id` (`id`,`videoId`),
  CONSTRAINT `del_videoFile_on_video` FOREIGN KEY (`videoId`) REFERENCES `streamable` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

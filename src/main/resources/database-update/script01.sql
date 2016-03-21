--liquibase formatted sql
--changeset david:v100cs01
ALTER TABLE person MODIFY password varchar(100);

--changeset georgi:v100cs02
ALTER TABLE person MODIFY password varchar(300);

--changeset david:v100cs03
RENAME TABLE person TO gt_user;
ALTER TABLE gt_user CHANGE externalId external_id varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL;
ALTER TABLE gt_user CHANGE avatarId avatar_id bigint(20) DEFAULT NULL;
ALTER TABLE gt_user CHANGE coverId cover_id bigint(20) DEFAULT NULL;
ALTER TABLE gt_user ADD CONSTRAINT gt_user_avatar_fk FOREIGN KEY (avatar_id) REFERENCES avatar(id);
ALTER TABLE gt_user ADD CONSTRAINT gt_user_cover_fk FOREIGN KEY (avatar_id) REFERENCES cover(id);

--changeset david:v100cs04
ALTER TABLE avatar CHANGE image image varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL;
ALTER TABLE cover CHANGE image image varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL;

--changeset lucia:v100cs05
ALTER TABLE avatar DROP FOREIGN KEY del_avatar_on_person;
ALTER TABLE avatar DROP COLUMN userId;
ALTER TABLE cover DROP FOREIGN KEY del_cover_on_person;
ALTER TABLE cover DROP COLUMN userId;

--changeset lucia:v100cs06
ALTER TABLE graffiti DROP FOREIGN KEY del_graffiti_on_tag;
ALTER TABLE graffiti DROP COLUMN tagId;

--changeset georgi:v100cs07
alter table gt_user drop foreign key gt_user_avatar_fk;
alter table gt_user drop foreign key gt_user_cover_fk;
DROP TABLE avatar;
DROP TABLE cover;
CREATE TABLE asset (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	guid varchar(40) NOT NULL,
	asset_type varchar(20) NOT NULL,
	user_id bigint(20) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT asset_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id)
)

--changeset georgi:v100cs08
alter table asset add order_key int(11) not null;

--changeset georgi:v100cs09
alter table gt_user add guid varchar(40) NOT NULL;

--changeset david:v100cs10
ALTER TABLE following DROP FOREIGN KEY del_following_on_person_1;
ALTER TABLE following DROP FOREIGN KEY del_following_on_person_2;
ALTER TABLE following CHANGE followingId following_id bigint(20) not null;
ALTER TABLE following CHANGE userId user_id bigint(20) not null;
ALTER TABLE following ADD CONSTRAINT user_following_fk1 FOREIGN KEY (following_id) REFERENCES gt_user(id);
ALTER TABLE following ADD CONSTRAINT user_following_fk2 FOREIGN KEY (user_id) REFERENCES gt_user(id);

--changeset georgi:v100cs11
alter table gt_user add status varchar(50) not null;
CREATE TABLE gt_user_metadata (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	user_id bigint(20) NOT NULL,
	metadata_key varchar(50) NOT NULL,
	metadata_value varchar(300) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT gt_user_metadata_fk FOREIGN KEY (user_id) REFERENCES gt_user (id)
)

--changeset georgi:v100cs12
ALTER TABLE gt_user DROP COLUMN avatar_id;
ALTER TABLE gt_user DROP COLUMN cover_id;

--changeset georgi:v100cs13
ALTER TABLE gt_user DROP COLUMN external_id;

--changeset georgi:v100cs14
ALTER TABLE device DROP FOREIGN KEY del_device_on_person;
ALTER TABLE device CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE device MODIFY token varchar(1000) NOT NULL;
ALTER TABLE device CHANGE os os_type varchar(11) NOT NULL;

--changeset georgi:v100cs15
ALTER TABLE device ADD CONSTRAINT device_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);

--changeset georgi:v100cs16
alter table device add order_key int(11) not null;

--changeset georgi:v100cs17
ALTER TABLE activity DROP FOREIGN KEY del_activity_on_person;
ALTER TABLE activity CHANGE userId user_id bigint(20) NOT NULL;
alter table activity add commenter_id bigint(20);
alter table activity add commented_item_id bigint(20);
alter table activity add comment_id bigint(20);
alter table activity add created_item_id bigint(20);
alter table activity add creator_id bigint(20);
alter table activity add followed_user_id bigint(20);
alter table activity add follower_id bigint(20);
alter table activity add liked_item_id bigint(20);
alter table activity add liker_id bigint(20);
ALTER TABLE activity ADD CONSTRAINT activity_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);
drop table activityComment;
drop table activityCreateStreamable;
drop table activityFollow;
drop table activityLike;

--changeset georgi:v100cs18
ALTER TABLE notification DROP FOREIGN KEY del_notification_on_person;
ALTER TABLE notification CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE notification CHANGE isRead is_read int(11) NOT NULL DEFAULT 0;
alter table notification add commenter_id bigint(20);
alter table notification add commented_item_id bigint(20);
alter table notification add comment_id bigint(20);
alter table notification add follower_id bigint(20);
alter table notification add liked_item_id bigint(20);
alter table notification add liker_id bigint(20);
alter table notification add mentioned_item_id bigint(20);
alter table notification add mentioner_id bigint(20);
ALTER TABLE notification ADD CONSTRAINT notification_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);
drop table notificationComment;
drop table notificationFollow;
drop table notificationLike;
drop table notificationMention;
drop table notificationWelcome;

--changeset georgi:v100cs19
alter table following add order_key int(11) not null;

--changeset georgi:v100cs20
alter table notification add order_key int(11) not null;

--changeset georgi:v100cs21
alter table notification change type notification_type varchar(50) NOT NULL;

--changeset georgi:v100cs22
alter table notification change is_read is_read varchar(10) NOT NULL;

--changeset georgi:v100cs23
alter table notification change date date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL;

--changeset georgi:v100cs24
alter table notification change date date bigint(20) NOT NULL;

--changeset georgi:v100cs25
alter table notification change date date TIMESTAMP NOT NULL;

--changeset georgi:v100cs26
ALTER TABLE notification ADD CONSTRAINT notification_commenter_id FOREIGN KEY (commenter_id) REFERENCES gt_user(id);
ALTER TABLE notification ADD CONSTRAINT notification_follower_id FOREIGN KEY (follower_id) REFERENCES gt_user(id);
ALTER TABLE notification ADD CONSTRAINT notification_liker_id FOREIGN KEY (liker_id) REFERENCES gt_user(id);
ALTER TABLE notification ADD CONSTRAINT notification_mentioner_id FOREIGN KEY (mentioner_id) REFERENCES gt_user(id);

--changeset georgi:v100cs27
alter table notification change date date bigint(20) NOT NULL;

--changeset georgi:v100cs28
ALTER TABLE activity ADD CONSTRAINT activity_commenter_id FOREIGN KEY (commenter_id) REFERENCES gt_user(id);
ALTER TABLE activity ADD CONSTRAINT activity_creator_id FOREIGN KEY (creator_id) REFERENCES gt_user(id);
ALTER TABLE activity ADD CONSTRAINT activity_followed_user_id FOREIGN KEY (followed_user_id) REFERENCES gt_user(id);
ALTER TABLE activity ADD CONSTRAINT activity_follower_id FOREIGN KEY (follower_id) REFERENCES gt_user(id);
ALTER TABLE activity ADD CONSTRAINT activity_liker_id FOREIGN KEY (liker_id) REFERENCES gt_user(id);

--changeset georgi:v100cs29
ALTER TABLE asset DROP FOREIGN KEY asset_user_fk;
ALTER TABLE asset DROP COLUMN user_id;
ALTER TABLE asset DROP COLUMN order_key;

--changeset georgi:v100cs30
alter table gt_user add avatar_asset_id bigint(20);
alter table gt_user add cover_asset_id bigint(20);
ALTER TABLE gt_user ADD CONSTRAINT gt_user_avatar_asset_id FOREIGN KEY (avatar_asset_id) REFERENCES asset(id);
ALTER TABLE gt_user ADD CONSTRAINT gt_user_cover_asset_id FOREIGN KEY (cover_asset_id) REFERENCES asset(id);

--changeset david:v100cs31
ALTER TABLE session DROP FOREIGN KEY del_session_on_person;
ALTER TABLE session CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE session CHANGE sessionId session_id VARCHAR(150) DEFAULT NULL;
ALTER TABLE session ADD content BLOB DEFAULT NULL;
ALTER TABLE session ADD CONSTRAINT user_session_fk FOREIGN KEY (user_id) REFERENCES gt_user(id);
ALTER TABLE session ADD version int(11) NOT NULL;

--changeset david:v100cs32
ALTER TABLE thumbnail DROP FOREIGN KEY del_thumbnail_on_graffiti;
drop table video;
drop table graffiti;

--changeset georgi:v100cs33
RENAME TABLE thumbnail TO asset_thumbnail;
ALTER TABLE asset_thumbnail CHANGE graffitiId asset_id bigint(20) NOT NULL;
alter table asset_thumbnail add guid varchar(40) NOT NULL;
ALTER TABLE asset_thumbnail DROP COLUMN image;
ALTER TABLE asset_thumbnail ADD CONSTRAINT thumbnail_asset_id_fk FOREIGN KEY (asset_id) REFERENCES asset(id);

--changeset georgi:v100cs34
ALTER TABLE streamable DROP FOREIGN KEY del_streamable_on_person;
ALTER TABLE streamable CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE streamable CHANGE isPrivate is_private int(11) NOT NULL DEFAULT 0;
ALTER TABLE streamable CHANGE isFlagged is_flagged int(11) NOT NULL DEFAULT 0;
ALTER TABLE streamable DROP COLUMN width;
ALTER TABLE streamable DROP COLUMN height;
alter table streamable add asset_id bigint(20);
alter table streamable add latitude double;
alter table streamable add longitude double;
ALTER TABLE streamable ADD CONSTRAINT streamable_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);
drop table streamableTag;
drop table streamableVideo;

--changeset georgi:v100cs35
alter table streamable change is_private is_private varchar(10) NOT NULL;
alter table streamable change is_flagged is_flagged varchar(10) NOT NULL;
alter table streamable change asset_id asset_id bigint(20) NOT NULL;

--changeset georgi:v100cs36
alter table streamable change type streamable_type varchar(50) NOT NULL;

--changeset georgi:v100cs37
alter table streamable add order_key int(11) not null;

--changeset georgi:v100cs38
alter table streamable add roll DOUBLE;
alter table streamable add yaw DOUBLE;
alter table streamable add pitch DOUBLE;

--changeset georgi:v100cs39
ALTER TABLE likes DROP FOREIGN KEY del_like_on_item;
ALTER TABLE likes DROP FOREIGN KEY del_like_on_person;
ALTER TABLE likes CHANGE itemId streamable_id bigint(20) NOT NULL;
ALTER TABLE likes CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE likes ADD CONSTRAINT likes_streamable_fk FOREIGN KEY (streamable_id) REFERENCES streamable (id);
ALTER TABLE likes ADD CONSTRAINT likes_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);

--changeset georgi:v100cs40
alter table likes add order_key int(11) not null;

--changeset georgi:v100cs41
ALTER TABLE activity ADD CONSTRAINT activity_commented_item_fk FOREIGN KEY (commented_item_id) REFERENCES streamable(id);
ALTER TABLE activity ADD CONSTRAINT activity_created_item_fk FOREIGN KEY (created_item_id) REFERENCES streamable(id);
ALTER TABLE activity ADD CONSTRAINT activity_liked_item_fk FOREIGN KEY (liked_item_id) REFERENCES streamable(id);
ALTER TABLE notification ADD CONSTRAINT notification_commented_item_fk FOREIGN KEY (commented_item_id) REFERENCES streamable(id);
ALTER TABLE notification ADD CONSTRAINT notification_liked_item_fk FOREIGN KEY (liked_item_id) REFERENCES streamable(id);
ALTER TABLE notification ADD CONSTRAINT notification_mentioned_item_fk FOREIGN KEY (mentioned_item_id) REFERENCES streamable(id);

--changeset georgi:v100cs42
ALTER TABLE comment DROP FOREIGN KEY del_comment_on_item;
ALTER TABLE comment DROP FOREIGN KEY del_comment_on_person;
ALTER TABLE comment CHANGE itemId streamable_id bigint(20) NOT NULL;
ALTER TABLE comment CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE comment ADD CONSTRAINT comments_streamable_fk FOREIGN KEY (streamable_id) REFERENCES streamable (id);
ALTER TABLE comment ADD CONSTRAINT comments_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);
alter table comment add order_key int(11) not null;

--changeset georgi:v100cs43
ALTER TABLE likes RENAME TO gt_like;

--changeset georgi:v100cs44
ALTER TABLE activity ADD CONSTRAINT activity_comment_fk FOREIGN KEY (comment_id) REFERENCES comment(id);
ALTER TABLE notification ADD CONSTRAINT notification_comment_fk FOREIGN KEY (comment_id) REFERENCES comment(id);

--changeset georgi:v100cs45
alter table comment add edit_date bigint(20);

--changeset georgi:v100cs46
ALTER TABLE asset_thumbnail DROP FOREIGN KEY thumbnail_asset_id_fk;
drop table asset_thumbnail;

--changeset georgi:v100cs47
ALTER TABLE location DROP FOREIGN KEY del_location_on_person;
ALTER TABLE location CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE location ADD CONSTRAINT user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);
alter table location add order_key int(11) not null;

--changeset georgi:v100cs48
ALTER TABLE homeStream RENAME TO feed;
ALTER TABLE feed DROP FOREIGN KEY del_homeStream_on_person;
ALTER TABLE feed CHANGE userId user_id bigint(20) NOT NULL;
ALTER TABLE feed CHANGE itemId streamable_id bigint(20) NOT NULL;
ALTER TABLE feed ADD CONSTRAINT feed_user_fk FOREIGN KEY (user_id) REFERENCES gt_user (id);
ALTER TABLE feed ADD CONSTRAINT feed_streamable_fk FOREIGN KEY (streamable_id) REFERENCES streamable (id);
alter table feed add order_key int(11) not null;

--changeset georgi:v100cs49
ALTER TABLE hashtag CHANGE itemId streamable_id bigint(20) NOT NULL;
ALTER TABLE hashtag ADD CONSTRAINT hashtag_streamable_fk FOREIGN KEY (streamable_id) REFERENCES streamable (id);
alter table hashtag add order_key int(11) not null;

--changeset georgi:v100cs50
ALTER TABLE hashtag DROP FOREIGN KEY del_hashtag_on_item;
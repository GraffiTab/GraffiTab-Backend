--liquibase formatted sql
--changeset david:v200cs01
ALTER TABLE asset ADD url varchar(2000) DEFAULT NULL;

--changeset david:v200cs02
ALTER TABLE asset ADD thumbnail_url varchar(2000) DEFAULT NULL;

--changeset david:v200cs03
ALTER TABLE gt_user ADD is_recommendation int(11) NOT NULL DEFAULT 0;

--changeset david:v200cs04
INSERT INTO `gt_user` (`username`, `password`, `firstname`, `lastname`, `email`, `website`, `about`, `guid`, `status`, `avatar_asset_id`, `cover_asset_id`, `created_on`, `updated_on`, `failed_logins`) VALUES
	('flickr', '$2a$10$KN.KpJXANNas6eQqZvqQ1OzOqGSw.xwZ5BdQIYv8crTZhJFaDt6Dm', 'Flickr', 'Daily', 'flickr@mailinator.com', NULL, NULL, 'DB3xVHubQ6LRi', 'ACTIVE', null, null, 1503002477267, null, 0);

--changeset david:v200cs05
INSERT INTO `gt_user` (`username`, `password`, `firstname`, `lastname`, `email`, `website`, `about`, `guid`, `status`, `avatar_asset_id`, `cover_asset_id`, `created_on`, `updated_on`, `failed_logins`)
VALUES
	('deviantart', '$2a$10$7.ibgCqiPZK/9CkCeloLFeo5SGaJlBBIeuZ4iMRQDtwLFtCixaUxK', 'DeviantArt', 'Daily', 'admin@graffitab.com', NULL, NULL, 'U4wDNrcqY8PQI', 'ACTIVE', null, null, 1496501885743, null, 0);

--changeset david:v200cs06
alter table gt_user change is_recommendation is_recommendation varchar(10) NOT NULL DEFAULT 'N';
update gt_user set is_recommendation='Y' where username in ('flickr', 'deviantart')

--changeset david:v200cs07
update gt_user set is_recommendation='N' where is_recommendation='0'

--changeset david:v200cs08
alter table streamable add text varchar(5000) DEFAULT NULL;

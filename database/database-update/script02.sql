--liquibase formatted sql
--changeset david:v200cs01
ALTER TABLE asset ADD url varchar(2000) DEFAULT NULL;

--changeset david:v200cs02
ALTER TABLE asset ADD thumbnail_url varchar(2000) DEFAULT NULL;
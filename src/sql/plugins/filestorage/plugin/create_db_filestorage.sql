-- liquibase formatted sql
-- changeset filestorage:create_db_filestorage.sql
-- preconditions onFail:MARK_RAN onError:WARN

DROP TABLE IF EXISTS filestorage_file;
CREATE TABLE filestorage_file (
  id_file INT AUTO_INCREMENT,
  file_key VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(500) DEFAULT '' NOT NULL,
  mime_type VARCHAR(100) DEFAULT '' NOT NULL,
  file_size BIGINT DEFAULT 0 NOT NULL,
  date_creation TIMESTAMP NULL,
  creator VARCHAR(255) DEFAULT '' NOT NULL,
  PRIMARY KEY (id_file)
);

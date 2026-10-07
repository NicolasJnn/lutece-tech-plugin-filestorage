-- liquibase formatted sql
-- changeset filestorage:init_core_filestorage.sql
-- preconditions onFail:MARK_RAN onError:WARN

DELETE FROM core_admin_right WHERE id_right IN ( 'FILESTORAGE_MANAGEMENT', 'FILESTORAGE_INSERT' );
INSERT INTO core_admin_right (id_right,name,level_right,admin_url,description,is_updatable,plugin_name,id_feature_group,icon_url,documentation_url) VALUES
('FILESTORAGE_MANAGEMENT','filestorage.adminFeature.filestorage_management.name',1,'jsp/admin/plugins/filestorage/ManageStoredFiles.jsp','filestorage.adminFeature.filestorage_management.description',0,'filestorage','APPLICATIONS',NULL,NULL);
INSERT INTO core_admin_right (id_right,name,level_right,admin_url,description,is_updatable,plugin_name,id_feature_group,icon_url,documentation_url) VALUES
('FILESTORAGE_INSERT','filestorage.adminFeature.filestorage_insert.name',3,NULL,'filestorage.adminFeature.filestorage_insert.description',0,'filestorage',NULL,NULL,NULL);

DELETE FROM core_user_right WHERE id_right IN ( 'FILESTORAGE_MANAGEMENT', 'FILESTORAGE_INSERT' );
INSERT INTO core_user_right (id_right,id_user) VALUES ('FILESTORAGE_MANAGEMENT',1);
INSERT INTO core_user_right (id_right,id_user) VALUES ('FILESTORAGE_INSERT',1);

DELETE FROM core_admin_role WHERE role_key = 'filestorage_manager';
INSERT INTO core_admin_role (role_key,role_description) VALUES ('filestorage_manager','FILESTORAGE - Gestion des fichiers');

DELETE FROM core_admin_role_resource WHERE role_key = 'filestorage_manager';
INSERT INTO core_admin_role_resource (role_key,resource_type,resource_id,permission) VALUES ('filestorage_manager','FILESTORAGE_FILE','*','*');

DELETE FROM core_user_role WHERE role_key = 'filestorage_manager';
INSERT INTO core_user_role (role_key,id_user) VALUES ('filestorage_manager',1);

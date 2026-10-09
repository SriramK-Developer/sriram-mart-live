-- SriramMart – MySQL 8 setup. Run once as an administrator (for example: mysql -u root -p < db/mysql-setup.sql)
-- 1) database   2) application login (user id + password)   3) least-privilege grants
CREATE DATABASE IF NOT EXISTS srirammart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CHANGE THIS PASSWORD, then use the same value in DB_PASS
CREATE USER IF NOT EXISTS 'srirammart_app'@'%' IDENTIFIED BY 'ChangeMe@12345';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON srirammart.* TO 'srirammart_app'@'%';
FLUSH PRIVILEGES;

-- Tables are created automatically by the application on first start (Hibernate ddl-auto=update).
-- Start the app with:  SPRING_PROFILES_ACTIVE=mysql  DB_USER=srirammart_app  DB_PASS=<your password>

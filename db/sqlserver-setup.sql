-- SriramMart – Microsoft SQL Server setup. Run once with SSMS / sqlcmd as an administrator.
-- Creates the database, a SQL login (user id + password) and a database user for the application.
IF DB_ID(N'srirammart') IS NULL
    CREATE DATABASE srirammart;
GO

USE master;
GO
-- CHANGE THIS PASSWORD, then use the same value in DB_PASS
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'srirammart_app')
    CREATE LOGIN srirammart_app WITH PASSWORD = N'ChangeMe@12345', CHECK_POLICY = ON;
GO

USE srirammart;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'srirammart_app')
    CREATE USER srirammart_app FOR LOGIN srirammart_app;
GO
ALTER ROLE db_datareader ADD MEMBER srirammart_app;
ALTER ROLE db_datawriter ADD MEMBER srirammart_app;
ALTER ROLE db_ddladmin  ADD MEMBER srirammart_app;   -- lets the app create its tables on first start
GO

-- Start the app with:  SPRING_PROFILES_ACTIVE=sqlserver  DB_USER=srirammart_app  DB_PASS=<your password>
-- (TCP/IP must be enabled for SQL Server and mixed-mode authentication turned on.)

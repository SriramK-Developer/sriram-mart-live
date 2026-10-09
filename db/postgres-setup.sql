-- SriramMart – PostgreSQL setup script.
-- Tables and schema are automatically created/updated by Spring Boot (Hibernate ddl-auto=update).
-- To create the database locally or on a VPS:
CREATE DATABASE srirammart;

-- Create application user (if not using default postgres user)
CREATE USER srirammart_app WITH ENCRYPTED PASSWORD 'ChangeMe@12345';
GRANT ALL PRIVILEGES ON DATABASE srirammart TO srirammart_app;
ALTER DATABASE srirammart OWNER TO srirammart_app;

-- On Render, the PostgreSQL database and DATABASE_URL are provisioned automatically.

package com.srirammart.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${DB_URL:#{null}}")
    private String dbUrl;

    @Value("${spring.datasource.url:#{null}}")
    private String springDatasourceUrl;

    @Value("${DB_USER:#{null}}")
    private String dbUser;

    @Value("${DB_PASS:#{null}}")
    private String dbPass;

    @Bean
    @Primary
    public DataSource dataSource() {
        String rawUrl = getRawUrl();

        if (rawUrl != null && (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://"))) {
            log.info("Configuring PostgreSQL DataSource from URI: {}", maskUrl(rawUrl));
            return createPostgresDataSource(rawUrl);
        }

        if (rawUrl != null && rawUrl.startsWith("jdbc:postgresql:")) {
            log.info("Configuring PostgreSQL DataSource from JDBC URL: {}", maskUrl(rawUrl));
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(rawUrl);
            config.setDriverClassName("org.postgresql.Driver");
            if (dbUser != null && !dbUser.isBlank()) config.setUsername(dbUser);
            if (dbPass != null) config.setPassword(dbPass);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            return new HikariDataSource(config);
        }

        // Default: H2 or standard Spring DataSource (MySQL, SQL Server, H2)
        String jdbcUrl = rawUrl != null ? rawUrl : "jdbc:h2:file:./data/srirammart;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
        log.info("Configuring default DataSource: {}", maskUrl(jdbcUrl));
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (dbUser != null && !dbUser.isBlank()) config.setUsername(dbUser);
        else if (jdbcUrl.contains(":h2:")) config.setUsername("sa");
        if (dbPass != null) config.setPassword(dbPass);
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    private String getRawUrl() {
        if (databaseUrl != null && !databaseUrl.isBlank()) return databaseUrl.trim();
        if (dbUrl != null && !dbUrl.isBlank() && !dbUrl.contains("h2:file")) return dbUrl.trim();
        if (springDatasourceUrl != null && !springDatasourceUrl.isBlank() && !springDatasourceUrl.contains("h2:file")) return springDatasourceUrl.trim();
        if (dbUrl != null && !dbUrl.isBlank()) return dbUrl.trim();
        return springDatasourceUrl;
    }

    private DataSource createPostgresDataSource(String rawUri) {
        try {
            // Clean postgres:// or postgresql://
            String uriString = rawUri;
            if (uriString.startsWith("postgresql://")) {
                uriString = "postgres://" + uriString.substring("postgresql://".length());
            }
            URI uri = new URI(uriString);

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String dbName = (path != null && path.length() > 1) ? path.substring(1) : "srirammart";

            String username = null;
            String password = null;
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                username = parts[0];
                if (parts.length > 1) password = parts[1];
            }
            if (username == null && dbUser != null) username = dbUser;
            if (password == null && dbPass != null) password = dbPass;

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl += "?" + uri.getQuery();
            }

            log.info("Parsed PostgreSQL JDBC URL: {}", jdbcUrl);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setDriverClassName("org.postgresql.Driver");
            if (username != null) config.setUsername(username);
            if (password != null) config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            return new HikariDataSource(config);
        } catch (URISyntaxException e) {
            log.error("Failed to parse PostgreSQL URI: {}", rawUri, e);
            throw new IllegalArgumentException("Invalid database URI: " + rawUri, e);
        }
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^:@]+@", ":****@");
    }
}

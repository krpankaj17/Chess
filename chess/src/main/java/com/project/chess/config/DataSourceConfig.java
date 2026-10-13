package com.project.chess.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/chess_db}")
    private String defaultUrl;

    @Value("${spring.datasource.username:postgres}")
    private String defaultUsername;

    @Value("${spring.datasource.password:root}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = defaultUrl;
        }

        HikariConfig config = new HikariConfig();

        if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(dbUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    config.setUsername(URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                    config.setPassword(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                } else if (userInfo != null) {
                    config.setUsername(URLDecoder.decode(userInfo, StandardCharsets.UTF_8));
                }

                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath() != null && !uri.getPath().isEmpty() ? uri.getPath() : "/chess_db";
                String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path;

                String query = uri.getQuery();
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                }

                if (!jdbcUrl.contains("sslmode=")) {
                    jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
                }

                config.setJdbcUrl(jdbcUrl);
                System.out.println(">>> DataSourceConfig: Successfully parsed DATABASE_URL for host: " + uri.getHost());
            } catch (Exception e) {
                System.err.println(">>> DataSourceConfig: Error parsing DATABASE_URL: " + e.getMessage());
                config.setJdbcUrl(dbUrl);
                config.setUsername(defaultUsername);
                config.setPassword(defaultPassword);
            }
        } else {
            config.setJdbcUrl(dbUrl);
            config.setUsername(System.getenv("SPRING_DATASOURCE_USERNAME") != null ? System.getenv("SPRING_DATASOURCE_USERNAME") : defaultUsername);
            config.setPassword(System.getenv("SPRING_DATASOURCE_PASSWORD") != null ? System.getenv("SPRING_DATASOURCE_PASSWORD") : defaultPassword);
            System.out.println(">>> DataSourceConfig: Using JDBC URL: " + dbUrl);
        }

        String driver = dbUrl.startsWith("jdbc:h2:") ? "org.h2.Driver" : "org.postgresql.Driver";
        config.setDriverClassName(driver);

        return new HikariDataSource(config);
    }
}

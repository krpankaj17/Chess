package com.project.chess.config;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("DATABASE_URL");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }

        try {
            Map<String, Object> props = new HashMap<>();

            // Direct JDBC URL
            if (dbUrl.startsWith("jdbc:postgresql://")) {
                String jdbcUrl = dbUrl;
                if (!jdbcUrl.contains("sslmode=")) {
                    jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
                }
                props.put("SPRING_DATASOURCE_URL", jdbcUrl);
                props.put("spring.datasource.url", jdbcUrl);
                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrlProperties", props));
                System.out.println(">>> DatabaseEnvironmentPostProcessor: Configured JDBC URL from DATABASE_URL");
                return;
            }

            // Postgres URI format (Render or Supabase)
            if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                URI uri = new URI(dbUrl);
                String userInfo = uri.getUserInfo();
                String username = null;
                String password = null;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
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

                props.put("SPRING_DATASOURCE_URL", jdbcUrl);
                props.put("spring.datasource.url", jdbcUrl);
                if (username != null) {
                    props.put("SPRING_DATASOURCE_USERNAME", username);
                    props.put("spring.datasource.username", username);
                }
                if (password != null) {
                    props.put("SPRING_DATASOURCE_PASSWORD", password);
                    props.put("spring.datasource.password", password);
                }

                environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrlProperties", props));
                System.out.println(">>> DatabaseEnvironmentPostProcessor: Connected to database at " + uri.getHost());
            }
        } catch (Exception e) {
            System.err.println(">>> DatabaseEnvironmentPostProcessor error parsing DATABASE_URL: " + e.getMessage());
        }
    }
}

package com.vibe.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Support multiple origins via FRONTEND_ORIGINS (comma separated) or legacy FRONTEND_ORIGIN
        String multi = System.getenv("FRONTEND_ORIGINS");
        if (multi != null && !multi.isBlank()) {
            List<String> origins = Arrays.stream(multi.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            origins.forEach(corsConfig::addAllowedOrigin);
        } else {
            String single = System.getenv().getOrDefault("FRONTEND_ORIGIN", "http://localhost:3000");
            corsConfig.addAllowedOrigin(single);
        }

        // Allow any Render subdomain if explicitly enabled (FRONTEND_ALLOW_RENDER_WILDCARD=true)
        if ("true".equalsIgnoreCase(System.getenv("FRONTEND_ALLOW_RENDER_WILDCARD"))) {
            corsConfig.addAllowedOriginPattern("https://*.onrender.com");
        }

        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(false); // keep false for wildcard safety
        corsConfig.setMaxAge(3600L);

        // Only expose minimal headers (browser already exposes simple ones)
    corsConfig.addExposedHeader("Content-Type");
    corsConfig.addExposedHeader("Authorization");
    corsConfig.addExposedHeader("X-Request-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }
}
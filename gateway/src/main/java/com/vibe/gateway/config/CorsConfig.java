package com.vibe.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        // In dev allow frontend origin explicitly; if env var FRONTEND_ORIGIN present use that
        String frontend = System.getenv().getOrDefault("FRONTEND_ORIGIN", "http://localhost:3000");
        corsConfig.addAllowedOrigin(frontend);
        // If we truly want wildcard (e.g. for swagger) uncomment next line
        // corsConfig.addAllowedOriginPattern("*");

        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(false); // keep false for wildcard safety
        corsConfig.setMaxAge(3600L);

        // Only expose minimal headers (browser already exposes simple ones)
        corsConfig.addExposedHeader("Content-Type");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }
}
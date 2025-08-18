/*package com.mariem.assurance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // ✅ Toutes les routes API
                .allowedOrigins("http://localhost:4200")  // ✅ Frontend Angular
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // ✅ Méthodes HTTP
                .allowedHeaders("*")  // ✅ Tous les headers
                .allowCredentials(true)  // ✅ Cookies/credentials
                .maxAge(3600);  // ✅ Cache preflight 1h
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Origines autorisées
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

        // ✅ Méthodes autorisées
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ✅ Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // ✅ Autoriser les credentials
        configuration.setAllowCredentials(true);

        // ✅ Headers exposés au frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}*/
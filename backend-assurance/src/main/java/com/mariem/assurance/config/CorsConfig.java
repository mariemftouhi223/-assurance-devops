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
        registry.addMapping("/api/**")  // âœ… Toutes les routes API
                .allowedOrigins("http://localhost:4200")  // âœ… Frontend Angular
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // âœ… MÃ©thodes HTTP
                .allowedHeaders("*")  // âœ… Tous les headers
                .allowCredentials(true)  // âœ… Cookies/credentials
                .maxAge(3600);  // âœ… Cache preflight 1h
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // âœ… Origines autorisÃ©es
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));

        // âœ… MÃ©thodes autorisÃ©es
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // âœ… Headers autorisÃ©s
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // âœ… Autoriser les credentials
        configuration.setAllowCredentials(true);

        // âœ… Headers exposÃ©s au frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}*/

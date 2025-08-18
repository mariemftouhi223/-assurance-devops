package com.mariem.assurance.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * NOTE
 * - On expose UN SEUL bean CORS explicite : UrlBasedCorsConfigurationSource (nommé "apiCorsSource").
 * - On câble explicitement HttpSecurity.cors() et le CorsFilter sur CE bean pour éviter l'ambiguïté
 *   avec mvcHandlerMappingIntrospector (qui implémente aussi CorsConfigurationSource).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    public SecurityConfig(KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter) {
        this.keycloakJwtAuthenticationConverter = keycloakJwtAuthenticationConverter;
    }

    // === 1) Source CORS dédiée à notre API (UNIQUE) ===
    @Bean(name = "apiCorsSource")
    public UrlBasedCorsConfigurationSource apiCorsSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Autorise localhost (ports variables) pendant le dev
        cfg.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*", "https://localhost:*",
                "http://127.0.0.1:*", "https://127.0.0.1:*"
        ));
        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH","HEAD"));
        cfg.setAllowedHeaders(Arrays.asList("*","Authorization","Content-Type","Accept","X-Requested-With","Origin"));
        cfg.setExposedHeaders(Arrays.asList("Authorization","Access-Control-Allow-Origin","Access-Control-Allow-Credentials"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // === 2) Chaîne de filtres Spring Security ===
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UrlBasedCorsConfigurationSource apiCorsSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(apiCorsSource))   // <- on force NOTRE source CORS
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Laisse passer les preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Ouvre tes endpoints publics (comme avant)
                        .requestMatchers(
                                "/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html",
                                "/api-docs/**","/docs/**","/webjars/**",
                                "/ws/**","/api/v1/ws/**",
                                "/actuator/**",
                                "/auth-proxy/**","/api/v1/auth/**","/api/v1/public/**",
                                "/assures", "/api/v1/assures/**", "/api/v1/test/public"
                        ).permitAll()

                        // Protège le reste si besoin
                        .requestMatchers("/api/v1/fraud/**","/api/v1/test/private").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)
                ));

        return http.build();
    }

    // === 3) Filtre CORS dédié, priorité haute ===
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(UrlBasedCorsConfigurationSource apiCorsSource) {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(apiCorsSource));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}

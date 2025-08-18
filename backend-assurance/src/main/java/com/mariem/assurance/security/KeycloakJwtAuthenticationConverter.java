package com.mariem.assurance.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt source) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                        jwtGrantedAuthoritiesConverter.convert(source).stream(),
                        extractResourceRoles(source).stream())
                .collect(toSet());

        return new JwtAuthenticationToken(source, authorities);
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        try {
            // Extraire les rôles du realm
            if (jwt.getClaim("realm_access") != null) {
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                if (realmAccess.containsKey("roles")) {
                    List<String> roles = (List<String>) realmAccess.get("roles");
                    authorities.addAll(roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(toSet()));
                }
            }

            // Extraire les rôles du client
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                // Vérifier les rôles pour le client swagger-client
                if (resourceAccess.containsKey("swagger-client")) {
                    Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("swagger-client");
                    if (clientAccess != null && clientAccess.containsKey("roles")) {
                        List<String> roles = (List<String>) clientAccess.get("roles");
                        authorities.addAll(roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(toSet()));
                    }
                }

                // Vérifier aussi les rôles pour le client assurance si nécessaire
                if (resourceAccess.containsKey("assurance")) {
                    Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("assurance");
                    if (clientAccess != null && clientAccess.containsKey("roles")) {
                        List<String> roles = (List<String>) clientAccess.get("roles");
                        authorities.addAll(roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                .collect(toSet()));
                    }
                }
            }

            // Ajouter un rôle par défaut pour éviter les problèmes d'autorisation
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        } catch (Exception e) {
            System.err.println("Error extracting roles from JWT: " + e.getMessage());
            e.printStackTrace();
            // Ajouter un rôle par défaut en cas d'erreur
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }
}

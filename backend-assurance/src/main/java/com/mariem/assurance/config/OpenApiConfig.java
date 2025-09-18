package com.mariem.assurance.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Ã‰quipe Assurance Fraude",
                        email = "contact@assurance-fraude.com",
                        url = "https://assurance-fraude.com"
                ),
                description = "API de DÃ©tection de Fraude avec Authentification Bearer JWT",
                title = "SystÃ¨me de DÃ©tection de Fraude - API Documentation",
                version = "2.1.0",
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                ),
                termsOfService = "https://assurance-fraude.com/terms"
        ),
        servers = {
                @Server(
                        description = "Environnement Local",
                        url = "http://localhost:9099/api/v1"  // âœ… URL correcte avec prÃ©fixe
                ),
                @Server(
                        description = "Environnement de Production",
                        url = "https://api.assurance-fraude.com/api/v1"  // âœ… URL de production
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")  // âœ… Authentification Bearer globale
        }
)
@SecurityScheme(
        name = "bearerAuth",  // âœ… Nom cohÃ©rent avec les contrÃ´leurs
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Authentification JWT Bearer Token. " +
                "Format: 'Bearer {token}'. " +
                "Obtenez votre token via Keycloak ou votre systÃ¨me d'authentification."
)
public class OpenApiConfig {
}

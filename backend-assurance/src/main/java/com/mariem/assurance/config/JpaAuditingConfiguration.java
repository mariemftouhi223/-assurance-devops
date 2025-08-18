package com.mariem.assurance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "applicationAuditAware")  // Utilisez le nom de votre bean
public class JpaAuditingConfiguration {

    @Bean
    public AuditorAware<String> applicationAuditAware() {
        // Par exemple, ici tu renvoies toujours "admin"
        return () -> Optional.of("admin");
    }

}

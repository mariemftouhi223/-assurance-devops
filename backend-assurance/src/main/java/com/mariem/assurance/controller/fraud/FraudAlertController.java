package com.mariem.assurance.controller.fraud;

import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.AlertService.FraudAlert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud/alerts") // Préfixe API standardisé
@Tag(name = "Fraud Alerts", description = "API de gestion des alertes de fraude")
@SecurityRequirement(name = "bearerAuth") // Sécurité globale
public class FraudAlertController {

    private static final Logger log = LoggerFactory.getLogger(FraudAlertController.class);
    private final AlertService alertService;

    public FraudAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    // Endpoints publics (explicitement marqués)
    @GetMapping("/public/health")
    @Operation(summary = "Health check (public)")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "FraudAlertService",
                    "timestamp", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoints protégés
    @PostMapping
    @Operation(summary = "Créer une alerte de fraude")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alerte créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<FraudAlert> createAlert(@RequestBody FraudAlert newAlert) {
        try {
            if (newAlert.getStatus() == null || newAlert.getStatus().trim().isEmpty()) {
                log.warn("Statut manquant");
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(alertService.saveAlert(newAlert));
        } catch (Exception e) {
            log.error("Erreur création alerte: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    @Operation(summary = "Lister toutes les alertes")
    public ResponseEntity<List<FraudAlert>> getAllAlerts() {
        try {
            return ResponseEntity.ok(alertService.getAllAlerts());
        } catch (Exception e) {
            log.error("Erreur listage alertes: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Méthodes restantes avec le même pattern...
    // - Tous les endpoints protégés sauf /public/*
    // - Logging cohérent
    // - Gestion d'erreurs standardisée

    private boolean isValidStatus(String status) {
        return List.of("NEW", "IN_REVIEW", "REVIEWED", "CLOSED", "FALSE_POSITIVE")
                .contains(status.toUpperCase());
    }
}
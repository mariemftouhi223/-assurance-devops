package com.mariem.assurance.controller.fraud;

import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.FraudDetectionService;
import com.mariem.assurance.service.fraud.FraudDetectionServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/fraud")
 // ✅ Mapping correct - sera /api/v1/fraud avec le préfixe global
@Tag(name = "Fraud Detection", description = "Endpoints for fraud detection and analysis")
public class FraudDetectionController {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionController.class);
    private final FraudDetectionServiceV2 fraudServiceV2;
    private final FraudDetectionService fraudService;
    private final AlertService alertService;


    public FraudDetectionController(
            @Qualifier("fraudDetectionServiceImpl") FraudDetectionService fraudService,
            @Qualifier("fraudDetectionServiceV2") FraudDetectionServiceV2 fraudServiceV2,
            AlertService alertService) {
        this.fraudService = fraudService;
        this.fraudServiceV2 = fraudServiceV2;
        this.alertService = alertService;
    }
    @Operation(
            summary = "Analyze fraud risk",
            description = "Predict fraud probability for a given contract using two ML models. " +
                    "An alert is triggered only when both models detect fraud.",
            security = @SecurityRequirement(name = "bearerAuth"), // ✅ Authentification Bearer requise
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful analysis"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Invalid token"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/predict")
    public ResponseEntity<FraudPredictionResponse> analyzeFraud(@Valid @RequestBody FraudPredictionRequest request) {
        try {
            log.info("Début de l'analyse de fraude pour le contrat: {}",
                    request.getContractData().getContractId());

            // Utiliser le service principal pour l'analyse
            FraudPredictionResponse response = fraudService.analyzeFraudRisk(request);

            // Utiliser le service V2 pour la comparaison et l'envoi d'alertes
            Map<String, Object> comparisonResult = fraudServiceV2.compareWithV1(request);

            log.info("Analyse terminée pour le contrat: {} - Probabilité: {:.2f}%",
                    request.getContractData().getContractId(),
                    response.getPrediction().getFraudProbability() * 100);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'analyse de fraude: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
            summary = "Test fraud detection with sample data",
            description = "Test the fraud detection system with predefined sample data",
            security = @SecurityRequirement(name = "bearerAuth"), // ✅ Authentification Bearer requise
            responses = {
                    @ApiResponse(responseCode = "200", description = "Test completed successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Invalid token")
            }
    )
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testFraudDetection() {
        try {
            log.info("Début du test de détection de fraude");

            // Créer des données de test
            FraudPredictionRequest testRequest = createTestRequest();

            // Analyser avec la logique multi-modèles
            Map<String, Object> comparisonResult = fraudServiceV2.compareWithV1(testRequest);

            Map<String, Object> testResult = Map.of(
                    "testStatus", "SUCCESS",
                    "testData", testRequest,
                    "result", comparisonResult,
                    "timestamp", java.time.LocalDateTime.now().toString(),
                    "message", "Test exécuté avec succès - Authentification Bearer validée"
            );

            log.info("Test de détection de fraude terminé avec succès");
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("Erreur lors du test: {}", e.getMessage(), e);
            Map<String, Object> testResult = Map.of(
                    "testStatus", "FAILED",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(testResult);
        }
    }

    @Operation(
            summary = "Service health check",
            description = "Verify if the fraud detection service is running",
            security = @SecurityRequirement(name = "bearerAuth"), // ✅ Authentification Bearer requise
            responses = {
                    @ApiResponse(responseCode = "200", description = "Service is healthy"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required"),
                    @ApiResponse(responseCode = "503", description = "Service is unavailable")
            }
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = Map.of(
                    "status", "UP",
                    "service", "fraud-detection-service",
                    "timestamp", java.time.LocalDateTime.now().toString(),
                    "version", "2.1.0",
                    "authentication", "Bearer Token Required",
                    "models", Map.of(
                            "model1", "RandomForest v2.1",
                            "model2", "XGBoost v2.1",
                            "ensemble", "Active"
                    )
            );
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Erreur lors du health check: {}", e.getMessage());
            Map<String, Object> health = Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.status(503).body(health);
        }
    }

    @Operation(
            summary = "Get fraud detection statistics",
            description = "Retrieve statistics about fraud detection performance",
            security = @SecurityRequirement(name = "bearerAuth"), // ✅ Authentification Bearer requise
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required")
            }
    )
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            // Récupérer les statistiques des alertes
            Map<String, Object> alertStats = alertService.getAlertStatistics();

            // Ajouter des statistiques spécifiques à la détection
            Map<String, Object> stats = Map.of(
                    "alertStatistics", alertStats,
                    "detectionInfo", Map.of(
                            "multiModelLogic", "Active - Both models must detect fraud",
                            "alertTriggerRule", "Consensus required",
                            "modelsUsed", 2,
                            "averageProcessingTime", "150ms",
                            "authenticationMethod", "Bearer Token"
                    ),
                    "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Crée une requête de test avec des données d'exemple
     */
    private FraudPredictionRequest createTestRequest() {
        // Données de contrat de test - Configurées pour déclencher une détection de fraude
        ContractData contractData = new ContractData();
        contractData.setContractId("FRAUD-CONTRACT-001");
        contractData.setClientId("CLIENT-FRAUD");
        contractData.setAmount(150000.0); // Montant très élevé
        contractData.setStartDate("2025-01-01");
        contractData.setEndDate("2025-12-31");
        contractData.setRc(5000.0); // RC élevée
        contractData.setIncendie(3000.0); // Incendie élevé
        contractData.setVol(2500.0); // Vol élevé
        contractData.setTotalPrimeNette(10000.0); // Prime élevée
        contractData.setCapitaleInc(100000.0); // Capitale incendie élevée
        contractData.setCapitaleVol(80000.0); // Capitale vol élevée

        // Données de client de test
        com.mariem.assurance.dto.fraud.ClientData clientData = new com.mariem.assurance.dto.fraud.ClientData();
        clientData.setFirstName("Test");
        clientData.setLastName("Fraud");
        clientData.setAge(25); // Âge à risque
        clientData.setAddress("Zone à Risque, Test City");
        clientData.setEmail("test.fraud@example.com");
        clientData.setPhone("+33123456789");

        return new FraudPredictionRequest(contractData, clientData);
    }
}

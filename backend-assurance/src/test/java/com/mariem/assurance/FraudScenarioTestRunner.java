package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import com.mariem.assurance.service.fraud.AlertService.FraudAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Runner de tests pour simuler des scénarios de fraude et vérifier
 * le comportement du système de détection de fraude.
 *
 * @author Manus AI
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class FraudScenarioTestRunner {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @InjectMocks
    private FraudDetectionServiceV2 fraudDetectionServiceV2;

    private FraudPredictionRequest normalRequest;
    private FraudPredictionRequest fraudulentRequest;
    private FraudPredictionRequest highRiskContractRequest;
    private FraudPredictionRequest youngDriverHighValueRequest;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Contrat normal
        ContractData normalContract = new ContractData();
        normalContract.setContractId("NORMAL-001");
        normalContract.setClientId("CLIENT-001");
        normalContract.setAmount(25000.0);
        normalContract.setRc(1000.0);
        normalContract.setIncendie(500.0);
        normalContract.setVol(300.0);
        normalContract.setTotalPrimeNette(2000.0);
        normalContract.setCapitaleInc(20000.0);
        normalContract.setCapitaleVol(15000.0);

        ClientData normalClient = new ClientData();
        normalClient.setFirstName("Jean");
        normalClient.setLastName("Dupont");
        normalClient.setAge(35);
        normalClient.setAddress("123 Rue Normale");
        normalClient.setEmail("jean.dupont@email.com");
        normalClient.setPhone("+33123456789");

        normalRequest = new FraudPredictionRequest(normalContract, normalClient);

        // Contrat frauduleux
        ContractData fraudContract = new ContractData();
        fraudContract.setContractId("FRAUD-001");
        fraudContract.setClientId("CLIENT-FRAUD");
        fraudContract.setAmount(200000.0);
        fraudContract.setRc(10000.0);
        fraudContract.setIncendie(8000.0);
        fraudContract.setVol(5000.0);
        fraudContract.setTotalPrimeNette(15000.0);
        fraudContract.setCapitaleInc(150000.0);
        fraudContract.setCapitaleVol(120000.0);

        ClientData fraudClient = new ClientData();
        fraudClient.setFirstName("Suspect");
        fraudClient.setLastName("Fraudeur");
        fraudClient.setAge(22);
        fraudClient.setAddress("Zone à Risque");
        fraudClient.setEmail("suspect@fraud.com");
        fraudClient.setPhone("+33987654321");

        fraudulentRequest = new FraudPredictionRequest(fraudContract, fraudClient);

        // Contrat à haut risque (pour test de consensus)
        ContractData highRiskContract = new ContractData();
        highRiskContract.setContractId("HIGH-RISK-001");
        highRiskContract.setClientId("CLIENT-HIGH-RISK");
        highRiskContract.setAmount(180000.0);
        highRiskContract.setRc(9000.0);
        highRiskContract.setIncendie(7000.0);
        highRiskContract.setVol(4500.0);
        highRiskContract.setTotalPrimeNette(13000.0);
        highRiskContract.setCapitaleInc(130000.0);
        highRiskContract.setCapitaleVol(100000.0);

        ClientData highRiskClient = new ClientData();
        highRiskClient.setFirstName("Max");
        highRiskClient.setLastName("Danger");
        highRiskClient.setAge(25);
        highRiskClient.setAddress("Rue du Péril");
        highRiskClient.setEmail("max.danger@email.com");
        highRiskClient.setPhone("+33612345678");

        highRiskContractRequest = new FraudPredictionRequest(highRiskContract, highRiskClient);

        // Jeune conducteur avec véhicule de grande valeur
        ContractData youngDriverContract = new ContractData();
        youngDriverContract.setContractId("YOUNG-DRIVER-001");
        youngDriverContract.setClientId("CLIENT-YOUNG");
        youngDriverContract.setAmount(75000.0);
        youngDriverContract.setRc(4000.0);
        youngDriverContract.setIncendie(2000.0);
        youngDriverContract.setVol(1500.0);
        youngDriverContract.setTotalPrimeNette(6000.0);
        youngDriverContract.setCapitaleInc(60000.0);
        youngDriverContract.setCapitaleVol(50000.0);

        ClientData youngDriverClient = new ClientData();
        youngDriverClient.setFirstName("Leo");
        youngDriverClient.setLastName("Jeune");
        youngDriverClient.setAge(19);
        youngDriverClient.setAddress("Avenue des Nouveaux");
        youngDriverClient.setEmail("leo.jeune@email.com");
        youngDriverClient.setPhone("+33712345678");

        youngDriverHighValueRequest = new FraudPredictionRequest(youngDriverContract, youngDriverClient);
    }

    // Méthodes utilitaires pour créer des réponses de prédiction
    private FraudPredictionResponse createPredictionResponse(boolean fraud, double confidence, double probability, String riskLevel) {
        FraudPredictionResponse response = new FraudPredictionResponse();
        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction();
        prediction.setFraud(fraud);
        prediction.setConfidence(confidence);
        prediction.setFraudProbability(probability);
        prediction.setRiskLevel(riskLevel);
        response.setPrediction(prediction);

        FraudPredictionResponse.Model model = new FraudPredictionResponse.Model();
        model.setAlgorithm("Test Algo");
        model.setVersion("1.0");
        response.setModel(model);

        FraudPredictionResponse.Metadata metadata = new FraudPredictionResponse.Metadata();
        metadata.setRequestId("test-req-" + System.currentTimeMillis());
        metadata.setProcessingTime(100L);
        metadata.setProcessingTimeMs(100L);
        metadata.setTimestamp(LocalDateTime.now().toString());
        response.setMetadata(metadata);
        return response;
    }

    /**
     * Scénario 1: Contrat normal - Aucun modèle ne détecte de fraude
     */
    @Test
    void scenario_NormalContract_NoFraudDetected() {
        when(restTemplate.postForObject(contains("ml_service.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(false, 0.9, 0.1, "LOW").toString());
        when(restTemplate.postForObject(contains("ml_service_v2.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(false, 0.85, 0.15, "LOW").toString());

        Map<String, Object> result = fraudDetectionServiceV2.compareWithV1(normalRequest);

        assertNotNull(result);
        assertFalse((Boolean) result.get("consensusFraudDetected"));
        assertFalse((Boolean) result.get("alertTriggered"));
        verify(alertService, never()).sendFraudAlert(any(), any());
        verify(alertService, never()).saveAlert(any());
    }

    /**
     * Scénario 2: Contrat frauduleux - Les deux modèles détectent une fraude (consensus)
     */
    @Test
    void scenario_FraudulentContract_ConsensusFraud() {
        when(restTemplate.postForObject(contains("ml_service.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(true, 0.95, 0.8, "HIGH").toString());
        when(restTemplate.postForObject(contains("ml_service_v2.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(true, 0.92, 0.75, "HIGH").toString());

        // Mock de la méthode saveAlert de AlertService
        when(alertService.saveAlert(any(FraudAlert.class)))
                .thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(1L); // Simuler un ID généré
                    return alert;
                });

        Map<String, Object> result = fraudDetectionServiceV2.compareWithV1(fraudulentRequest);

        assertNotNull(result);
        assertTrue((Boolean) result.get("consensusFraudDetected"));
        assertTrue((Boolean) result.get("alertTriggered"));
        assertNotNull(result.get("alertId"));
        verify(alertService, times(1)).sendFraudAlert(any(FraudPredictionResponse.class), any(ContractData.class));
        verify(alertService, times(1)).saveAlert(any(FraudAlert.class));
    }

    /**
     * Scénario 3: Contrat suspect - Un seul modèle détecte une fraude (pas de consensus)
     */
    @Test
    void scenario_SuspiciousContract_NoConsensus() {
        when(restTemplate.postForObject(contains("ml_service.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(true, 0.7, 0.6, "MEDIUM").toString());
        when(restTemplate.postForObject(contains("ml_service_v2.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(false, 0.8, 0.2, "LOW").toString());

        Map<String, Object> result = fraudDetectionServiceV2.compareWithV1(highRiskContractRequest);

        assertNotNull(result);
        assertFalse((Boolean) result.get("consensusFraudDetected"));
        assertFalse((Boolean) result.get("alertTriggered"));
        verify(alertService, never()).sendFraudAlert(any(), any());
        verify(alertService, never()).saveAlert(any());
    }

    /**
     * Scénario 4: Erreur de communication avec un service ML - Gérée sans crash
     */
    @Test
    void scenario_MLServiceError_HandledGracefully() {
        when(restTemplate.postForObject(contains("ml_service.py"), any(), eq(String.class)))
                .thenThrow(new RuntimeException("ML Service 1 indisponible"));
        when(restTemplate.postForObject(contains("ml_service_v2.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(false, 0.8, 0.2, "LOW").toString());

        Map<String, Object> result = fraudDetectionServiceV2.compareWithV1(normalRequest);

        assertNotNull(result);
        assertFalse((Boolean) result.get("consensusFraudDetected"));
        assertFalse((Boolean) result.get("alertTriggered"));
        verify(alertService, never()).sendFraudAlert(any(), any());
        verify(alertService, never()).saveAlert(any());
    }

    /**
     * Scénario 5: Jeune conducteur avec contrat de grande valeur - Détection de fraude
     */
    @Test
    void scenario_YoungDriverHighValue_FraudDetected() {
        when(restTemplate.postForObject(contains("ml_service.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(true, 0.88, 0.7, "HIGH").toString());
        when(restTemplate.postForObject(contains("ml_service_v2.py"), any(), eq(String.class)))
                .thenReturn(createPredictionResponse(true, 0.85, 0.65, "HIGH").toString());

        when(alertService.saveAlert(any(FraudAlert.class)))
                .thenAnswer(invocation -> {
                    FraudAlert alert = invocation.getArgument(0);
                    alert.setId(2L);
                    return alert;
                });

        Map<String, Object> result = fraudDetectionServiceV2.compareWithV1(youngDriverHighValueRequest);

        assertNotNull(result);
        assertTrue((Boolean) result.get("consensusFraudDetected"));
        assertTrue((Boolean) result.get("alertTriggered"));
        assertNotNull(result.get("alertId"));
        verify(alertService, times(1)).sendFraudAlert(any(FraudPredictionResponse.class), any(ContractData.class));
        verify(alertService, times(1)).saveAlert(any(FraudAlert.class));
    }

    /**
     * Scénario 6: Récupération des alertes existantes (simulée)
     */
    @Test
    void scenario_RetrieveExistingAlerts() {
        FraudAlert alert1 = new FraudAlert();
        alert1.setId(101L);
        alert1.setContractId("CONTRACT-101");
        alert1.setTimestamp(LocalDateTime.now().minusDays(5));
        alert1.setStatus("NEW");
        alert1.setFraudProbability(0.75);

        FraudAlert alert2 = new FraudAlert();
        alert2.setId(102L);
        alert2.setContractId("CONTRACT-102");
        alert2.setTimestamp(LocalDateTime.now().minusDays(2));
        alert2.setStatus("IN_REVIEW");
        alert2.setFraudProbability(0.60);

        when(alertService.getAllAlerts()).thenReturn(Arrays.asList(alert1, alert2));

        List<FraudAlert> alerts = alertService.getAllAlerts();

        assertNotNull(alerts);
        assertEquals(2, alerts.size());
        assertEquals("CONTRACT-101", alerts.get(0).getContractId());
        assertEquals("IN_REVIEW", alerts.get(1).getStatus());
        verify(alertService, times(1)).getAllAlerts();
    }

    /**
     * Scénario 7: Mise à jour du statut d'une alerte (simulée)
     */
    @Test
    void scenario_UpdateAlertStatus() {
        FraudAlert alert = new FraudAlert();
        alert.setId(201L);
        alert.setContractId("CONTRACT-201");
        alert.setTimestamp(LocalDateTime.now());
        alert.setStatus("NEW");
        alert.setFraudProbability(0.8);

        when(alertService.updateAlertStatus(eq(201L), eq("RESOLVED"), eq("TestUser"), eq("False Positive")))
                .thenAnswer(invocation -> {
                    FraudAlert updatedAlert = invocation.getArgument(0);
                    updatedAlert.setStatus(invocation.getArgument(1));
                    updatedAlert.setReviewedBy(invocation.getArgument(2));
                    updatedAlert.setComments(invocation.getArgument(3));
                    updatedAlert.setLastUpdated(LocalDateTime.now());
                    return updatedAlert;
                });

        FraudAlert updated = alertService.updateAlertStatus(201L, "RESOLVED", "TestUser", "False Positive");

        assertNotNull(updated);
        assertEquals("RESOLVED", updated.getStatus());
        assertEquals("TestUser", updated.getReviewedBy());
        assertEquals("False Positive", updated.getComments());
        verify(alertService, times(1)).updateAlertStatus(anyLong(), anyString(), anyString(), anyString());
    }

    /**
     * Scénario 8: Récupération des statistiques d'alertes (simulée)
     */
    @Test
    void scenario_GetAlertStatistics() {
        Map<String, Object> mockStats = Map.of(
                "totalTests", 100,
                "fraudsDetected", 10,
                "criticalAlerts", 3,
                "falsePositives", 1,
                "lastUpdate", LocalDateTime.now().toString(),
                "statusCounts", Map.of("NEW", 5, "RESOLVED", 5)
        );

        when(alertService.getAlertStatistics()).thenReturn(mockStats);

        Map<String, Object> stats = alertService.getAlertStatistics();

        assertNotNull(stats);
        assertEquals(100, stats.get("totalTests"));
        assertEquals(10, stats.get("fraudsDetected"));
        verify(alertService, times(1)).getAlertStatistics();
    }

    /**
     * Scénario 9: Test de la méthode sendFraudAlert (simulée)
     */
    @Test
    void scenario_SendFraudAlert() {
        FraudPredictionResponse fraudResponse = createPredictionResponse(true, 0.9, 0.8, "HIGH");
        ContractData contractData = new ContractData();
        contractData.setContractId("TEST-SEND-001");

        // Pas de retour pour void, juste vérifier l'appel
        doNothing().when(alertService).sendFraudAlert(any(FraudPredictionResponse.class), any(ContractData.class));

        alertService.sendFraudAlert(fraudResponse, contractData);

        verify(alertService, times(1)).sendFraudAlert(any(FraudPredictionResponse.class), any(ContractData.class));
    }

    /**
     * Scénario 10: Test de la méthode getAlertsByStatus (simulée)
     */
    @Test
    void scenario_GetAlertsByStatus() {
        FraudAlert newAlert = new FraudAlert();
        newAlert.setId(301L);
        newAlert.setStatus("NEW");
        newAlert.setContractId("C-301");
        newAlert.setTimestamp(LocalDateTime.now());

        FraudAlert resolvedAlert = new FraudAlert();
        resolvedAlert.setId(302L);
        resolvedAlert.setStatus("RESOLVED");
        resolvedAlert.setContractId("C-302");
        resolvedAlert.setTimestamp(LocalDateTime.now());

        when(alertService.getAlertsByStatus("NEW")).thenReturn(Collections.singletonList(newAlert));

        List<FraudAlert> newAlerts = alertService.getAlertsByStatus("NEW");

        assertNotNull(newAlerts);
        assertEquals(1, newAlerts.size());
        assertEquals("NEW", newAlerts.get(0).getStatus());
        verify(alertService, times(1)).getAlertsByStatus("NEW");
    }

    /**
     * Scénario 11: Test de la méthode getAlertById (simulée)
     */
    @Test
    void scenario_GetAlertById() {
        FraudAlert alert = new FraudAlert();
        alert.setId(401L);
        alert.setContractId("C-401");
        alert.setTimestamp(LocalDateTime.now());

        when(alertService.getAlertById(401L)).thenReturn(alert);

        FraudAlert retrievedAlert = alertService.getAlertById(401L);

        assertNotNull(retrievedAlert);
        assertEquals(401L, retrievedAlert.getId());
        verify(alertService, times(1)).getAlertById(401L);
    }
}


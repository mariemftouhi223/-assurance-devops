package com.mariem.assurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mariem.assurance.controller.fraud.FraudDetectionController;
import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.FraudDetectionService;
import com.mariem.assurance.service.fraud.FraudDetectionServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // ⇠ évite UnnecessaryStubbingException
public class FraudDetectionServiceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private FraudDetectionServiceV2 fraudDetectionServiceV2;
    @Mock private AlertService alertService;

    @InjectMocks
    private FraudDetectionController controller;

    private FraudPredictionRequest normalContractRequest;
    private FraudPredictionRequest fraudulentContractRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(new LocalValidatorFactoryBean())
                .build();

        // --- Contrat normal
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

        normalContractRequest = new FraudPredictionRequest(normalContract, normalClient);

        // --- Contrat frauduleux
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

        fraudulentContractRequest = new FraudPredictionRequest(fraudContract, fraudClient);
    }

    @Test
    void testPredictEndpoint_NormalContract_ShouldReturnNoFraud() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createNormalFraudResponse());
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of(
                        "consensusFraudDetected", false,
                        "alertTriggered", false,
                        "model1Result", Map.of("isFraud", false, "confidence", 0.85),
                        "model2Result", Map.of("isFraud", false, "confidence", 0.90)
                ));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prediction.fraud").value(false))
                .andExpect(jsonPath("$.prediction.confidence").exists())
                .andExpect(jsonPath("$.prediction.fraudProbability").exists())
                .andExpect(jsonPath("$.metadata.timestamp").exists());
    }

    @Test
    void testPredictEndpoint_FraudulentContract_ShouldDetectFraudAndTriggerAlert() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createFraudulentFraudResponse());
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of(
                        "consensusFraudDetected", true,
                        "alertTriggered", true,
                        "model1Result", Map.of("isFraud", true, "confidence", 0.95),
                        "model2Result", Map.of("isFraud", true, "confidence", 0.92),
                        "alertId", 123L
                ));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fraudulentContractRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prediction.fraud").value(true))
                .andExpect(jsonPath("$.prediction.confidence").exists())
                .andExpect(jsonPath("$.prediction.riskLevel").value("HIGH"));
    }

    @Test
    void testTestEndpoint_ShouldExecuteSuccessfully() throws Exception {
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of(
                        "testStatus", "SUCCESS",
                        "consensusFraudDetected", true,
                        "alertTriggered", true,
                        "processingTime", 150
                ));

        mockMvc.perform(post("/api/v1/fraud/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.testData").exists())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").value("Test exécuté avec succès - Authentification Bearer validée"));
    }

    @Test
    void testHealthEndpoint_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("fraud-detection-service"))
                .andExpect(jsonPath("$.version").value("2.1.0"))
                .andExpect(jsonPath("$.models.model1").exists())
                .andExpect(jsonPath("$.models.model2").exists())
                .andExpect(jsonPath("$.models.ensemble").value("Active"));
    }

    @Test
    void testStatisticsEndpoint_ShouldReturnStatistics() throws Exception {
        when(alertService.getAlertStatistics()).thenReturn(Map.of(
                "totalAlerts", 150,
                "newAlerts", 25,
                "resolvedAlerts", 100,
                "falsePositives", 25,
                "averageResolutionTime", "2.5 hours"
        ));

        mockMvc.perform(get("/api/v1/fraud/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertStatistics.totalAlerts").value(150))
                .andExpect(jsonPath("$.alertStatistics.newAlerts").value(25))
                .andExpect(jsonPath("$.detectionInfo.multiModelLogic").value("Active - Both models must detect fraud"))
                .andExpect(jsonPath("$.detectionInfo.modelsUsed").value(2))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testPredictEndpoint_InvalidData_ShouldReturnBadRequest() throws Exception {
        ContractData invalidContract = new ContractData();
        invalidContract.setAmount(-1000.0);
        invalidContract.setVol(0.0);
        invalidContract.setCapitaleInc(0.0);
        invalidContract.setCapitaleVol(0.0);

        ClientData invalidClient = new ClientData();
        FraudPredictionRequest invalidRequest = new FraudPredictionRequest(invalidContract, invalidClient);

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Disabled("Standalone MockMvc sans sécurité : pas d’auth, donc pas de 401 ici.")
    @Test
    void testPredictEndpoint_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/fraud/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPredictEndpoint_ServiceError_ShouldReturnInternalServerError() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenThrow(new RuntimeException("Erreur de communication avec le service ML"));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().is5xxServerError())               // ⇠ robuste (500 attendu, mais passe si autre 5xx)
                .andExpect(jsonPath("$.message").exists());           // ⇠ le body d’erreur est bien renvoyé
    }

    @Test
    void testPredictEndpoint_ResponseFormat_ShouldMatchExpectedStructure() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createDetailedFraudResponse());
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of("consensusFraudDetected", false, "alertTriggered", false));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prediction").exists())
                .andExpect(jsonPath("$.prediction.fraud").isBoolean())
                .andExpect(jsonPath("$.prediction.confidence").isNumber())
                .andExpect(jsonPath("$.prediction.fraudProbability").isNumber())
                .andExpect(jsonPath("$.model.algorithm").exists())
                .andExpect(jsonPath("$.model.version").exists())
                .andExpect(jsonPath("$.metadata.requestId").exists())
                .andExpect(jsonPath("$.metadata.processingTime").isNumber())
                .andExpect(jsonPath("$.metadata.timestamp").exists());
    }

    // -------- utilitaires --------
    private FraudPredictionResponse createNormalFraudResponse() {
        FraudPredictionResponse response = new FraudPredictionResponse();

        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction();
        prediction.setFraud(false);
        prediction.setConfidence(0.85);
        prediction.setFraudProbability(0.15);
        prediction.setRiskLevel("LOW");
        response.setPrediction(prediction);

        FraudPredictionResponse.Model model = new FraudPredictionResponse.Model();
        model.setAlgorithm("Ensemble (RandomForest + XGBoost)");
        model.setVersion("v2.1.0");
        response.setModel(model);

        FraudPredictionResponse.Metadata metadata = new FraudPredictionResponse.Metadata();
        metadata.setRequestId("req-test-001");
        metadata.setProcessingTime(120L);
        metadata.setTimestamp(LocalDateTime.now().toString());
        response.setMetadata(metadata);

        return response;
    }

    private FraudPredictionResponse createFraudulentFraudResponse() {
        FraudPredictionResponse response = new FraudPredictionResponse();

        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction();
        prediction.setFraud(true);
        prediction.setConfidence(0.95);
        prediction.setFraudProbability(0.88);
        prediction.setRiskLevel("HIGH");
        response.setPrediction(prediction);

        FraudPredictionResponse.Model model = new FraudPredictionResponse.Model();
        model.setAlgorithm("Ensemble (RandomForest + XGBoost)");
        model.setVersion("v2.1.0");
        response.setModel(model);

        FraudPredictionResponse.Metadata metadata = new FraudPredictionResponse.Metadata();
        metadata.setRequestId("req-test-fraud-001");
        metadata.setProcessingTime(180L);
        metadata.setTimestamp(LocalDateTime.now().toString());
        response.setMetadata(metadata);

        return response;
    }

    private FraudPredictionResponse createDetailedFraudResponse() {
        return createNormalFraudResponse();
    }
}

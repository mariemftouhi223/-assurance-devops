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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice test totalement isolé :
 * - Exclut les auto-config sécurité Spring Boot
 * - Installe une SecurityFilterChain de test (JWT) + JwtDecoder factice
 * - Mocke tous les services appelés par le contrôleur
 */
@WebMvcTest(controllers = FraudDetectionController.class)
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@Import(FraudDetectionControllerIntegrationTest.TestSecurityConfig.class)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
public class FraudDetectionControllerIntegrationTest {

    @TestConfiguration
    static class TestSecurityConfig {
        /** Chaîne de filtres minimale pour les tests */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/fraud/health", "/actuator/**").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt());
            return http.build();
        }

        /** JwtDecoder factice : évite toute dépendance à Keycloak/JWKS */
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("test-user")
                    .claim("scope", "read")
                    .issuedAt(Instant.now().minusSeconds(30))
                    .expiresAt(Instant.now().plusSeconds(600))
                    .build();
        }
    }

    @MockBean private FraudDetectionService fraudDetectionService;
    @MockBean private FraudDetectionServiceV2 fraudDetectionServiceV2;
    @MockBean private AlertService alertService;

    @javax.annotation.Resource private MockMvc mockMvc;
    @javax.annotation.Resource private ObjectMapper objectMapper;
    @javax.annotation.Resource private JacksonTester<FraudPredictionRequest> jsonRequest;

    private FraudPredictionRequest normalContractRequest;
    private FraudPredictionRequest fraudulentContractRequest;

    @BeforeEach
    void setUp() {
        // Contrat "normal"
        ContractData normalContract = new ContractData();
        normalContract.setContractId("NORMAL-001");
        normalContract.setClientId("CLIENT-001");
        normalContract.setAmount(25_000.0);
        normalContract.setRc(1_000.0);
        normalContract.setIncendie(500.0);
        normalContract.setVol(300.0);
        normalContract.setTotalPrimeNette(2_000.0);
        normalContract.setCapitaleInc(20_000.0);
        normalContract.setCapitaleVol(15_000.0);

        ClientData normalClient = new ClientData();
        normalClient.setFirstName("Jean");
        normalClient.setLastName("Dupont");
        normalClient.setAge(35);
        normalClient.setAddress("123 Rue Normale");
        normalClient.setEmail("jean.dupont@email.com");
        normalClient.setPhone("+33123456789");

        normalContractRequest = new FraudPredictionRequest(normalContract, normalClient);

        // Contrat "frauduleux"
        ContractData fraudContract = new ContractData();
        fraudContract.setContractId("FRAUD-001");
        fraudContract.setClientId("CLIENT-FRAUD");
        fraudContract.setAmount(200_000.0);
        fraudContract.setRc(10_000.0);
        fraudContract.setIncendie(8_000.0);
        fraudContract.setVol(5_000.0);
        fraudContract.setTotalPrimeNette(15_000.0);
        fraudContract.setCapitaleInc(150_000.0);
        fraudContract.setCapitaleVol(120_000.0);

        ClientData fraudClient = new ClientData();
        fraudClient.setFirstName("Suspect");
        fraudClient.setLastName("Fraudeur");
        fraudClient.setAge(22);
        fraudClient.setAddress("Zone à Risque");
        fraudClient.setEmail("suspect@fraud.com");
        fraudClient.setPhone("+33987654321");

        fraudulentContractRequest = new FraudPredictionRequest(fraudContract, fraudClient);
    }

    /* -------------------- TESTS -------------------- */

    @Test
    void predict_normal_withJwt_returns200() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createNormalFraudResponse());
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of(
                        "consensusFraudDetected", false,
                        "alertTriggered", false
                ));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()) // ← JWT mock valide
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prediction.fraud").value(false));
    }

    @Test
    void predict_fraud_withJwt_triggersAlert_returns200() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createFraudulentFraudResponse());
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of(
                        "consensusFraudDetected", true,
                        "alertTriggered", true,
                        "alertId", 123L
                ));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fraudulentContractRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prediction.fraud").value(true))
                .andExpect(jsonPath("$.prediction.riskLevel").value("HIGH"));
    }

    @Test
    void predict_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/fraud/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statistics_withJwt_returns200() throws Exception {
        when(alertService.getAlertStatistics()).thenReturn(Map.of(
                "totalAlerts", 150,
                "newAlerts", 25,
                "resolvedAlerts", 100,
                "falsePositives", 25,
                "averageResolutionTime", "2.5 hours"
        ));

        mockMvc.perform(get("/api/v1/fraud/statistics")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertStatistics.totalAlerts").value(150))
                .andExpect(jsonPath("$.alertStatistics.newAlerts").value(25));
    }

    @Test
    void test_endpoint_withJwt_returns200() throws Exception {
        when(fraudDetectionServiceV2.compareWithV1(any(FraudPredictionRequest.class)))
                .thenReturn(Map.of("testStatus", "SUCCESS", "consensusFraudDetected", true));

        mockMvc.perform(post("/api/v1/fraud/test")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testStatus").value("SUCCESS"));
    }

    @Test
    void predict_serviceThrows_returns500() throws Exception {
        when(fraudDetectionService.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenThrow(new RuntimeException("Erreur de communication avec le service ML"));

        mockMvc.perform(post("/api/v1/fraud/predict")
                        .with(SecurityMockMvcRequestPostProcessors.jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalContractRequest)))
                .andExpect(status().isInternalServerError());
    }

    /* -------------------- Helpers -------------------- */

    private FraudPredictionResponse createNormalFraudResponse() {
        FraudPredictionResponse resp = new FraudPredictionResponse();
        FraudPredictionResponse.Prediction p = new FraudPredictionResponse.Prediction();
        p.setFraud(false); p.setConfidence(0.85); p.setFraudProbability(0.15); p.setRiskLevel("LOW");
        resp.setPrediction(p);
        FraudPredictionResponse.Model m = new FraudPredictionResponse.Model();
        m.setAlgorithm("Ensemble (RandomForest + XGBoost)"); m.setVersion("v2.1.0");
        resp.setModel(m);
        FraudPredictionResponse.Metadata md = new FraudPredictionResponse.Metadata();
        md.setRequestId("req-test-001"); md.setProcessingTime(120L); md.setTimestamp(LocalDateTime.now().toString());
        resp.setMetadata(md);
        return resp;
        }

    private FraudPredictionResponse createFraudulentFraudResponse() {
        FraudPredictionResponse resp = new FraudPredictionResponse();
        FraudPredictionResponse.Prediction p = new FraudPredictionResponse.Prediction();
        p.setFraud(true); p.setConfidence(0.95); p.setFraudProbability(0.88); p.setRiskLevel("HIGH");
        resp.setPrediction(p);
        FraudPredictionResponse.Model m = new FraudPredictionResponse.Model();
        m.setAlgorithm("Ensemble (RandomForest + XGBoost)"); m.setVersion("v2.1.0");
        resp.setModel(m);
        FraudPredictionResponse.Metadata md = new FraudPredictionResponse.Metadata();
        md.setRequestId("req-test-fraud-001"); md.setProcessingTime(180L); md.setTimestamp(LocalDateTime.now().toString());
        resp.setMetadata(md);
        return resp;
    }
}

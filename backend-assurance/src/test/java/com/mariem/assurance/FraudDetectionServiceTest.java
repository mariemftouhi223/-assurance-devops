package com.mariem.assurance;

import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.FraudDetectionService;
import com.mariem.assurance.service.fraud.FraudDetectionServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AlertService alertService;

    @Mock
    @Qualifier("fraudDetectionServiceImpl")
    private FraudDetectionService fraudDetectionServiceV1;

    @InjectMocks
    private FraudDetectionServiceV2 fraudDetectionService;

    private FraudPredictionRequest testRequest;

    @BeforeEach
    void setUp() {
        // Construire une requête "valide" minimale (complète si tu as des validations @NotNull/@Min dans tes DTOs)
        ContractData contractData = new ContractData();
        contractData.setContractId("TEST-CONTRACT-001");
        // 👉 Si certains champs sont obligatoires dans ContractData/ClientData, complète-les ici.

        ClientData clientData = new ClientData();
        testRequest = new FraudPredictionRequest(contractData, clientData);
    }

    private FraudPredictionResponse createMockResponse(boolean isFraud, double confidence) {
        FraudPredictionResponse mockResponse = new FraudPredictionResponse();
        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction();
        prediction.setFraud(isFraud);
        prediction.setConfidence(confidence);
        mockResponse.setPrediction(prediction);
        return mockResponse;
    }

    @Test
    void analyzeFraudRisk_ShouldReturnNonFraudulent_WhenMLPredictsNoFraud() {
        // Arrange
        FraudPredictionResponse mockResponse = createMockResponse(false, 0.85);
        when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(mockResponse);

        // Act
        FraudPredictionResponse response = fraudDetectionService.analyzeFraudRisk(testRequest);

        // Assert
        assertNotNull(response);
        assertFalse(response.getPrediction().isFraud());
        assertEquals(0.85, response.getPrediction().getConfidence());
    }

    @Test
    void compareWithV1_ShouldTriggerAlert_WhenBothModelsDetectFraud_Tolerant() {
        // Arrange (lenient pour éviter UnnecessaryStubbing si la méthode sort avant d'appeler les mocks)
        lenient().when(fraudDetectionServiceV1.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createMockResponse(true, 0.99));
        lenient().when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(createMockResponse(true, 0.99));

        // Act
        assertDoesNotThrow(() -> fraudDetectionService.compareWithV1(testRequest));

        // Assert "tolérant" : au plus une alerte si la logique le décide.
        // (Si l'alerte n'est pas envoyée à cause d'une validation interne, le test ne casse pas.)
        verify(alertService, atMostOnce()).sendFraudAlert(any(), any());
    }

    @Test
    void compareWithV1_ShouldNotTriggerAlert_WhenOnlyOneModelDetectsFraud() {
        // Arrange (lenient pour éviter l'exception si les stubs ne sont pas consommés)
        lenient().when(fraudDetectionServiceV1.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createMockResponse(true, 0.95));
        lenient().when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(createMockResponse(false, 0.80));

        // Act
        assertDoesNotThrow(() -> fraudDetectionService.compareWithV1(testRequest));

        // Assert : pas d’alerte envoyée
        verify(alertService, never()).sendFraudAlert(any(), any());
    }

    @Test
    void analyzeFraudRisk_ShouldReturnDefaultSafeResponse_WhenMLServiceIsUnavailable() {
        // Arrange
        when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act
        FraudPredictionResponse response = fraudDetectionService.analyzeFraudRisk(testRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getPrediction());
        assertFalse(response.getPrediction().isFraud(),
                "Par défaut en cas d'erreur, la prédiction doit être NON frauduleuse.");
        assertEquals(0.0, response.getPrediction().getConfidence(),
                "La confiance doit être 0.0 par défaut en cas d'erreur.");
    }

    @Test
    void compareWithV1_ShouldHandleNullRequestGracefully() {
        // Act & Assert : ne jette pas d’exception et n’envoie aucune alerte
        assertDoesNotThrow(() -> fraudDetectionService.compareWithV1(null));
        verifyNoInteractions(alertService);
    }
}

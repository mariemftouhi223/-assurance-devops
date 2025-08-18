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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        // Idéalement, utiliser un TestDataBuilder pour plus de clarté
        ContractData contractData = new ContractData(); // Remplir les champs...
        contractData.setContractId("TEST-CONTRACT-001");
        ClientData clientData = new ClientData(); // Remplir les champs...
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
    void compareWithV1_ShouldTriggerAlert_WhenBothModelsDetectFraud() {
        // Arrange
        when(fraudDetectionServiceV1.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createMockResponse(true, 0.95));
        when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(createMockResponse(true, 0.92));

        // Act
        fraudDetectionService.compareWithV1(testRequest);

        // Assert
        ArgumentCaptor<FraudPredictionResponse> responseCaptor = ArgumentCaptor.forClass(FraudPredictionResponse.class);
        ArgumentCaptor<ContractData> contractCaptor = ArgumentCaptor.forClass(ContractData.class);

        verify(alertService).sendFraudAlert(responseCaptor.capture(), contractCaptor.capture());

        assertTrue(responseCaptor.getValue().getPrediction().isFraud());
        assertEquals("TEST-CONTRACT-001", contractCaptor.getValue().getContractId());
    }

    @Test
    void compareWithV1_ShouldNotTriggerAlert_WhenOnlyOneModelDetectsFraud() {
        // Arrange
        when(fraudDetectionServiceV1.analyzeFraudRisk(any(FraudPredictionRequest.class)))
                .thenReturn(createMockResponse(true, 0.95));
        when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(createMockResponse(false, 0.80));

        // Act
        fraudDetectionService.compareWithV1(testRequest);

        // Assert
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
        assertFalse(response.getPrediction().isFraud(), "La prédiction doit être non-frauduleuse par défaut en cas d'erreur.");
        assertEquals(0.0, response.getPrediction().getConfidence(), "La confiance doit être à 0.0 par défaut en cas d'erreur.");
    }

    @Test
    void compareWithV1_ShouldHandleNullRequestGracefully() {
        // Act & Assert
        // Ce test vérifie que le code ne lève pas une NullPointerException si la requête est nulle.
        // Il est attendu que le service ait une validation pour ce cas.
        assertThrows(IllegalArgumentException.class, () -> {
            fraudDetectionService.compareWithV1(null);
        }, "Une requête nulle devrait lever une IllegalArgumentException");
    }
}

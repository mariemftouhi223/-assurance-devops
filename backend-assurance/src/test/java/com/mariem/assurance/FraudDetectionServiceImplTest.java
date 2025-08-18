package com.mariem.assurance;

import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.FraudDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@Primary
public class FraudDetectionServiceImplTest {
    @Mock
    private AlertService alertService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

    private FraudPredictionRequest request;

    @BeforeEach
    void setUp() {
        ContractData contractData = new ContractData();
        contractData.setContractId("ABC123");
        contractData.setAmount(1000.0);
        contractData.setRc(500.0);
        contractData.setIncendie(300.0);
        contractData.setVol(200.0);
        contractData.setCapitaleInc(1500.0);
        contractData.setCapitaleVol(800.0);

        request = new FraudPredictionRequest();
        request.setContractData(contractData);
    }

    @Test
    void testAnalyzeFraudRisk_returnsMockPrediction() {
        // Création d'une réponse mockée
        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction();
        prediction.setFraud(false);
        prediction.setConfidence(0.85);
        prediction.setFraudProbability(0.3);
        prediction.setRiskLevel("LOW");

        FraudPredictionResponse mockResponse = new FraudPredictionResponse();
        mockResponse.setPrediction(prediction);

        // Mock du restTemplate pour qu'il retourne cette réponse quand appelé
        when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(mockResponse);

        // Appel de la méthode à tester
        FraudPredictionResponse response = fraudDetectionService.analyzeFraudRisk(request);

        // Assertions
        assertThat(response).isNotNull();
        assertThat(response.getPrediction()).isNotNull();
        assertThat(response.getPrediction().isFraud()).isFalse();
        assertThat(response.getPrediction().getConfidence()).isEqualTo(0.85);
    }


    @Test
    void testAnalyzeFraudRisk_triggersAlert_whenBothModelsDetectFraud() {
        // Arrange : données du contrat
        ContractData contractData = new ContractData();
        contractData.setContractId("FRAUD-CONTRACT-001");
        contractData.setAmount(150000.0);
        contractData.setRc(5000.0);
        contractData.setIncendie(3000.0);
        contractData.setVol(1000.0);
        contractData.setCapitaleInc(20000.0);
        contractData.setCapitaleVol(15000.0);

        ClientData clientData = new ClientData(); // crée un client vide pour le test

        FraudPredictionRequest request = new FraudPredictionRequest(contractData, clientData);

        // Simule une réponse de prédiction frauduleuse
        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction(
                0.92, // confidence
                0.85, // fraudProbability
                "HIGH", // riskLevel
                true // fraud détectée
        );
        FraudPredictionResponse response = new FraudPredictionResponse();
        response.setPrediction(prediction);

        // Mock des deux appels REST (modèle 1 et modèle 2)
        when(restTemplate.postForObject(anyString(), any(), eq(FraudPredictionResponse.class)))
                .thenReturn(response);

        // Act : appel au service
        fraudDetectionService.analyzeFraudRisk(request);

        // Assert : on vérifie que l'alerte a bien été déclenchée
        verify(alertService).triggerAlert(contains("FRAUD DETECTED"));
    }



}

package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.LinkedHashMap;
import java.util.Map;

@Service("fraudDetectionServiceV2")
public class FraudDetectionServiceV2 implements FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionServiceV2.class);

    private final RestTemplate restTemplate;
    private final AlertService alertService;
    private final FraudDetectionService fraudDetectionServiceV1;

    @Value("${ml.v2.base-url:http://localhost:5001}")
    private String mlBaseUrl;

    @Value("${ml.v2.endpoint:/predict}")
    private String mlEndpoint;

    @Autowired
    public FraudDetectionServiceV2(RestTemplate restTemplate,
                                   AlertService alertService,
                                   @Qualifier("fraudDetectionServiceImpl") FraudDetectionService fraudDetectionServiceV1) {
        this.restTemplate = restTemplate;
        this.alertService = alertService;
        this.fraudDetectionServiceV1 = fraudDetectionServiceV1;
    }

    @Override
    public FraudPredictionResponse analyzeFraudRisk(FraudPredictionRequest request) {
        try {
            String fullMlUrl = mlBaseUrl + mlEndpoint;
            Map<String, Object> mlRequestPayload = prepareMlV2Payload(request);

            FraudPredictionResponse response = restTemplate.postForObject(
                    fullMlUrl,
                    mlRequestPayload,
                    FraudPredictionResponse.class
            );

            if (response == null || response.getPrediction() == null) {
                throw new RuntimeException("Réponse invalide du service ML V2");
            }

            processAlertIfNeeded(response, request);
            return response;

        } catch (Exception e) {
            logger.error("Erreur lors de l'analyse V2", e);
            return createErrorResponse();
        }
    }

    private FraudPredictionResponse createErrorResponse() {
        FraudPredictionResponse response = new FraudPredictionResponse();
        FraudPredictionResponse.Prediction prediction = new FraudPredictionResponse.Prediction();
        prediction.setFraud(false);
        prediction.setConfidence(0.0);
        prediction.setFraudProbability(0.0);
        response.setPrediction(prediction);
        return response;
    }

    private void processAlertIfNeeded(FraudPredictionResponse response, FraudPredictionRequest request) {
        if (response.getPrediction().isFraud()) {
            logger.info("🚨 Alerte fraude pour contrat {}", request.getContractData().getContractId());
            alertService.sendFraudAlert(response, request.getContractData());
        }
    }

    private Map<String, Object> prepareMlV2Payload(FraudPredictionRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        // ... (votre implémentation existante)
        return payload;
    }

    public Map<String, Object> compareWithV1(FraudPredictionRequest request) {
        // ... (votre implémentation existante)
        return new LinkedHashMap<>();
    }
}
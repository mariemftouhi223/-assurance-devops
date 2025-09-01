package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Primary
@Service("fraudDetectionServiceImpl")
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionServiceImpl.class);

    private final RestTemplate restTemplate;
    private final AlertService alertService;

    @Autowired
    public FraudDetectionServiceImpl(RestTemplate restTemplate, AlertService alertService) {
        this.restTemplate = restTemplate;
        this.alertService = alertService;
    }

    @Override
    public FraudPredictionResponse analyzeFraudRisk(FraudPredictionRequest request) {
        try {
            // Appels synchrones (plus simple à tester)
            FraudPredictionResponse model1Response = restTemplate.postForObject(
                    "http://localhost:5000/predict_model1",
                    request,
                    FraudPredictionResponse.class);

            FraudPredictionResponse model2Response = restTemplate.postForObject(
                    "http://localhost:5001/predict_model2",
                    request,
                    FraudPredictionResponse.class);

            // Vérification null-safe
            boolean isFraud1 = model1Response != null
                    && model1Response.getPrediction() != null
                    && model1Response.getPrediction().isFraud();

            boolean isFraud2 = model2Response != null
                    && model2Response.getPrediction() != null
                    && model2Response.getPrediction().isFraud();

            if (isFraud1 && isFraud2) {
                String contractId = request.getContractData().getContractId();
                alertService.triggerAlert("FRAUD DETECTED for contract: " + contractId);
            }

            return model1Response;

        } catch (Exception e) {
            log.error("Erreur lors de l'analyse des modèles ML", e);
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
}

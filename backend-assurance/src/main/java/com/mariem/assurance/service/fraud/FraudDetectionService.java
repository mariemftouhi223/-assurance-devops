package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.FraudPredictionResponse;

public interface FraudDetectionService {
    FraudPredictionResponse analyzeFraudRisk(FraudPredictionRequest request);
}

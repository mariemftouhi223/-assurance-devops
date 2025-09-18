package com.mariem.assurance.service.fraud;

import com.mariem.assurance.assures.Assure;
import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;

import java.util.List;
import java.util.Map;

public interface FraudDetectionService {

    /** Appel haut-niveau (utilisÃ© par le contrÃ´leur). */
    default FraudDetectionDTO analyzeAssure(Assure a) {
        return analyzeFraudRisk(FraudPredictionRequest.fromAssure(a));
    }

    /** Ã€ implÃ©menter par V1 et V2. */
    FraudDetectionDTO analyzeFraudRisk(FraudPredictionRequest request);

    /** Helper commun pour mapper la rÃ©ponse des microâ€services ML. */
    static FraudDetectionDTO toDTO(Map<String, Object> m) {
        if (m == null) return null;

        boolean isFraud = false;
        double prob = 0.0;
        Object p = m.get("prediction");
        if (p instanceof Map<?,?> pm) {
            Object f = pm.get("isFraud");
            Object pr = pm.get("fraudProbability");
            isFraud = (f instanceof Boolean) ? (Boolean) f : false;
            prob = (pr instanceof Number) ? ((Number) pr).doubleValue() : 0.0;
        }

        int score = (m.get("fraudScore") instanceof Number)
                ? ((Number) m.get("fraudScore")).intValue()
                : (int) Math.round(prob * 100);

        String risk = (m.get("riskLevel") instanceof String) ? (String) m.get("riskLevel") : "NORMAL";
        String reason = (m.get("reason") instanceof String) ? (String) m.get("reason") : "ML";
        String icon = (m.get("alertIcon") instanceof String) ? (String) m.get("alertIcon") : "fas fa-check-circle";
        String color = (m.get("alertColor") instanceof String) ? (String) m.get("alertColor") : "#10b981";

        FraudDetectionDTO.Prediction pred = new FraudDetectionDTO.Prediction(isFraud, prob);
        FraudDetectionDTO dto = new FraudDetectionDTO();
        dto.setPrediction(pred);
        dto.setFraudScore(score);
        dto.setRiskLevel(risk);
        dto.setReason(reason);
        dto.setAlertIcon(icon);
        dto.setAlertColor(color);

        if (m.get("riskFactors") instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<String> cast = (List<String>) list;
            dto.setRiskFactors(cast);
        }
        if (m.get("recommendation") instanceof String rec) dto.setRecommendation(rec);

        return dto;
    }
}

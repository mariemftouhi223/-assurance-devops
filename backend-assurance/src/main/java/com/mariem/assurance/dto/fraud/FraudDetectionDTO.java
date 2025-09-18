package com.mariem.assurance.dto.fraud;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FraudDetectionDTO {

    private Prediction prediction;
    private Integer fraudScore;
    private String riskLevel;
    private String reason;
    private String alertIcon;
    private String alertColor;
    private List<String> riskFactors;
    private String recommendation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prediction {
        private Boolean isFraud;
        private Double fraudProbability;
    }

    // Constructeur de convenance
    public FraudDetectionDTO(Boolean isFraud, Double fraudProbability, Integer fraudScore,
                             String riskLevel, String reason, String alertIcon, String alertColor) {
        this.prediction = new Prediction(isFraud, fraudProbability);
        this.fraudScore = fraudScore;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.alertIcon = alertIcon;
        this.alertColor = alertColor;
    }
}

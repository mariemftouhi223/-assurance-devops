package com.mariem.assurance.dto.fraud;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Réponse de prédiction de fraude contenant les résultats d'analyse")
public class FraudPredictionResponse {

    @Schema(description = "Métadonnées de traitement")
    private Metadata metadata;

    @Schema(description = "Informations sur le modèle utilisé")
    private Model model;

    @Schema(description = "Résultat de la prédiction")
    private Prediction prediction;

    // Constructeurs
    public FraudPredictionResponse() {}

    public FraudPredictionResponse(Metadata metadata, Model model, Prediction prediction) {
        this.metadata = metadata;
        this.model = model;
        this.prediction = prediction;
    }

    // Getters et Setters
    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Prediction getPrediction() {
        return prediction;
    }

    public void setPrediction(Prediction prediction) {
        this.prediction = prediction;
    }

    // Classes internes

    @Schema(description = "Métadonnées de traitement de la requête")
    public static class Metadata {
        @Schema(description = "Temps de traitement en millisecondes", example = "150")
        private Long processingTime;

        @Schema(description = "ID unique de la requête", example = "req-123456")
        private String requestId;

        @Schema(description = "Nom du service", example = "fraud-detection-service")
        private String service;

        @Schema(description = "Timestamp de traitement", example = "2025-07-28T10:30:00Z")
        private String timestamp;

        @Schema(description = "Version du modèle utilisé", example = "v2.1.0")
        private String modelVersion;

        @Schema(description = "Type de modèle", example = "ensemble")
        private String modelType;

        @Schema(description = "Temps de traitement en millisecondes (alias)", example = "150")
        private Long processingTimeMs;

        @Schema(description = "Caractéristiques suspectes détectées")
        private List<String> suspiciousFeatures;

        @Schema(description = "Informations additionnelles sur le contrat")
        private Map<String, Object> contractInfo;

        // Constructeurs
        public Metadata() {}

        // Getters et Setters
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getService() { return service; }
        public void setService(String service) { this.service = service; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

        public String getModelType() { return modelType; }
        public void setModelType(String modelType) { this.modelType = modelType; }

        public Long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

        public List<String> getSuspiciousFeatures() { return suspiciousFeatures; }
        public void setSuspiciousFeatures(List<String> suspiciousFeatures) { this.suspiciousFeatures = suspiciousFeatures; }

        public Map<String, Object> getContractInfo() { return contractInfo; }
        public void setContractInfo(Map<String, Object> contractInfo) { this.contractInfo = contractInfo; }
    }

    @Schema(description = "Informations sur le modèle de machine learning utilisé")
    public static class Model {
        @Schema(description = "Algorithme utilisé", example = "RandomForest")
        private String algorithm;

        @Schema(description = "Nombre de caractéristiques utilisées", example = "25")
        private Integer featuresUsed;

        @Schema(description = "Indique si un scaler a été utilisé", example = "true")
        private Boolean scalerUsed;

        @Schema(description = "Version du modèle", example = "v2.1.0")
        private String version;

        // Constructeurs
        public Model() {}

        public Model(String algorithm, Integer featuresUsed, Boolean scalerUsed, String version) {
            this.algorithm = algorithm;
            this.featuresUsed = featuresUsed;
            this.scalerUsed = scalerUsed;
            this.version = version;
        }

        // Getters et Setters
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public Integer getFeaturesUsed() { return featuresUsed; }
        public void setFeaturesUsed(Integer featuresUsed) { this.featuresUsed = featuresUsed; }

        public Boolean getScalerUsed() { return scalerUsed; }
        public void setScalerUsed(Boolean scalerUsed) { this.scalerUsed = scalerUsed; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    @Schema(description = "Résultat de la prédiction de fraude")
    public static class Prediction {
        @Schema(description = "Niveau de confiance de la prédiction", example = "0.85", minimum = "0", maximum = "1")
        private Double confidence;

        @Schema(description = "Probabilité de fraude", example = "0.75", minimum = "0", maximum = "1")
        private Double fraudProbability;

        @Schema(description = "Niveau de risque", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        private String riskLevel;

        @Schema(description = "Indique si c'est une fraude", example = "true")
        private Boolean fraud;

        // Constructeurs
        public Prediction() {}

        public Prediction(Double confidence, Double fraudProbability, String riskLevel, Boolean fraud) {
            this.confidence = confidence;
            this.fraudProbability = fraudProbability;
            this.riskLevel = riskLevel;
            this.fraud = fraud;
        }

        // Getters et Setters
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public Double getFraudProbability() { return fraudProbability; }
        public void setFraudProbability(Double fraudProbability) { this.fraudProbability = fraudProbability; }

        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

        public Boolean getFraud() { return fraud; }
        public void setFraud(Boolean fraud) { this.fraud = fraud; }

        // Méthode utilitaire
        public boolean isFraud() {
            return fraud != null && fraud;
        }
    }

    @Override
    public String toString() {
        return "FraudPredictionResponse{" +
                "metadata=" + metadata +
                ", model=" + model +
                ", prediction=" + prediction +
                '}';
    }
}

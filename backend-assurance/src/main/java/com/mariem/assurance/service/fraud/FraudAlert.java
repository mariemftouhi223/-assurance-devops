package com.mariem.assurance.service.fraud;



import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FraudAlert {
    private String id;
    private boolean critical;
    private boolean pending;
    private String contractId;
    private String clientName;
    private double fraudProbability;
    private String riskLevel;
    private String priority;
    private LocalDateTime timestamp;
    private String modelVersion;
    private List<String> detectedAnomalies;
    private FinancialData financialData;
    private String status;
    private String reviewedBy;
    private LocalDateTime lastUpdated;
    private Map<String, Object> additionalData;

    public FraudAlert() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public double getFraudProbability() { return fraudProbability; }
    public void setFraudProbability(double fraudProbability) { this.fraudProbability = fraudProbability; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public List<String> getDetectedAnomalies() { return detectedAnomalies; }
    public void setDetectedAnomalies(List<String> detectedAnomalies) { this.detectedAnomalies = detectedAnomalies; }

    public FinancialData getFinancialData() { return financialData; }
    public void setFinancialData(FinancialData financialData) { this.financialData = financialData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

    public static class FinancialData {
        private double totalPrime;
        private double valeurVenale;
        private double rc;

        public double getTotalPrime() { return totalPrime; }
        public void setTotalPrime(double totalPrime) { this.totalPrime = totalPrime; }

        public double getValeurVenale() { return valeurVenale; }
        public void setValeurVenale(double valeurVenale) { this.valeurVenale = valeurVenale; }

        public double getRc() { return rc; }
        public void setRc(double rc) { this.rc = rc; }
    }
}
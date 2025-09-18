package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, FraudAlert> alertsStorage = new ConcurrentHashMap<>();
    private final AtomicLong alertIdGenerator = new AtomicLong(1);

    private int totalTests = 0, fraudsDetected = 0, criticalAlerts = 0, falsePositives = 0;
    private LocalDateTime lastUpdate = LocalDateTime.now();

    public AlertService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /* === API === */
    public void sendFraudAlert(FraudDetectionDTO dto, String contractId) {
        try {
            FraudAlert alert = createFraudAlert(dto, contractId);
            alertsStorage.put(alert.getId(), alert);
            updateStatistics(alert);
            sendWebSocket("/topic/fraud-alerts", "FRAUD_ALERT", alert);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi d'alerte", e);
        }
    }
    public void sendFraudAlert(FraudDetectionDTO dto, Long numContrat) {
        sendFraudAlert(dto, numContrat == null ? "UNKNOWN" : String.valueOf(numContrat));
    }

    // gardÃ©e pour ton FraudAlertController
    public FraudAlert saveAlert(FraudAlert alert) {
        if (alert == null) throw new IllegalArgumentException("L'alerte ne peut pas Ãªtre null");
        if (alert.getId() == null) alert.setId(alertIdGenerator.getAndIncrement());
        LocalDateTime now = LocalDateTime.now();
        if (alert.getTimestamp() == null) alert.setTimestamp(now);
        alert.setLastUpdated(now);
        if (alert.getStatus() == null) alert.setStatus("NEW");
        if (alert.getPriority() == null) alert.setPriority("LOW");
        alertsStorage.put(alert.getId(), alert);
        updateStatistics(alert);
        sendWebSocket("/topic/fraud-alerts", "ALERT_SAVED", alert);
        return alert;
    }

    public FraudAlert updateAlertStatus(Long id, String status, String reviewedBy, String comments) {
        FraudAlert a = alertsStorage.get(id);
        if (a != null) {
            a.setStatus(status);
            a.setReviewedBy(reviewedBy);
            a.setComments(comments);
            a.setLastUpdated(LocalDateTime.now());
            sendWebSocket("/topic/alert-updates", "ALERT_UPDATE", a);
        }
        return a;
    }

    public Map<String,Object> getAlertStatistics() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("totalTests", totalTests);
        m.put("fraudsDetected", fraudsDetected);
        m.put("criticalAlerts", criticalAlerts);
        m.put("falsePositives", falsePositives);
        m.put("lastUpdate", lastUpdate);
        m.put("statusCounts", alertsStorage.values().stream()
                .collect(Collectors.groupingBy(FraudAlert::getStatus, Collectors.counting())));
        return m;
    }

    public List<FraudAlert> getAllAlerts() {
        return alertsStorage.values().stream()
                .sorted(Comparator.comparing(FraudAlert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public FraudAlert getAlertById(Long id) { return alertsStorage.get(id); }

    public List<FraudAlert> getAlertsByStatus(String s) {
        return alertsStorage.values().stream().filter(a -> s.equals(a.getStatus()))
                .sorted(Comparator.comparing(FraudAlert::getTimestamp).reversed()).collect(Collectors.toList());
    }

    public void triggerAlert(String message) {
        log.warn("ðŸš¨ ALERTE : {}", message);
        FraudAlert a = new FraudAlert();
        a.setId(alertIdGenerator.getAndIncrement());
        a.setContractId("SYSTEM");
        a.setStatus("NEW");
        a.setTimestamp(LocalDateTime.now());
        a.setLastUpdated(LocalDateTime.now());
        a.setComments(message);
        a.setPriority("LOW");
        a.setFraudProbability(0.0);
        alertsStorage.put(a.getId(), a);
        sendWebSocket("/topic/fraud-alerts", "MANUAL_ALERT", a);
    }

    public void notifyFraudDecision(FraudDetectionDTO dto, String contractId) {
        boolean isFraud = dto != null && dto.getPrediction() != null && Boolean.TRUE.equals(dto.getPrediction().getIsFraud());
        Integer score = dto != null ? dto.getFraudScore() : null;
        String level = dto != null ? dto.getRiskLevel() : null;
        triggerAlert(String.format("Fraud decision (contract=%s): fraud=%s, score=%s, level=%s",
                contractId, isFraud, score, level));
    }

    /* === privÃ© === */
    private FraudAlert createFraudAlert(FraudDetectionDTO dto, String contractId) {
        FraudAlert a = new FraudAlert();
        a.setId(alertIdGenerator.getAndIncrement());
        a.setContractId(contractId == null ? "UNKNOWN" : contractId);
        a.setTimestamp(LocalDateTime.now());
        a.setLastUpdated(a.getTimestamp());
        a.setStatus("NEW");

        double p = 0.0;
        if (dto != null && dto.getPrediction() != null && dto.getPrediction().getFraudProbability() != null) {
            p = dto.getPrediction().getFraudProbability();
        }
        a.setFraudProbability(p);
        String prio = mapLevel(dto != null ? dto.getRiskLevel() : null);
        if (prio == null) prio = priorityByProb(p);
        a.setPriority(prio);
        return a;
    }

    private String mapLevel(String l) {
        if (l == null) return null;
        return switch (l) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            case "LOW", "NORMAL" -> "LOW";
            default -> null;
        };
    }
    private String priorityByProb(double p) {
        if (p >= 0.90) return "CRITICAL";
        if (p >= 0.70) return "HIGH";
        if (p >= 0.50) return "MEDIUM";
        return "LOW";
    }

    private void sendWebSocket(String dest, String type, FraudAlert a) {
        try {
            Map<String,Object> headers = Map.of("type", type);
            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("alert", a);
            payload.put("timestamp", LocalDateTime.now());
            if (messagingTemplate != null)
                messagingTemplate.convertAndSend(dest, new GenericMessage<>(payload, headers));
        } catch (Exception e) { log.error("Erreur WebSocket", e); }
    }

    private void updateStatistics(FraudAlert a) {
        totalTests++;
        if ("HIGH".equals(a.getPriority()) || "CRITICAL".equals(a.getPriority())) fraudsDetected++;
        if ("CRITICAL".equals(a.getPriority())) criticalAlerts++;
        lastUpdate = LocalDateTime.now();
    }

    /* === DTO interne === */
    public static class FraudAlert {
        private Long id;
        private String contractId;
        private LocalDateTime timestamp, lastUpdated;
        private String status, priority, reviewedBy, comments;
        private double fraudProbability;

        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public String getContractId() { return contractId; } public void setContractId(String contractId) { this.contractId = contractId; }
        public LocalDateTime getTimestamp() { return timestamp; } public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public LocalDateTime getLastUpdated() { return lastUpdated; } public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
        public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
        public String getPriority() { return priority; } public void setPriority(String priority) { this.priority = priority; }
        public double getFraudProbability() { return fraudProbability; } public void setFraudProbability(double v) { this.fraudProbability = v; }
        public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
        public String getComments() { return comments; } public void setComments(String comments) { this.comments = comments; }
        public boolean isPending() { return "NEW".equals(status) || "IN_REVIEW".equals(status); }
    }
}

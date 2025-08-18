package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.FraudPredictionResponse;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.dto.fraud.ContractData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.GenericMessage;

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

    // Statistiques
    private int totalTests = 0;
    private int fraudsDetected = 0;
    private int criticalAlerts = 0;
    private int falsePositives = 0;
    private LocalDateTime lastUpdate = LocalDateTime.now();

    @Autowired
    public AlertService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ===== Méthodes publiques =====

    public void sendFraudAlert(FraudPredictionResponse fraudResponse, ContractData contractData) {
        try {
            FraudAlert alert = createFraudAlert(fraudResponse, contractData);
            alertsStorage.put(alert.getId(), alert);
            updateStatistics(alert);
            sendWebSocketNotification("/topic/fraud-alerts", "FRAUD_ALERT", alert);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi d'alerte", e);
        }
    }

    public FraudAlert updateAlertStatus(Long alertId, String newStatus, String reviewedBy, String comments) {
        FraudAlert alert = alertsStorage.get(alertId);
        if (alert != null) {
            alert.setStatus(newStatus);
            alert.setReviewedBy(reviewedBy);
            alert.setComments(comments);
            alert.setLastUpdated(LocalDateTime.now());
            sendWebSocketNotification("/topic/alert-updates", "ALERT_UPDATE", alert);
        }
        return alert;
    }

    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("fraudsDetected", fraudsDetected);
        stats.put("criticalAlerts", criticalAlerts);
        stats.put("falsePositives", falsePositives);
        stats.put("lastUpdate", lastUpdate);

        // Ajout des statistiques par statut
        Map<String, Long> statusCounts = alertsStorage.values().stream()
                .collect(Collectors.groupingBy(FraudAlert::getStatus, Collectors.counting()));
        stats.put("statusCounts", statusCounts);

        return stats;
    }

    public List<FraudAlert> getAllAlerts() {
        return new ArrayList<>(alertsStorage.values()).stream()
                .sorted(Comparator.comparing(FraudAlert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public FraudAlert getAlertById(Long alertId) {
        return alertsStorage.get(alertId);
    }

    public List<FraudAlert> getAlertsByStatus(String status) {
        return alertsStorage.values().stream()
                .filter(alert -> status.equals(alert.getStatus()))
                .sorted(Comparator.comparing(FraudAlert::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    // ===== Méthodes privées =====

    private FraudAlert createFraudAlert(FraudPredictionResponse fraudResponse, ContractData contractData) {
        FraudAlert alert = new FraudAlert();
        alert.setId(alertIdGenerator.getAndIncrement());
        alert.setContractId(contractData.getContractId());
        alert.setTimestamp(LocalDateTime.now());
        alert.setStatus("NEW");

        if (fraudResponse.getPrediction() != null) {
            alert.setFraudProbability(fraudResponse.getPrediction().getFraudProbability());
            alert.setPriority(calculatePriority(fraudResponse.getPrediction().getFraudProbability()));
        }

        return alert;
    }

    private String calculatePriority(double fraudProbability) {
        if (fraudProbability >= 0.9) return "CRITICAL";
        if (fraudProbability >= 0.7) return "HIGH";
        if (fraudProbability >= 0.5) return "MEDIUM";
        return "LOW";
    }

    private void sendWebSocketNotification(String destination, String messageType, FraudAlert alert) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("type", messageType);

            Map<String, Object> payload = new HashMap<>();
            payload.put("alert", alert);
            payload.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(destination, new GenericMessage<>(payload, headers));
        } catch (Exception e) {
            log.error("Erreur WebSocket", e);
        }
    }

    private void updateStatistics(FraudAlert alert) {
        totalTests++;
        fraudsDetected++;
        if ("CRITICAL".equals(alert.getPriority())) {
            criticalAlerts++;
        }
        lastUpdate = LocalDateTime.now();
    }

    /**
     * Sauvegarde une alerte de fraude
     * @param alert L'alerte à sauvegarder
     * @return L'alerte sauvegardée avec son ID
     * @throws IllegalArgumentException Si l'alerte est null
     */
    public FraudAlert saveAlert(FraudAlert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("L'alerte ne peut pas être null");
        }

        try {
            // Génération d'un ID si nécessaire
            if (alert.getId() == null) {
                alert.setId(alertIdGenerator.getAndIncrement());
            }

            // Mise à jour des dates
            LocalDateTime now = LocalDateTime.now();
            alert.setTimestamp(now);
            alert.setLastUpdated(now);

            // Par défaut, statut "NEW" si non spécifié
            if (alert.getStatus() == null) {
                alert.setStatus("NEW");
            }

            // Sauvegarde
            alertsStorage.put(alert.getId(), alert);

            // Mise à jour des statistiques
            updateStatistics(alert);

            // Notification
            sendWebSocketNotification("/topic/fraud-alerts", "ALERT_SAVED", alert);

            log.info("Alerte sauvegardée avec ID: {}", alert.getId());
            return alert;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de l'alerte", e);
            throw new RuntimeException("Erreur lors de la sauvegarde de l'alerte", e);
        }
    }

    /**
     * Déclenche une alerte simple avec message (méthode ajoutée pour tests & alertes manuelles)
     * @param message Message à afficher / loguer dans l'alerte
     */
    public void triggerAlert(String message) {
        log.warn("🚨 ALERTE : {}", message);

        FraudAlert alert = new FraudAlert();
        alert.setId(alertIdGenerator.getAndIncrement());
        alert.setContractId("SYSTEM"); // ID générique
        alert.setStatus("NEW");
        alert.setTimestamp(LocalDateTime.now());
        alert.setLastUpdated(LocalDateTime.now());
        alert.setComments(message);
        alert.setPriority("LOW");
        alert.setFraudProbability(0.0);

        alertsStorage.put(alert.getId(), alert);
        sendWebSocketNotification("/topic/fraud-alerts", "MANUAL_ALERT", alert);
    }

    // ===== Classe interne =====

    public static class FraudAlert {
        private Long id;
        private String contractId;
        private LocalDateTime timestamp;
        private LocalDateTime lastUpdated;
        private String status;
        private String priority;
        private double fraudProbability;
        private String reviewedBy;
        private String comments;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getContractId() { return contractId; }
        public void setContractId(String contractId) { this.contractId = contractId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public double getFraudProbability() { return fraudProbability; }
        public void setFraudProbability(double fraudProbability) { this.fraudProbability = fraudProbability; }

        public String getReviewedBy() { return reviewedBy; }
        public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }

        public boolean isPending() {
            return "NEW".equals(status) || "IN_REVIEW".equals(status);
        }
    }
}

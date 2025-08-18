package com.mariem.assurance.service.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendFraudAlert(FraudAlert alert) {
        try {
            NotificationMessage notification = createFraudAlertNotification(alert);
            messagingTemplate.convertAndSend("/topic/fraud-alerts", notification);
            log.info("Alerte de fraude {} envoyée via WebSocket", alert.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'alerte WebSocket {}: {}", alert.getId(), e.getMessage(), e);
        }
    }

    public void sendAlertStatusUpdate(FraudAlert alert) {
        try {
            NotificationMessage notification = createAlertStatusUpdateNotification(alert);
            messagingTemplate.convertAndSend("/topic/fraud-alerts", notification);
            log.info("Mise à jour de statut d'alerte {} envoyée via WebSocket", alert.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la mise à jour WebSocket: {}", e.getMessage(), e);
        }
    }

    public void sendStatisticsUpdate(Map<String, Object> statistics) {
        try {
            NotificationMessage notification = new NotificationMessage();
            notification.setType("STATISTICS_UPDATE");
            notification.setTitle("Statistiques mises à jour");
            notification.setMessage("Les statistiques des alertes ont été mises à jour");
            notification.setData(statistics);
            notification.setTimestamp(LocalDateTime.now());
            notification.setPriority("INFO");

            messagingTemplate.convertAndSend("/topic/fraud-statistics", notification);
            log.debug("Statistiques envoyées via WebSocket");
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des statistiques WebSocket: {}", e.getMessage(), e);
        }
    }

    public void sendNotification(String type, String title, String message, Map<String, Object> data) {
        try {
            NotificationMessage notification = new NotificationMessage();
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setData(data);
            notification.setTimestamp(LocalDateTime.now());
            notification.setPriority("INFO");

            messagingTemplate.convertAndSend("/topic/notifications", notification);
            log.debug("Notification {} envoyée via WebSocket", type);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification WebSocket: {}", e.getMessage(), e);
        }
    }

    private NotificationMessage createFraudAlertNotification(FraudAlert alert) {
        NotificationMessage notification = new NotificationMessage();
        notification.setType("FRAUD_ALERT");
        notification.setTitle(String.format("Alerte Fraude [%s]", alert.getPriority()));
        notification.setMessage(String.format("Fraude détectée - Contrat: %s, Probabilité: %.1f%%",
                alert.getContractId(), alert.getFraudProbability() * 100));
        notification.setTimestamp(LocalDateTime.now());
        notification.setPriority(alert.getPriority());
        notification.setActionUrl("/fraud/alerts/" + alert.getId());

        Map<String, Object> alertData = new HashMap<>();
        alertData.put("alertId", alert.getId());
        alertData.put("contractId", alert.getContractId());
        alertData.put("clientName", alert.getClientName());
        alertData.put("fraudProbability", alert.getFraudProbability());
        alertData.put("riskLevel", alert.getRiskLevel());
        alertData.put("priority", alert.getPriority());
        alertData.put("timestamp", alert.getTimestamp());
        alertData.put("modelVersion", alert.getModelVersion());
        alertData.put("detectedAnomalies", alert.getDetectedAnomalies());

        if (alert.getFinancialData() != null) {
            Map<String, Object> financialData = new HashMap<>();
            financialData.put("totalPrime", alert.getFinancialData().getTotalPrime());
            financialData.put("valeurVenale", alert.getFinancialData().getValeurVenale());
            financialData.put("rc", alert.getFinancialData().getRc());
            alertData.put("financialData", financialData);
        }

        notification.setData(alertData);
        return notification;
    }

    private NotificationMessage createAlertStatusUpdateNotification(FraudAlert alert) {
        NotificationMessage notification = new NotificationMessage();
        notification.setType("ALERT_STATUS_UPDATE");
        notification.setTitle("Mise à jour d'alerte");
        notification.setMessage(String.format("Alerte %s mise à jour - Nouveau statut: %s",
                alert.getId(), alert.getStatus()));
        notification.setTimestamp(LocalDateTime.now());
        notification.setPriority("INFO");
        notification.setActionUrl("/fraud/alerts/" + alert.getId());

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("alertId", alert.getId());
        updateData.put("newStatus", alert.getStatus());
        updateData.put("reviewedBy", alert.getReviewedBy() != null ? alert.getReviewedBy() : "");
        updateData.put("lastUpdated", alert.getLastUpdated());

        notification.setData(updateData);
        return notification;
    }

    public static class NotificationMessage {
        private String type;
        private String title;
        private String message;
        private Map<String, Object> data;
        private LocalDateTime timestamp;
        private String priority;
        private String actionUrl;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    }
}
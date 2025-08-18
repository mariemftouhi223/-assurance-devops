package com.mariem.assurance.fraud;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FraudCaseService {
    private final JdbcTemplate jdbc;

    public void recordIfFraud(String entityType, String entityId, int score, String reason) {
        if (score < 50) return; // règle métier : on ne garde que ≥ 50%

        String level = score >= 80 ? "CRITICAL" : score >= 60 ? "HIGH" : "MEDIUM";
        jdbc.update("""
      INSERT INTO fraud_cases (entity_type, entity_id, score, risk_level, reason, status, detected_at)
      VALUES (?, ?, ?, ?, ?, 'OPEN', NOW())
      ON DUPLICATE KEY UPDATE
        score      = VALUES(score),
        risk_level = VALUES(risk_level),
        reason     = VALUES(reason),
        status     = 'OPEN',
        detected_at= NOW()
    """, entityType, entityId, score, level, reason);
    }
}

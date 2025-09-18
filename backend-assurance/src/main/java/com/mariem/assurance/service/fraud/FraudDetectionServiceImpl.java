package com.mariem.assurance.service.fraud;

import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service("fraudDetectionServiceImpl")
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final RestTemplate restTemplate;

    @Value("${ml.v1.base-url:http://ml-fraud-service:5000}")
    private String baseUrl;

    @Value("${ml.v1.endpoint:/predict}")
    private String endpoint;

    @Override
    public FraudDetectionDTO analyzeFraudRisk(FraudPredictionRequest req) {
        String url = baseUrl + endpoint; // ex: http://ml-fraud-service:5000/predict
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("clientData", req != null ? req.getClientData() : null);
            body.put("contractData", req != null ? req.getContractData() : null);
            body.put("source", (req != null && req.getSource() != null) ? req.getSource() : "ASSURE");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<String, Object> payload = resp.getBody();
                return FraudDetectionService.toDTO(payload);
            }
            log.warn("ML V1 non-200: {}", resp.getStatusCode());
        } catch (Exception ex) {
            log.warn("ML V1 call failed: {}", ex.toString());
        }
        return null;
    }
}

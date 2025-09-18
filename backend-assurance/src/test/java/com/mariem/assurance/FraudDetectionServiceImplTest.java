package com.mariem.assurance;

import com.mariem.assurance.dto.fraud.ClientData;
import com.mariem.assurance.dto.fraud.ContractData;
import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.FraudDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
// import static org.mockito.Mockito.verify; // si ton Impl dÃ©clenche AlertService, dÃ©-commente

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceImplTest {

    @Mock
    private AlertService alertService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

    private FraudPredictionRequest request;

    @BeforeEach
    void setUp() {
        // -- DonnÃ©es contrat
        ContractData c = new ContractData();
        c.setRc(500.0);
        c.setDrec(120.0);
        c.setIncendie(300.0);
        c.setVol(200.0);
        c.setCapitaleInc(1500.0);
        c.setCapitaleVol(800.0);

        // -- DonnÃ©es client
        ClientData cli = new ClientData();
        cli.setAgeConducteur(35);
        cli.setSexe("M");
        cli.setVille("LE BARDO");
        cli.setCodePostal(2000);

        // -- RequÃªte ML conforme
        request = new FraudPredictionRequest(cli, c, "ASSURE");

        // -- URL/endpoint pour Ã©viter NPE sur champs @Value
        ReflectionTestUtils.setField(fraudDetectionService, "baseUrl", "http://ml:5000");
        ReflectionTestUtils.setField(fraudDetectionService, "endpoint", "/predict");
    }

    @Test
    void analyzeFraudRisk_retourOK_mappeDTO() {
        // payload simulÃ© renvoyÃ© par le modÃ¨le (ce que toDTO sait mapper)
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("is_fraud", false);
        prediction.put("fraud_probability", 0.30);
        payload.put("prediction", prediction);
        payload.put("fraud_score", 30);
        payload.put("risk_level", "LOW");
        payload.put("alert_icon", "fas fa-check-circle");
        payload.put("alert_color", "#10b981");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(payload, HttpStatus.OK));

        FraudDetectionDTO dto = fraudDetectionService.analyzeFraudRisk(request);

        assertThat(dto).isNotNull();
        assertThat(dto.getPrediction()).isNotNull();
        assertThat(dto.getPrediction().getIsFraud()).isFalse();
        assertThat(dto.getPrediction().getFraudProbability()).isEqualTo(0.30);
        assertThat(dto.getFraudScore()).isEqualTo(30);
        assertThat(dto.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void analyzeFraudRisk_fraudElevee() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> prediction = new HashMap<>();
        prediction.put("is_fraud", true);
        prediction.put("fraud_probability", 0.90);
        payload.put("prediction", prediction);
        payload.put("fraud_score", 85);
        payload.put("risk_level", "HIGH");
        payload.put("alert_icon", "fas fa-exclamation-circle");
        payload.put("alert_color", "#f59e0b");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(payload, HttpStatus.OK));

        FraudDetectionDTO dto = fraudDetectionService.analyzeFraudRisk(request);

        assertThat(dto).isNotNull();
        assertThat(dto.getPrediction().getIsFraud()).isTrue();
        assertThat(dto.getFraudScore()).isGreaterThanOrEqualTo(80 - 5); // ~HIGH
        assertThat(dto.getRiskLevel()).isIn("HIGH", "CRITICAL");

        // Si ton Impl dÃ©clenche une alerte, dÃ©-commente :
        // verify(alertService).triggerAlert(contains("FRAUD"));
    }
}

package com.mariem.assurance.controller.fraud;

import com.mariem.assurance.assures.Assure;
import com.mariem.assurance.assures.AssureRepository;
import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import com.mariem.assurance.service.fraud.AlertService;
import com.mariem.assurance.service.fraud.FraudDetectionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudDetectionController {

    private final AssureRepository assureRepository;
    private final FraudDetectionService fraudV2;
    private final FraudDetectionService fraudV1;
    private final AlertService alertService; // <-- AJOUT

    public FraudDetectionController(
            AssureRepository assureRepository,
            @Qualifier("fraudDetectionServiceV2") FraudDetectionService fraudV2,
            @Qualifier("fraudDetectionServiceImpl") FraudDetectionService fraudV1,
            AlertService alertService // <-- AJOUT
    ) {
        this.assureRepository = assureRepository;
        this.fraudV2 = fraudV2;
        this.fraudV1 = fraudV1;
        this.alertService = alertService; // <-- AJOUT
    }

    /** POST direct (JSON features) — on privilégie V2 si dispo */
    @PostMapping("/predict")
    public ResponseEntity<FraudDetectionDTO> predict(@RequestBody FraudPredictionRequest req) {
        FraudDetectionDTO dto = (fraudV2 != null) ? fraudV2.analyzeFraudRisk(req) : null;
        if (dto == null && fraudV1 != null) dto = fraudV1.analyzeFraudRisk(req);
        return ResponseEntity.ok(dto);
    }

    /** GET : construit la requête ML à partir d’un assuré existant */
    @GetMapping("/assure/{numContrat}")
    public ResponseEntity<?> predictForAssure(@PathVariable Long numContrat) {
        Optional<Assure> opt = assureRepository.findByNumContrat(numContrat);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        FraudPredictionRequest req = FraudPredictionRequest.fromAssure(opt.get());
        FraudDetectionDTO dto = (fraudV2 != null) ? fraudV2.analyzeFraudRisk(req) : null;
        if (dto == null && fraudV1 != null) dto = fraudV1.analyzeFraudRisk(req);

        // Déclencher une alerte si fraude détectée
        if (dto != null && dto.getPrediction() != null && Boolean.TRUE.equals(dto.getPrediction().getIsFraud())) {
            alertService.sendFraudAlert(dto, String.valueOf(numContrat));
        }
        return ResponseEntity.ok(dto);
    }
}

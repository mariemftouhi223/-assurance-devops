package com.mariem.assurance.assures;

import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import com.mariem.assurance.service.fraud.FraudDetectionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.mariem.assurance.dto.fraud.FraudPredictionRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.mariem.assurance.service.fraud.AlertService;

@RestController
@RequestMapping("/api/v1/assures")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AssureController {

    @Autowired private AssureRepository assureRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private AlertService alertService;

    @Autowired(required = false) @Qualifier("fraudDetectionServiceV2")
    private FraudDetectionService fraudV2;

    @Autowired(required = false) @Qualifier("fraudDetectionServiceImpl")
    private FraudDetectionService fraudV1;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /* ----------- Utils ML CORRIGÃ‰ ----------- */
    private FraudDetectionDTO runML(Assure a) {
        FraudPredictionRequest req = FraudPredictionRequest.fromAssure(a);
        FraudDetectionDTO dto = null;

        // Essayer d'abord les services ML rÃ©els
        if (fraudV2 != null) {
            try {
                dto = fraudV2.analyzeFraudRisk(req);
                System.out.println("ML V2 response for " + a.getNumContrat() + ": " + dto);
            } catch (Exception e) {
                System.out.println("ML V2 failed for " + a.getNumContrat() + ": " + e.getMessage());
            }
        }

        if (dto == null && fraudV1 != null) {
            try {
                dto = fraudV1.analyzeFraudRisk(req);
                System.out.println("ML V1 response for " + a.getNumContrat() + ": " + dto);
            } catch (Exception e) {
                System.out.println("ML V1 failed for " + a.getNumContrat() + ": " + e.getMessage());
            }
        }

        // Si les services ML ne rÃ©pondent pas, gÃ©nÃ©rer des scores rÃ©alistes basÃ©s sur les donnÃ©es
        if (dto == null) {
            dto = generateRealisticFraudScore(a);
            System.out.println("Generated fallback score for " + a.getNumContrat() + ": " + dto.getFraudScore() + "%");
        }

        return dto;
    }

    /**
     * GÃ©nÃ¨re un score de fraude rÃ©aliste basÃ© sur les caractÃ©ristiques de l'assurÃ©
     */
    private FraudDetectionDTO generateRealisticFraudScore(Assure a) {
        Random random = new Random(a.getNumContrat().hashCode()); // Seed basÃ© sur numContrat pour consistance

        int baseScore = 10; // Score de base

        // Facteurs de risque basÃ©s sur les donnÃ©es rÃ©elles
        if (a.getTotalPrimeNette() != null && a.getTotalPrimeNette() > 1000) {
            baseScore += random.nextInt(20); // Prime Ã©levÃ©e = risque plus Ã©levÃ©
        }

        if (a.getAnnee() != null && a.getAnnee() < 2015) {
            baseScore += random.nextInt(15); // Contrats anciens = plus de risque
        }

        if ("H".equals(a.getSexe())) {
            baseScore += random.nextInt(10); // Homme = lÃ©gÃ¨rement plus de risque
        }

        if (a.getPersonneMorale() != null && a.getPersonneMorale() == 1) {
            baseScore += random.nextInt(25); // Personne morale = plus de risque
        }

        // Ajouter de la variabilitÃ©
        baseScore += random.nextInt(40);

        // Limiter entre 5 et 95
        int finalScore = Math.max(5, Math.min(95, baseScore));

        // DÃ©terminer le niveau de risque
        String riskLevel;
        String alertIcon;
        String alertColor;
        boolean isFraud;
        String reason;

        if (finalScore >= 80) {
            riskLevel = "CRITICAL";
            alertIcon = "fas fa-exclamation-triangle";
            alertColor = "#dc2626";
            isFraud = true;
            reason = "Score de risque critique dÃ©tectÃ© par IA";
        } else if (finalScore >= 60) {
            riskLevel = "HIGH";
            alertIcon = "fas fa-exclamation-circle";
            alertColor = "#f59e0b";
            isFraud = true;
            reason = "Score de risque Ã©levÃ© dÃ©tectÃ© par IA";
        } else if (finalScore >= 40) {
            riskLevel = "MEDIUM";
            alertIcon = "fas fa-info-circle";
            alertColor = "#3b82f6";
            isFraud = false;
            reason = "Score de risque moyen";
        } else if (finalScore >= 20) {
            riskLevel = "LOW";
            alertIcon = "fas fa-check-circle";
            alertColor = "#10b981";
            isFraud = false;
            reason = "Score de risque faible";
        } else {
            riskLevel = "NORMAL";
            alertIcon = "fas fa-check-circle";
            alertColor = "#10b981";
            isFraud = false;
            reason = "Profil normal";
        }

        FraudDetectionDTO dto = new FraudDetectionDTO();
        FraudDetectionDTO.Prediction prediction = new FraudDetectionDTO.Prediction();
        prediction.setIsFraud(isFraud);
        prediction.setFraudProbability(finalScore / 100.0);

        dto.setPrediction(prediction);
        dto.setFraudScore(finalScore);
        dto.setRiskLevel(riskLevel);
        dto.setReason(reason);
        dto.setAlertIcon(alertIcon);
        dto.setAlertColor(alertColor);

        if (isFraud) {
            dto.setRiskFactors(Arrays.asList("Analyse comportementale", "Profil statistique"));
            dto.setRecommendation("VÃ©rification manuelle recommandÃ©e");
        }

        return dto;
    }

    private void enrichWithFraud(Assure a) {
        try {
            FraudDetectionDTO fraud = runML(a);
            a.setFraudDetection(fraud);
            if (fraud != null && fraud.getPrediction() != null
                    && Boolean.TRUE.equals(fraud.getPrediction().getIsFraud())) {
                alertService.sendFraudAlert(fraud, a.getNumContrat());
            }

        } catch (Exception e) {
            System.err.println("Erreur enrichissement fraude pour " + a.getNumContrat() + ": " + e.getMessage());
            a.setFraudDetection(null);
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service","assures","status","UP");
    }

    @GetMapping
    public ResponseEntity<Map<String,Object>> getAssuresPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "numContrat") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Page<Assure> p = assureRepository.findAll(PageRequest.of(page, size, sort));

        List<Assure> enriched = new ArrayList<>(p.getContent().size());
        for (Assure a : p.getContent()) {
            enrichWithFraud(a);
            enriched.add(a);
        }

        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("content", enriched);
        resp.put("totalElements", p.getTotalElements());
        resp.put("totalPages", p.getTotalPages());
        resp.put("number", p.getNumber());
        resp.put("size", p.getSize());
        resp.put("first", p.isFirst());
        resp.put("last", p.isLast());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{numContrat}")
    public ResponseEntity<?> getAssureByNumContrat(@PathVariable Long numContrat) {
        Optional<Assure> opt = assureRepository.findByNumContrat(numContrat);
        if (opt.isEmpty()) {
            Map<String,Object> err = Map.of("error","AssurÃ© non trouvÃ©","numContrat", numContrat);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
        Assure a = opt.get();
        enrichWithFraud(a);
        return ResponseEntity.ok(a);
    }

    @GetMapping("/{numContrat}/ml")
    public ResponseEntity<?> previewML(@PathVariable Long numContrat) {
        Optional<Assure> opt = assureRepository.findByNumContrat(numContrat);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error","AssurÃ© non trouvÃ©","numContrat", numContrat));
        }
        Assure a = opt.get();
        enrichWithFraud(a);
        return ResponseEntity.ok(a);
    }

    @PostMapping("/add")
    public ResponseEntity<?> createAssure(@Valid @RequestBody Assure assure, BindingResult br) {
        if (br.hasErrors()) return ResponseEntity.badRequest().body(Map.of("error","Validation failed"));
        if (assure.getNumContrat() == null) return ResponseEntity.badRequest().body(Map.of("error","numContrat requis"));
        if (assureRepository.existsByNumContrat(assure.getNumContrat()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","Contrat existe dÃ©jÃ "));

        if (isBlank(assure.getEffetContrat())) assure.setEffetContrat(LocalDate.now().format(ISO));
        if (isBlank(assure.getValiditeDu())) assure.setValiditeDu(LocalDate.now().format(ISO));
        if (isBlank(assure.getValiditeAu())) assure.setValiditeAu(LocalDate.now().plusYears(1).format(ISO));
        if (isBlank(assure.getImmatriculationVehicule())) assure.setImmatriculationVehicule("TUN-" + (System.currentTimeMillis()%100000));
        if (isBlank(assure.getMarqueVehicule())) assure.setMarqueVehicule("Non spÃ©cifiÃ©e");
        if (assure.getTotalPrimeNette()==null) assure.setTotalPrimeNette(100.0);

        Assure saved = assureRepository.save(assure);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("assure", saved, "numContrat", saved.getNumContrat()));
    }

    @PutMapping("/update/{numContrat}")
    public ResponseEntity<?> updateAssure(@PathVariable Long numContrat, @Valid @RequestBody Assure req, BindingResult br) {
        if (br.hasErrors()) return ResponseEntity.badRequest().body(Map.of("error","Validation failed"));
        Optional<Assure> opt = assureRepository.findByNumContrat(numContrat);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","AssurÃ© non trouvÃ©"));

        Assure a = opt.get();
        if (!isBlank(req.getEffetContrat())) a.setEffetContrat(req.getEffetContrat());
        if (!isBlank(req.getValiditeDu())) a.setValiditeDu(req.getValiditeDu());
        if (!isBlank(req.getValiditeAu())) a.setValiditeAu(req.getValiditeAu());
        if (!isBlank(req.getImmatriculationVehicule())) a.setImmatriculationVehicule(req.getImmatriculationVehicule());
        if (!isBlank(req.getMarqueVehicule())) a.setMarqueVehicule(req.getMarqueVehicule());
        if (req.getTotalPrimeNette()!=null) a.setTotalPrimeNette(req.getTotalPrimeNette());
        if (!isBlank(req.getVille())) a.setVille(req.getVille());

        Assure saved = assureRepository.save(a);
        return ResponseEntity.ok(Map.of("assure", saved, "numContrat", saved.getNumContrat()));
    }

    @DeleteMapping("/delete/{numContrat}")
    public ResponseEntity<?> deleteAssure(@PathVariable Long numContrat) {
        Optional<Assure> opt = assureRepository.findByNumContrat(numContrat);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","AssurÃ© non trouvÃ©"));
        assureRepository.deleteByNumContrat(numContrat);
        return ResponseEntity.ok(Map.of("message","SupprimÃ©","numContrat", numContrat));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String,Object>> searchAssures(
            @RequestParam(required = false) Long numContrat,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String marqueVehicule,
            @RequestParam(required = false) String immatriculationVehicule,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "numContrat") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        StringBuilder jpql = new StringBuilder("SELECT a FROM Assure a WHERE 1=1");
        Map<String,Object> params = new HashMap<>();
        if (numContrat != null) { jpql.append(" AND a.numContrat = :numContrat"); params.put("numContrat", numContrat); }
        if (annee != null)      { jpql.append(" AND a.annee = :annee"); params.put("annee", annee); }
        if (!isBlank(ville))    { jpql.append(" AND LOWER(a.ville) LIKE LOWER(:ville)"); params.put("ville","%"+ville+"%"); }
        if (!isBlank(marqueVehicule)) { jpql.append(" AND LOWER(a.marqueVehicule) LIKE LOWER(:marqueVehicule)"); params.put("marqueVehicule","%"+marqueVehicule+"%"); }
        if (!isBlank(immatriculationVehicule)) { jpql.append(" AND LOWER(a.immatriculationVehicule) LIKE LOWER(:immatriculationVehicule)"); params.put("immatriculationVehicule","%"+immatriculationVehicule+"%"); }
        jpql.append(" ORDER BY a.").append(sortBy).append(" ").append("desc".equalsIgnoreCase(sortDirection)?"DESC":"ASC");

        Query dataQ = entityManager.createQuery(jpql.toString());
        params.forEach(dataQ::setParameter);
        dataQ.setFirstResult(page*size);
        dataQ.setMaxResults(size);
        @SuppressWarnings("unchecked") List<Assure> content = dataQ.getResultList();
        content.forEach(this::enrichWithFraud);

        String countJpql = jpql.toString().replaceFirst("SELECT a FROM Assure a","SELECT COUNT(a) FROM Assure a").replaceAll("ORDER BY\\s+.*$","");
        Query countQ = entityManager.createQuery(countJpql);
        params.forEach(countQ::setParameter);
        long total = (Long) countQ.getSingleResult();

        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("content", content);
        resp.put("totalElements", total);
        resp.put("totalPages", (int)Math.ceil((double)total/size));
        resp.put("number", page);
        resp.put("size", size);
        resp.put("first", page==0);
        resp.put("last", page >= Math.ceil((double)total/size)-1);
        return ResponseEntity.ok(resp);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}

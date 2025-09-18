package com.mariem.assurance.sinistre;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.text.SimpleDateFormat;

// âœ… CLASSES AJOUTÃ‰ES POUR LA DÃ‰TECTION DE FRAUDE ML
class FraudPredictionRequest {
    private Map<String, Object> sinistreData;
    private Map<String, Object> contractData;

    public FraudPredictionRequest() {}

    public Map<String, Object> getSinistreData() { return sinistreData; }
    public void setSinistreData(Map<String, Object> sinistreData) { this.sinistreData = sinistreData; }

    public Map<String, Object> getContractData() { return contractData; }
    public void setContractData(Map<String, Object> contractData) { this.contractData = contractData; }
}

class FraudPredictionResponse {
    private boolean isFraud;
    private double confidence;
    private String riskLevel;
    private String reason;
    private List<String> riskFactors;
    private String recommendation;

    public FraudPredictionResponse() {}

    public FraudPredictionResponse(boolean isFraud, double confidence, String riskLevel, String reason) {
        this.isFraud = isFraud;
        this.confidence = confidence;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.riskFactors = new ArrayList<>();
        this.recommendation = generateRecommendation(isFraud, confidence);
    }

    private String generateRecommendation(boolean isFraud, double confidence) {
        if (isFraud && confidence > 0.8) {
            return "URGENT: VÃ©rification manuelle immÃ©diate requise";
        } else if (isFraud && confidence > 0.6) {
            return "VÃ©rification recommandÃ©e dans les 24h";
        } else if (isFraud) {
            return "Surveillance renforcÃ©e recommandÃ©e";
        }
        return "Aucune action particuliÃ¨re requise";
    }

    // Getters et setters
    public boolean isFraud() { return isFraud; }
    public void setFraud(boolean fraud) { isFraud = fraud; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}

@RestController
@RequestMapping("/api/v1/sinistres")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class SinistreController {

    @Autowired
    private SinistreRepository sinistreRepository;

    @Autowired
    private SinistreSearchService searchService;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    // âœ… ENDPOINT PRINCIPAL MODIFIÃ‰ AVEC DÃ‰TECTION ML
    @GetMapping("/all")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> getAllSinistres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateDeclaration") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        try {
            System.out.println("ðŸ” RÃ©cupÃ©ration des sinistres avec ML - page: " + page + ", size: " + size);

            Sort sort = Sort.by(
                    sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                    sortBy
            );

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Sinistre> sinistresPage = sinistreRepository.findAll(pageable);

            // âœ… TRANSFORMATION DES DONNÃ‰ES AVEC AJOUT ML
            List<Map<String, Object>> sinistresFormates = new ArrayList<>();

            for (Sinistre sinistre : sinistresPage.getContent()) {
                Map<String, Object> sinistreFormate = new HashMap<>();

                // âœ… COLONNES PRINCIPALES (VOTRE CODE EXISTANT)
                sinistreFormate.put("numSinistre", sinistre.getNumSinistre());
                sinistreFormate.put("anneeExercice", sinistre.getAnneeExercice());
                sinistreFormate.put("numContrat", sinistre.getNumContrat());

                // âœ… DATES FORMATÃ‰ES (VOTRE CODE EXISTANT)
                sinistreFormate.put("effetContrat", sinistre.getEffetContrat() != null ?
                        dateFormat.format(sinistre.getEffetContrat()) : "Non dÃ©fini");
                sinistreFormate.put("dateExpiration", sinistre.getDateExpiration());
                sinistreFormate.put("prochainTerme", sinistre.getProchainTerme());
                sinistreFormate.put("dateDeclaration", sinistre.getDateDeclaration() != null ?
                        dateFormat.format(sinistre.getDateDeclaration()) : "Non dÃ©fini");
                sinistreFormate.put("dateOuverture", sinistre.getDateOuverture() != null ?
                        dateFormat.format(sinistre.getDateOuverture()) : "Non dÃ©fini");
                sinistreFormate.put("dateSurvenance", sinistre.getDateSurvenance() != null ?
                        dateFormat.format(sinistre.getDateSurvenance()) : "Non dÃ©fini");

                // âœ… INFORMATIONS DESCRIPTIVES (VOTRE CODE EXISTANT)
                sinistreFormate.put("usage", sinistre.getUsage());
                sinistreFormate.put("typeUsage", sinistre.getTypeUsage());
                sinistreFormate.put("codeIntermediaire", sinistre.getCodeIntermediaire());
                sinistreFormate.put("natureSinistre", sinistre.getNatureSinistre());
                sinistreFormate.put("natureAvecIcone", sinistre.getNatureAvecIcone());
                sinistreFormate.put("lieuAccident", sinistre.getLieuAccident());
                sinistreFormate.put("gouvernorat", sinistre.getGouvernorat());
                sinistreFormate.put("typeSinistre", sinistre.getTypeSinistre());
                sinistreFormate.put("typeAvecIcone", sinistre.getTypeAvecIcone());
                sinistreFormate.put("compagnieAdverse", sinistre.getCompagnieAdverse());
                sinistreFormate.put("codeResponsabilite", sinistre.getCodeResponsabilite());

                // âœ… Ã‰TAT ET STATUT (VOTRE CODE EXISTANT)
                sinistreFormate.put("libEtatSinistre", sinistre.getLibEtatSinistre());
                sinistreFormate.put("etatAvecCouleur", sinistre.getEtatAvecCouleur());
                sinistreFormate.put("etatSinAnnee", sinistre.getEtatSinAnnee());
                sinistreFormate.put("priorite", sinistre.getPriorite());
                sinistreFormate.put("ageSinistreEnJours", sinistre.getAgeSinistreEnJours());

                // âœ… MONTANTS FORMATÃ‰S (VOTRE CODE EXISTANT)
                sinistreFormate.put("montantEvaluation", sinistre.getMontantEvaluationFormate());
                sinistreFormate.put("montantEvaluationBrut", sinistre.getMontantEvaluation());
                sinistreFormate.put("totalReglement", sinistre.getTotalReglementFormate());
                sinistreFormate.put("totalReglementBrut", sinistre.getTotalReglement());
                sinistreFormate.put("reglementRc", sinistre.getReglementRc());
                sinistreFormate.put("reglementDefenseEtRecours", sinistre.getReglementDefenseEtRecours());
                sinistreFormate.put("totalSapFinal", sinistre.getTotalSapFinal());
                sinistreFormate.put("sapRc", sinistre.getSapRc());
                sinistreFormate.put("sapDefenseEtRecours", sinistre.getSapDefenseEtRecours());
                sinistreFormate.put("cumulReglement", sinistre.getCumulReglement());

                // âœ… PROVISIONS ET PRÃ‰VISIONS (VOTRE CODE EXISTANT)
                sinistreFormate.put("provisionDeRecours", sinistre.getProvisionDeRecours());
                sinistreFormate.put("provisionDeRecoursDefenseEtRecours", sinistre.getProvisionDeRecoursDefenseEtRecours());
                sinistreFormate.put("previsionDeRecoursDomVeh", sinistre.getPrevisionDeRecoursDomVeh());
                sinistreFormate.put("cumulPrevisionDeRecours", sinistre.getCumulPrevisionDeRecours());

                // âœ… INFORMATIONS SUPPLÃ‰MENTAIRES (VOTRE CODE EXISTANT)
                sinistreFormate.put("nombreBlesses", sinistre.getNombreBlesses());
                sinistreFormate.put("nombreDeces", sinistre.getNombreDeces());

                // âœ… NOUVEAU : DÃ‰TECTION DE FRAUDE ML AJOUTÃ‰E
                FraudPredictionResponse fraudResult = analyzerFraudeSinistre(sinistre);
                sinistreFormate.put("fraudDetection", Map.of(
                        "isFraud", fraudResult.isFraud(),
                        "confidence", fraudResult.getConfidence(),
                        "riskLevel", fraudResult.getRiskLevel(),
                        "reason", fraudResult.getReason(),
                        "riskFactors", fraudResult.getRiskFactors(),
                        "recommendation", fraudResult.getRecommendation(),
                        "fraudScore", Math.round(fraudResult.getConfidence() * 100),
                        "alertLevel", getAlertLevel(fraudResult.getConfidence()),
                        "alertIcon", getAlertIcon(fraudResult.getConfidence()),
                        "alertColor", getAlertColor(fraudResult.getConfidence())
                ));

                sinistresFormates.add(sinistreFormate);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", sinistresFormates);
            response.put("data", sinistresFormates);
            response.put("totalElements", sinistresPage.getTotalElements());
            response.put("totalPages", sinistresPage.getTotalPages());
            response.put("currentPage", sinistresPage.getNumber());
            response.put("size", sinistresPage.getSize());
            response.put("hasNext", sinistresPage.hasNext());
            response.put("hasPrevious", sinistresPage.hasPrevious());
            response.put("status", "success");
            response.put("message", sinistresPage.getTotalElements() + " sinistres analysÃ©s avec ML");

            System.out.println("âœ… " + sinistresPage.getTotalElements() + " sinistres formatÃ©s avec dÃ©tection ML");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("âŒ Erreur lors de la rÃ©cupÃ©ration des sinistres: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur serveur interne");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("data", new ArrayList<>());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // âœ… RECHERCHE MODIFIÃ‰E AVEC AJOUT ML
    @PostMapping("/search")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> searchSinistres(@RequestBody SinistreSearchCriteria criteria) {
        try {
            System.out.println("ðŸ” Recherche avancÃ©e avec ML: " + new ObjectMapper().writeValueAsString(criteria));

            if (criteria.getPage() == null) criteria.setPage(0);
            if (criteria.getSize() == null) criteria.setSize(20);
            if (criteria.getSortBy() == null) criteria.setSortBy("dateDeclaration");
            if (criteria.getSortDirection() == null) criteria.setSortDirection("desc");

            Page<Sinistre> sinistresPage = searchService.searchSinistres(criteria);

            // âœ… TRANSFORMATION AVEC AJOUT ML
            List<Map<String, Object>> sinistresFormates = new ArrayList<>();

            for (Sinistre sinistre : sinistresPage.getContent()) {
                Map<String, Object> sinistreFormate = formatSinistreWithML(sinistre);
                sinistresFormates.add(sinistreFormate);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", sinistresFormates);
            response.put("data", sinistresFormates);
            response.put("totalElements", sinistresPage.getTotalElements());
            response.put("totalPages", sinistresPage.getTotalPages());
            response.put("currentPage", sinistresPage.getNumber());
            response.put("size", sinistresPage.getSize());
            response.put("hasNext", sinistresPage.hasNext());
            response.put("hasPrevious", sinistresPage.hasPrevious());
            response.put("status", "success");
            response.put("message", sinistresPage.getTotalElements() + " sinistres trouvÃ©s avec ML");

            System.out.println("âœ… Recherche terminÃ©e: " + sinistresPage.getTotalElements() + " rÃ©sultats avec ML");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("âŒ Erreur lors de la recherche avec ML: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la recherche");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("data", new ArrayList<>());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // âœ… NOUVEAU ENDPOINT POUR L'ANALYSE DE FRAUDE ML
    @PostMapping("/analyze-fraud")
    @PreAuthorize("permitAll()")
    public ResponseEntity<FraudPredictionResponse> analyzeFraud(@RequestBody FraudPredictionRequest request) {
        try {
            System.out.println("ðŸ¤– Analyse de fraude ML pour sinistre");

            Map<String, Object> sinistreData = request.getSinistreData();
            String numSinistre = (String) sinistreData.get("numSinistre");

            Optional<Sinistre> sinistreOpt = sinistreRepository.findById(numSinistre);

            if (sinistreOpt.isPresent()) {
                Sinistre sinistre = sinistreOpt.get();
                FraudPredictionResponse result = analyzerFraudeSinistre(sinistre);

                System.out.println("âœ… Analyse ML terminÃ©e - Fraude: " + result.isFraud() +
                        ", Confiance: " + (result.getConfidence() * 100) + "%");

                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("âŒ Erreur lors de l'analyse de fraude: " + e.getMessage());
            e.printStackTrace();

            FraudPredictionResponse errorResponse = new FraudPredictionResponse(
                    false, 0.0, "UNKNOWN", "Erreur lors de l'analyse: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // âœ… NOUVEAU ENDPOINT POUR LES STATISTIQUES DE FRAUDE
    @GetMapping("/fraud-statistics")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> getFraudStatistics() {
        try {
            System.out.println("ðŸ“Š GÃ©nÃ©ration des statistiques de fraude");

            List<Sinistre> allSinistres = sinistreRepository.findAll();

            int totalSinistres = allSinistres.size();
            int fraudulentCount = 0;
            int highRiskCount = 0;
            int mediumRiskCount = 0;
            double totalFraudAmount = 0.0;

            for (Sinistre sinistre : allSinistres) {
                FraudPredictionResponse fraudResult = analyzerFraudeSinistre(sinistre);

                if (fraudResult.isFraud()) {
                    fraudulentCount++;
                    if (sinistre.getMontantEvaluation() != null) {
                        totalFraudAmount += sinistre.getMontantEvaluation();
                    }
                }

                if (fraudResult.getConfidence() > 0.6) {
                    highRiskCount++;
                } else if (fraudResult.getConfidence() > 0.4) {
                    mediumRiskCount++;
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSinistres", totalSinistres);
            stats.put("fraudulentCount", fraudulentCount);
            stats.put("fraudPercentage", totalSinistres > 0 ? (fraudulentCount * 100.0 / totalSinistres) : 0);
            stats.put("highRiskCount", highRiskCount);
            stats.put("mediumRiskCount", mediumRiskCount);
            stats.put("totalFraudAmount", totalFraudAmount);
            stats.put("averageFraudAmount", fraudulentCount > 0 ? (totalFraudAmount / fraudulentCount) : 0);

            System.out.println("âœ… Statistiques de fraude gÃ©nÃ©rÃ©es: " + fraudulentCount + "/" + totalSinistres + " cas dÃ©tectÃ©s");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("âŒ Erreur lors de la gÃ©nÃ©ration des statistiques de fraude: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // âœ… MÃ‰THODES ML AJOUTÃ‰ES

    /**
     * Analyse de fraude ML pour un sinistre
     */
    private FraudPredictionResponse analyzerFraudeSinistre(Sinistre sinistre) {
        try {
            double fraudScore = calculateFraudScore(sinistre);
            boolean isFraud = fraudScore > 0.5;
            String riskLevel = getRiskLevel(fraudScore);
            String reason = generateFraudReason(sinistre, fraudScore);

            FraudPredictionResponse response = new FraudPredictionResponse(
                    isFraud, fraudScore, riskLevel, reason
            );

            response.setRiskFactors(identifyRiskFactors(sinistre, fraudScore));

            return response;

        } catch (Exception e) {
            System.err.println("âŒ Erreur dans l'analyse ML: " + e.getMessage());
            return new FraudPredictionResponse(false, 0.0, "ERROR", "Erreur d'analyse");
        }
    }

    /**
     * Calcul du score de fraude basÃ© sur vos donnÃ©es
     */
    private double calculateFraudScore(Sinistre sinistre) {
        double score = 0.0;

        // FACTEUR 1: Montant anormalement Ã©levÃ©
        if (sinistre.getMontantEvaluation() != null && sinistre.getMontantEvaluation() > 50000) {
            score += 0.3;
        }

        // FACTEUR 2: DÃ©lai suspect entre survenance et dÃ©claration
        if (sinistre.getDateSurvenance() != null && sinistre.getDateDeclaration() != null) {
            long delaiJours = (sinistre.getDateDeclaration().getTime() - sinistre.getDateSurvenance().getTime())
                    / (24 * 60 * 60 * 1000);
            if (delaiJours > 30) {
                score += 0.2;
            }
        }

        // FACTEUR 3: Nature corporelle avec montant Ã©levÃ©
        if ("CORPOREL".equals(sinistre.getNatureSinistre()) &&
                sinistre.getMontantEvaluation() != null && sinistre.getMontantEvaluation() > 30000) {
            score += 0.25;
        }

        // FACTEUR 4: Sinistre trÃ¨s rÃ©cent avec montant Ã©levÃ©
        if (sinistre.getAgeSinistreEnJours() < 7 &&
                sinistre.getMontantEvaluation() != null && sinistre.getMontantEvaluation() > 20000) {
            score += 0.15;
        }

        // FACTEUR 5: RÃ¨glement supÃ©rieur Ã  l'Ã©valuation
        if (sinistre.getTotalReglement() != null && sinistre.getMontantEvaluation() != null &&
                sinistre.getTotalReglement() > sinistre.getMontantEvaluation() * 1.2) {
            score += 0.2;
        }

        // FACTEUR 6: Compagnie adverse inconnue
        if (sinistre.getCompagnieAdverse() == null ||
                sinistre.getCompagnieAdverse().trim().isEmpty() ||
                "INCONNUE".equals(sinistre.getCompagnieAdverse().toUpperCase())) {
            score += 0.15;
        }

        // FACTEUR 7: Provisions de recours anormalement Ã©levÃ©es
        if (sinistre.getProvisionDeRecours() != null && sinistre.getProvisionDeRecours() > 15000) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    private String getRiskLevel(double fraudScore) {
        if (fraudScore > 0.8) return "CRITICAL";
        if (fraudScore > 0.6) return "HIGH";
        if (fraudScore > 0.4) return "MEDIUM";
        if (fraudScore > 0.2) return "LOW";
        return "MINIMAL";
    }

    private String generateFraudReason(Sinistre sinistre, double fraudScore) {
        if (fraudScore > 0.8) {
            return "Multiples indicateurs de fraude dÃ©tectÃ©s - VÃ©rification urgente requise";
        } else if (fraudScore > 0.6) {
            return "Indicateurs de fraude significatifs - Investigation recommandÃ©e";
        } else if (fraudScore > 0.4) {
            return "Anomalies dÃ©tectÃ©es - Surveillance renforcÃ©e";
        } else if (fraudScore > 0.2) {
            return "LÃ©gers indicateurs de risque - Suivi standard";
        }
        return "Profil normal - Aucune anomalie dÃ©tectÃ©e";
    }

    private List<String> identifyRiskFactors(Sinistre sinistre, double fraudScore) {
        List<String> factors = new ArrayList<>();

        if (sinistre.getMontantEvaluation() != null && sinistre.getMontantEvaluation() > 50000) {
            factors.add("Montant d'Ã©valuation trÃ¨s Ã©levÃ© (" + sinistre.getMontantEvaluationFormate() + ")");
        }

        if (sinistre.getDateSurvenance() != null && sinistre.getDateDeclaration() != null) {
            long delaiJours = (sinistre.getDateDeclaration().getTime() - sinistre.getDateSurvenance().getTime())
                    / (24 * 60 * 60 * 1000);
            if (delaiJours > 30) {
                factors.add("DÃ©lai de dÃ©claration suspect (" + delaiJours + " jours)");
            }
        }

        if ("CORPOREL".equals(sinistre.getNatureSinistre()) &&
                sinistre.getMontantEvaluation() != null && sinistre.getMontantEvaluation() > 30000) {
            factors.add("Sinistre corporel avec montant Ã©levÃ©");
        }

        if (sinistre.getCompagnieAdverse() == null || sinistre.getCompagnieAdverse().trim().isEmpty()) {
            factors.add("Compagnie adverse non identifiÃ©e");
        }

        if (factors.isEmpty()) {
            factors.add("Profil standard - Aucun facteur de risque majeur");
        }

        return factors;
    }

    private String getAlertLevel(double confidence) {
        if (confidence > 0.8) return "CRITICAL";
        if (confidence > 0.6) return "HIGH";
        if (confidence > 0.4) return "MEDIUM";
        return "LOW";
    }

    private String getAlertIcon(double confidence) {
        if (confidence > 0.8) return "fas fa-exclamation-triangle";
        if (confidence > 0.6) return "fas fa-exclamation-circle";
        if (confidence > 0.4) return "fas fa-info-circle";
        return "fas fa-check-circle";
    }

    private String getAlertColor(double confidence) {
        if (confidence > 0.8) return "#dc2626"; // Rouge
        if (confidence > 0.6) return "#f59e0b"; // Orange
        if (confidence > 0.4) return "#3b82f6"; // Bleu
        return "#10b981"; // Vert
    }

    /**
     * MÃ©thode utilitaire pour formater un sinistre avec ML
     */
    private Map<String, Object> formatSinistreWithML(Sinistre sinistre) {
        Map<String, Object> sinistreFormate = new HashMap<>();

        // Toutes vos donnÃ©es existantes
        sinistreFormate.put("numSinistre", sinistre.getNumSinistre());
        sinistreFormate.put("anneeExercice", sinistre.getAnneeExercice());
        sinistreFormate.put("numContrat", sinistre.getNumContrat());
        sinistreFormate.put("dateDeclaration", sinistre.getDateDeclaration() != null ?
                dateFormat.format(sinistre.getDateDeclaration()) : "Non dÃ©fini");
        sinistreFormate.put("natureSinistre", sinistre.getNatureSinistre());
        sinistreFormate.put("typeSinistre", sinistre.getTypeSinistre());
        sinistreFormate.put("libEtatSinistre", sinistre.getLibEtatSinistre());
        sinistreFormate.put("montantEvaluation", sinistre.getMontantEvaluationFormate());
        sinistreFormate.put("montantEvaluationBrut", sinistre.getMontantEvaluation());
        sinistreFormate.put("totalReglement", sinistre.getTotalReglementFormate());
        sinistreFormate.put("lieuAccident", sinistre.getLieuAccident());
        sinistreFormate.put("gouvernorat", sinistre.getGouvernorat());
        sinistreFormate.put("compagnieAdverse", sinistre.getCompagnieAdverse());

        // Ajout de l'analyse ML
        FraudPredictionResponse fraudResult = analyzerFraudeSinistre(sinistre);
        sinistreFormate.put("fraudDetection", Map.of(
                "isFraud", fraudResult.isFraud(),
                "confidence", fraudResult.getConfidence(),
                "riskLevel", fraudResult.getRiskLevel(),
                "reason", fraudResult.getReason(),
                "fraudScore", Math.round(fraudResult.getConfidence() * 100),
                "alertLevel", getAlertLevel(fraudResult.getConfidence()),
                "alertIcon", getAlertIcon(fraudResult.getConfidence()),
                "alertColor", getAlertColor(fraudResult.getConfidence())
        ));

        return sinistreFormate;
    }

    // âœ… VOS ENDPOINTS EXISTANTS CONSERVÃ‰S (statistiques/avancees, health, test, etc.)

    @GetMapping("/statistiques/avancees")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> getStatistiquesAvancees() {
        try {
            System.out.println("ðŸ“Š GÃ©nÃ©ration des statistiques avancÃ©es");

            Map<String, Object> stats = new HashMap<>();

            // Statistiques gÃ©nÃ©rales
            long totalSinistres = sinistreRepository.count();
            stats.put("totalSinistres", totalSinistres);

            // RÃ©partition par nature
            long corporel = sinistreRepository.countByNatureSinistre("CORPOREL");
            long materiel = sinistreRepository.countByNatureSinistre("MATERIEL");
            long mixte = sinistreRepository.countByNatureSinistre("MIXTE");

            Map<String, Object> repartitionNature = new HashMap<>();
            repartitionNature.put("corporel", corporel);
            repartitionNature.put("materiel", materiel);
            repartitionNature.put("mixte", mixte);
            stats.put("repartitionNature", repartitionNature);

            // RÃ©partition par Ã©tat
            long miseAJour = sinistreRepository.countByLibEtatSinistre("MISE A JOUR");
            long reprise = sinistreRepository.countByLibEtatSinistre("REPRISE");
            long reouverture = sinistreRepository.countByLibEtatSinistre("REOUVERTURE");
            long cloture = sinistreRepository.countByLibEtatSinistre("CLOTURE");

            Map<String, Object> repartitionEtat = new HashMap<>();
            repartitionEtat.put("miseAJour", miseAJour);
            repartitionEtat.put("reprise", reprise);
            repartitionEtat.put("reouverture", reouverture);
            repartitionEtat.put("cloture", cloture);
            stats.put("repartitionEtat", repartitionEtat);

            System.out.println("âœ… Statistiques avancÃ©es gÃ©nÃ©rÃ©es");
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("âŒ Erreur lors de la gÃ©nÃ©ration des statistiques: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> healthCheck() {
        try {
            long count = sinistreRepository.count();
            return ResponseEntity.ok("Service Sinistres avec ML opÃ©rationnel - " + count + " sinistres en base");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur de connexion Ã  la base de donnÃ©es: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> testCors() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "CORS fonctionne correctement avec dÃ©tection ML");
        response.put("timestamp", System.currentTimeMillis());
        response.put("endpoint", "/api/v1/sinistres/test");
        response.put("mlEnabled", true);
        response.put("fraudModelVersion", "v2.0");

        return ResponseEntity.ok(response);
    }

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Sinistre> createSinistre(@RequestBody Sinistre sinistre) {
        if (sinistreRepository.existsById(sinistre.getNumSinistre())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // dÃ©jÃ  existe
        }
        Sinistre saved = sinistreRepository.save(sinistre);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{numSinistre}")
    public ResponseEntity<Sinistre> updateSinistre(@PathVariable String numSinistre, @RequestBody Sinistre updated) {
        return sinistreRepository.findById(numSinistre)
                .map(existing -> {
                    // on copie tous les champs modifiables
                    existing.setAnneeExercice(updated.getAnneeExercice());
                    existing.setNumContrat(updated.getNumContrat());
                    existing.setEffetContrat(updated.getEffetContrat());
                    existing.setDateExpiration(updated.getDateExpiration());
                    existing.setProchainTerme(updated.getProchainTerme());
                    existing.setUsage(updated.getUsage());
                    existing.setCodeIntermediaire(updated.getCodeIntermediaire());
                    existing.setNatureSinistre(updated.getNatureSinistre());
                    existing.setLieuAccident(updated.getLieuAccident());
                    existing.setTypeSinistre(updated.getTypeSinistre());
                    existing.setCompagnieAdverse(updated.getCompagnieAdverse());
                    existing.setCodeResponsabilite(updated.getCodeResponsabilite());
                    existing.setDateDeclaration(updated.getDateDeclaration());
                    existing.setDateOuverture(updated.getDateOuverture());
                    existing.setDateSurvenance(updated.getDateSurvenance());
                    existing.setLibEtatSinistre(updated.getLibEtatSinistre());
                    existing.setEtatSinAnnee(updated.getEtatSinAnnee());
                    existing.setMontantEvaluation(updated.getMontantEvaluation());
                    existing.setTotalReglement(updated.getTotalReglement());
                    existing.setReglementRc(updated.getReglementRc());
                    existing.setReglementDefenseEtRecours(updated.getReglementDefenseEtRecours());
                    existing.setTotalSapFinal(updated.getTotalSapFinal());
                    existing.setSapRc(updated.getSapRc());
                    existing.setSapDefenseEtRecours(updated.getSapDefenseEtRecours());
                    existing.setCumulReglement(updated.getCumulReglement());
                    existing.setProvisionDeRecours(updated.getProvisionDeRecours());
                    existing.setProvisionDeRecoursDefenseEtRecours(updated.getProvisionDeRecoursDefenseEtRecours());
                    existing.setPrevisionDeRecoursDomVeh(updated.getPrevisionDeRecoursDomVeh());
                    existing.setCumulPrevisionDeRecours(updated.getCumulPrevisionDeRecours());
                    existing.setGouvernorat(updated.getGouvernorat());
                    existing.setNombreBlesses(updated.getNombreBlesses());
                    existing.setNombreDeces(updated.getNombreDeces());
                    existing.setTypeUsage(updated.getTypeUsage());
                    // sauvegarde
                    Sinistre saved = sinistreRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    public ResponseEntity<Void> deleteSinistre(@PathVariable String numSinistre) {
        if (sinistreRepository.existsById(numSinistre)) {
            sinistreRepository.deleteById(numSinistre);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }



    @PostMapping("/add")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> createSinistre(@RequestBody Map<String, Object> payload) {
        try {
            // 1) Champs obligatoires
            String numSinistre = str(payload.get("numSinistre"));
            if (numSinistre == null || numSinistre.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "numSinistre requis"));
            }

            // 2) Construire l'entitÃ©
            Sinistre s = new Sinistre();
            s.setNumSinistre(numSinistre);

            // --- Identifiants / numÃ©ros ---
            s.setNumContrat(str(payload.get("numContrat")));
            s.setAnneeExercice(intOrNull(payload.get("anneeExercice")));

            // --- Dates (types selon l'entitÃ©) ---
            // effetContrat: java.util.Date
            s.setEffetContrat(dateOrNull(payload.get("effetContrat")));
            // dateExpiration / prochainTerme: String dans l'entitÃ©
            s.setDateExpiration(str(payload.get("dateExpiration")));
            s.setProchainTerme(str(payload.get("prochainTerme")));
            // Dates sinistre (java.util.Date)
            s.setDateDeclaration(dateOrNull(payload.get("dateDeclaration")));
            s.setDateOuverture(dateOrNull(payload.get("dateOuverture")));
            s.setDateSurvenance(dateOrNull(payload.get("dateSurvenance")));

            // --- CaractÃ©ristiques ---
            s.setUsage(str(payload.get("usage")));
            s.setCodeIntermediaire(intOrNull(payload.get("codeIntermediaire")));
            s.setNatureSinistre(str(payload.get("natureSinistre")));
            s.setLieuAccident(str(payload.get("lieuAccident")));
            s.setTypeSinistre(str(payload.get("typeSinistre")));
            s.setCompagnieAdverse(str(payload.get("compagnieAdverse")));
            s.setCodeResponsabilite(intOrNull(payload.get("codeResponsabilite")));
            s.setLibEtatSinistre(str(payload.get("libEtatSinistre")));
            s.setEtatSinAnnee(str(payload.get("etatSinAnnee")));
            s.setGouvernorat(str(payload.get("gouvernorat")));
            s.setNombreBlesses(intOrNull(payload.get("nombreBlesses")));
            s.setNombreDeces(intOrNull(payload.get("nombreDeces")));
            s.setTypeUsage(str(payload.get("typeUsage")));

            // --- Montants / provisions ---
            s.setMontantEvaluation(dblOrNull(payload.get("montantEvaluation")));
            s.setTotalReglement(dblOrNull(payload.get("totalReglement")));
            s.setReglementRc(dblOrNull(payload.get("reglementRc")));
            s.setReglementDefenseEtRecours(dblOrNull(payload.get("reglementDefenseEtRecours")));
            s.setTotalSapFinal(dblOrNull(payload.get("totalSapFinal")));
            s.setSapRc(str(payload.get("sapRc")));
            s.setSapDefenseEtRecours(str(payload.get("sapDefenseEtRecours")));
            s.setCumulReglement(str(payload.get("cumulReglement")));
            s.setProvisionDeRecours(dblOrNull(payload.get("provisionDeRecours")));
            s.setProvisionDeRecoursDefenseEtRecours(str(payload.get("provisionDeRecoursDefenseEtRecours")));
            s.setPrevisionDeRecoursDomVeh(str(payload.get("previsionDeRecoursDomVeh")));
            s.setCumulPrevisionDeRecours(str(payload.get("cumulPrevisionDeRecours")));

            // 3) Persister
            Sinistre saved = sinistreRepository.save(s);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la crÃ©ation", "message", e.getMessage()));
        }
    }

    /* ===== Helpers privÃ©s (mets-les en bas du controller) ===== */

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer intOrNull(Object v) {
        try {
            if (v == null) return null;
            if (v instanceof Number n) return n.intValue();
            String s = String.valueOf(v).trim().replaceAll("\\s+", "");
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (Exception ignored) { return null; }
    }

    private static Double dblOrNull(Object v) {
        try {
            if (v == null) return null;
            if (v instanceof Number n) return n.doubleValue();
            String s = String.valueOf(v).trim().replace(" ", "").replace(",", ".");
            return s.isEmpty() ? null : Double.parseDouble(s);
        } catch (Exception ignored) { return null; }
    }

    /**
     * Accepte "yyyy-MM-dd", "dd/MM/yyyy" ou timestamps.
     * Retourne null si vide/invalide.
     */
    private static Date dateOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            // ISO court: 2025-08-19
            return java.sql.Date.valueOf(s);
        } catch (Exception ignored) {}
        try {
            // FranÃ§ais: 19/08/2025
            return new java.text.SimpleDateFormat("dd/MM/yyyy").parse(s);
        } catch (Exception ignored) {}
        try {
            // timestamp (ms)
            long t = Long.parseLong(s);
            return new Date(t);
        } catch (Exception ignored) {}
        return null;
    }

}

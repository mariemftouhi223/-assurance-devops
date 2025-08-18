package com.mariem.assurance.assures;

import com.mariem.assurance.services.IAService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/assures")
@CrossOrigin(
        origins = { "http://localhost:4200" },
        methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
        allowedHeaders = { "Authorization", "Content-Type", "Accept" },
        allowCredentials = "true",
        maxAge = 3600
)
public class AssureController {

    @Autowired private IAService iaService; // si utilisé ailleurs
    @Autowired private AssureRepository assureRepository;
    @Autowired private EntityManager entityManager;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- TEST / SANITY ---
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Controller fonctionne correctement");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // --- LISTE PAGINÉE ---
    @GetMapping
    public ResponseEntity<?> getAssuresPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "numContrat") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Page<Assure> assuresPage = assureRepository.findAll(PageRequest.of(page, size, sort));

            Map<String, Object> response = new HashMap<>();
            response.put("content", assuresPage.getContent());
            response.put("totalElements", assuresPage.getTotalElements());
            response.put("totalPages", assuresPage.getTotalPages());
            response.put("number", assuresPage.getNumber());
            response.put("size", assuresPage.getSize());
            response.put("first", assuresPage.isFirst());
            response.put("last", assuresPage.isLast());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des assurés", "message", e.getMessage()));
        }
    }

    // --- CRÉATION ---
    @PostMapping("/add")
    public ResponseEntity<?> createAssure(@Valid @RequestBody Assure assure, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Validation failed",
                        "details", getValidationErrors(bindingResult)
                ));
            }

            if (assure.getNumContrat() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "numContrat requis"));
            }
            if (assureRepository.existsByNumContrat(assure.getNumContrat())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Le numéro de contrat existe déjà",
                        "numContrat", assure.getNumContrat()
                ));
            }

            // Valeurs par défaut : en String (yyyy-MM-dd)
            if (isBlank(assure.getEffetContrat())) {
                assure.setEffetContrat(LocalDate.now().format(ISO));
            }
            if (isBlank(assure.getValiditeDu())) {
                assure.setValiditeDu(LocalDate.now().format(ISO));
            }
            if (isBlank(assure.getValiditeAu())) {
                assure.setValiditeAu(LocalDate.now().plusYears(1).format(ISO));
            }
            if (isBlank(assure.getImmatriculationVehicule())) {
                assure.setImmatriculationVehicule("TUN-" + (System.currentTimeMillis() % 100000));
            }
            if (isBlank(assure.getMarqueVehicule())) {
                assure.setMarqueVehicule("Non spécifiée");
            }
            if (assure.getTotalPrimeNette() == null) {
                assure.setTotalPrimeNette(100.0);
            }

            Assure saved = assureRepository.save(assure);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Assuré créé avec succès",
                    "assure", saved,
                    "numContrat", saved.getNumContrat()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création de l'assuré", "message", e.getMessage()));
        }
    }

    // --- LECTURE PAR numContrat ---
    @GetMapping("/{numContrat}")
    public ResponseEntity<?> getAssureByNumContrat(@PathVariable Long numContrat) {
        try {
            return assureRepository.findByNumContrat(numContrat)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                            "error", "Assuré non trouvé",
                            "numContrat", numContrat
                    )));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération de l'assuré", "message", e.getMessage()));
        }
    }

    // --- MISE À JOUR (en-place, sans getId/setId) ---
    @PutMapping("/update/{numContrat}")
    public ResponseEntity<?> updateAssure(
            @PathVariable Long numContrat,
            @Valid @RequestBody Assure request,
            BindingResult bindingResult
    ) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Validation failed",
                        "details", getValidationErrors(bindingResult)
                ));
            }

            Optional<Assure> existingOpt = assureRepository.findByNumContrat(numContrat);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Assuré non trouvé",
                        "numContrat", numContrat
                ));
            }

            Assure existing = existingOpt.get();

            // On n'autorise PAS la modification du numContrat (clé fonctionnelle)
            // Copie "en-place" des champs autorisés si présents dans la requête
            if (!isBlank(request.getEffetContrat()))          existing.setEffetContrat(request.getEffetContrat());
            if (!isBlank(request.getValiditeDu()))            existing.setValiditeDu(request.getValiditeDu());
            if (!isBlank(request.getValiditeAu()))            existing.setValiditeAu(request.getValiditeAu());
            if (!isBlank(request.getImmatriculationVehicule())) existing.setImmatriculationVehicule(request.getImmatriculationVehicule());
            if (!isBlank(request.getMarqueVehicule()))        existing.setMarqueVehicule(request.getMarqueVehicule());
            if (request.getTotalPrimeNette() != null)         existing.setTotalPrimeNette(request.getTotalPrimeNette());

            // … ajoute ici les autres champs modifiables selon ton entité (ville, usage, etc.)
            // Exemple :
            if (!isBlank(request.getVille()))                 existing.setVille(request.getVille());

            Assure updated = assureRepository.save(existing);

            return ResponseEntity.ok(Map.of(
                    "message", "Assuré modifié avec succès",
                    "assure", updated,
                    "numContrat", updated.getNumContrat()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la modification de l'assuré", "message", e.getMessage()));
        }
    }

    // --- SUPPRESSION ---
    @DeleteMapping("/delete/{numContrat}")
    public ResponseEntity<?> deleteAssure(@PathVariable Long numContrat) {
        try {
            Optional<Assure> existing = assureRepository.findByNumContrat(numContrat);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "Assuré non trouvé",
                        "numContrat", numContrat
                ));
            }

            assureRepository.deleteByNumContrat(numContrat);

            return ResponseEntity.ok(Map.of(
                    "message", "Assuré supprimé avec succès",
                    "numContrat", numContrat
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression de l'assuré", "message", e.getMessage()));
        }
    }

    // --- RECHERCHE (JPQL dynamique simple) ---
    @GetMapping("/search")
    public ResponseEntity<?> searchAssures(
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
        try {
            StringBuilder jpql = new StringBuilder("SELECT a FROM Assure a WHERE 1=1");
            Map<String, Object> params = new HashMap<>();

            if (numContrat != null) {
                jpql.append(" AND a.numContrat = :numContrat");
                params.put("numContrat", numContrat);
            }
            if (annee != null) {
                // si ton champ est "anneeExercice", remplace ci-dessous :
                jpql.append(" AND a.annee = :annee");
                params.put("annee", annee);
            }
            if (!isBlank(ville)) {
                jpql.append(" AND LOWER(a.ville) LIKE LOWER(:ville)");
                params.put("ville", "%" + ville + "%");
            }
            if (!isBlank(marqueVehicule)) {
                jpql.append(" AND LOWER(a.marqueVehicule) LIKE LOWER(:marqueVehicule)");
                params.put("marqueVehicule", "%" + marqueVehicule + "%");
            }
            if (!isBlank(immatriculationVehicule)) {
                jpql.append(" AND LOWER(a.immatriculationVehicule) LIKE LOWER(:immatriculationVehicule)");
                params.put("immatriculationVehicule", "%" + immatriculationVehicule + "%");
            }

            jpql.append(" ORDER BY a.").append(sortBy).append(" ")
                    .append("desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC");

            Query dataQ = entityManager.createQuery(jpql.toString());
            params.forEach(dataQ::setParameter);
            dataQ.setFirstResult(page * size);
            dataQ.setMaxResults(size);

            @SuppressWarnings("unchecked")
            List<Assure> content = dataQ.getResultList();

            String countJpql = jpql.toString()
                    .replaceFirst("SELECT a FROM Assure a", "SELECT COUNT(a) FROM Assure a")
                    .replaceAll("ORDER BY\\s+.*$", "");
            Query countQ = entityManager.createQuery(countJpql);
            params.forEach(countQ::setParameter);
            long totalElements = (Long) countQ.getSingleResult();

            Map<String, Object> response = new HashMap<>();
            response.put("content", content);
            response.put("totalElements", totalElements);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("number", page);
            response.put("size", size);
            response.put("first", page == 0);
            response.put("last", page >= Math.ceil((double) totalElements / size) - 1);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la recherche", "message", e.getMessage()));
        }
    }

    // --- HEALTH ---
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        try {
            long count = assureRepository.count();
            health.put("status", "UP");
            health.put("database", "Connected");
            health.put("totalAssures", count);
            health.put("timestamp", new Date());
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("database", "Disconnected");
            health.put("error", e.getMessage());
            health.put("timestamp", new Date());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    // --- Utils ---
    private List<String> getValidationErrors(BindingResult bindingResult) {
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(err -> errors.add(err.getField() + ": " + err.getDefaultMessage()));
        bindingResult.getGlobalErrors().forEach(err -> errors.add(err.getDefaultMessage()));
        return errors;
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}

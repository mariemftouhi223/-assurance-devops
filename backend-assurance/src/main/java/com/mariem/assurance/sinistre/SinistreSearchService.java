package com.mariem.assurance.sinistre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SinistreSearchService {

    @Autowired
    private SinistreRepository sinistreRepository;

    public Page<Sinistre> searchSinistres(SinistreSearchCriteria criteria) {
        try {
            System.out.println("🔍 Recherche avec critères: " + criteria);

            Specification<Sinistre> spec = createSpecification(criteria);
            Pageable pageable = createPageable(criteria);

            Page<Sinistre> results = sinistreRepository.findAll(spec, pageable);

            System.out.println("✅ " + results.getTotalElements() + " sinistres trouvés");

            return results;
        } catch (Exception e) {
            System.err.println("❌ Erreur dans la recherche: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private Specification<Sinistre> createSpecification(SinistreSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            try {
                // Critères de texte exact
                if (StringUtils.hasText(criteria.getNumContrat())) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("numContrat")),
                            "%" + criteria.getNumContrat().toLowerCase() + "%"
                    ));
                }

                if (criteria.getAnneeExercice() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("anneeExercice"), criteria.getAnneeExercice()));
                }

                if (StringUtils.hasText(criteria.getNatureSinistre())) {
                    predicates.add(criteriaBuilder.equal(root.get("natureSinistre"), criteria.getNatureSinistre()));
                }

                if (StringUtils.hasText(criteria.getTypeSinistre())) {
                    predicates.add(criteriaBuilder.equal(root.get("typeSinistre"), criteria.getTypeSinistre()));
                }

                if (StringUtils.hasText(criteria.getLibEtatSinistre())) {
                    predicates.add(criteriaBuilder.equal(root.get("libEtatSinistre"), criteria.getLibEtatSinistre()));
                }

                if (StringUtils.hasText(criteria.getGouvernorat())) {
                    predicates.add(criteriaBuilder.equal(root.get("gouvernorat"), criteria.getGouvernorat()));
                }

                if (criteria.getCodeIntermediaire() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("codeIntermediaire"), criteria.getCodeIntermediaire()));
                }

                if (StringUtils.hasText(criteria.getLieuAccident())) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("lieuAccident")),
                            "%" + criteria.getLieuAccident().toLowerCase() + "%"
                    ));
                }

                if (StringUtils.hasText(criteria.getCompagnieAdverse())) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("compagnieAdverse")),
                            "%" + criteria.getCompagnieAdverse().toLowerCase() + "%"
                    ));
                }

                if (criteria.getCodeResponsabilite() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("codeResponsabilite"), criteria.getCodeResponsabilite()));
                }

                // Critères de dates - Date de déclaration
                if (criteria.getDateDeclarationDebut() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateDeclaration"), criteria.getDateDeclarationDebut()));
                }
                if (criteria.getDateDeclarationFin() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateDeclaration"), criteria.getDateDeclarationFin()));
                }

                // Critères de dates - Date de survenance
                if (criteria.getDateSurvenanceDebut() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateSurvenance"), criteria.getDateSurvenanceDebut()));
                }
                if (criteria.getDateSurvenanceFin() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateSurvenance"), criteria.getDateSurvenanceFin()));
                }

                // Critères de dates - Date d'ouverture
                if (criteria.getDateOuvertureDebut() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateOuverture"), criteria.getDateOuvertureDebut()));
                }
                if (criteria.getDateOuvertureFin() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateOuverture"), criteria.getDateOuvertureFin()));
                }

                // Critères de montants - Montant d'évaluation
                if (criteria.getMontantEvaluationMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("montantEvaluation"), criteria.getMontantEvaluationMin()));
                }
                if (criteria.getMontantEvaluationMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("montantEvaluation"), criteria.getMontantEvaluationMax()));
                }

                // Critères de montants - Total règlement
                if (criteria.getTotalReglementMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalReglement"), criteria.getTotalReglementMin()));
                }
                if (criteria.getTotalReglementMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalReglement"), criteria.getTotalReglementMax()));
                }

                // Critères numériques - Nombre de blessés
                if (criteria.getNombreBlessesMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("nombreBlesses"), criteria.getNombreBlessesMin()));
                }
                if (criteria.getNombreBlessesMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("nombreBlesses"), criteria.getNombreBlessesMax()));
                }

                // Critères numériques - Nombre de décès
                if (criteria.getNombreDecesMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("nombreDeces"), criteria.getNombreDecesMin()));
                }
                if (criteria.getNombreDecesMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("nombreDeces"), criteria.getNombreDecesMax()));
                }

                // Recherche textuelle globale avec gestion des valeurs nulles
                if (StringUtils.hasText(criteria.getSearchText())) {
                    String searchPattern = "%" + criteria.getSearchText().toLowerCase() + "%";

                    List<Predicate> textPredicates = new ArrayList<>();

                    // Recherche dans les champs texte avec vérification de nullité
                    textPredicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numSinistre")), searchPattern));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("numContrat"), "")),
                            searchPattern
                    ));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("natureSinistre"), "")),
                            searchPattern
                    ));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("typeSinistre"), "")),
                            searchPattern
                    ));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("lieuAccident"), "")),
                            searchPattern
                    ));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("gouvernorat"), "")),
                            searchPattern
                    ));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("compagnieAdverse"), "")),
                            searchPattern
                    ));

                    textPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("libEtatSinistre"), "")),
                            searchPattern
                    ));

                    Predicate textSearch = criteriaBuilder.or(textPredicates.toArray(new Predicate[0]));
                    predicates.add(textSearch);
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur dans la création des prédicats: " + e.getMessage());
                e.printStackTrace();
            }

            return predicates.isEmpty() ?
                    criteriaBuilder.conjunction() :
                    criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable createPageable(SinistreSearchCriteria criteria) {
        try {
            // Valeurs par défaut sécurisées
            String sortBy = (criteria.getSortBy() != null && !criteria.getSortBy().trim().isEmpty())
                    ? criteria.getSortBy()
                    : "dateDeclaration";

            String sortDirection = (criteria.getSortDirection() != null && !criteria.getSortDirection().trim().isEmpty())
                    ? criteria.getSortDirection()
                    : "desc";

            int page = (criteria.getPage() != null && criteria.getPage() >= 0)
                    ? criteria.getPage()
                    : 0;

            int size = (criteria.getSize() != null && criteria.getSize() > 0 && criteria.getSize() <= 100)
                    ? criteria.getSize()
                    : 20;

            Sort sort = Sort.by(
                    sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                    sortBy
            );

            System.out.println("📄 Pagination: page=" + page + ", size=" + size + ", sort=" + sortBy + " " + sortDirection);

            return PageRequest.of(page, size, sort);

        } catch (Exception e) {
            System.err.println("❌ Erreur dans la création de la pagination: " + e.getMessage());
            e.printStackTrace();

            // Pagination par défaut en cas d'erreur
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dateDeclaration"));
        }
    }

    // ✅ MÉTHODE UTILITAIRE: Recherche simple par texte
    public Page<Sinistre> searchByText(String searchText, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setSearchText(searchText);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }

    // ✅ MÉTHODE UTILITAIRE: Recherche par nature
    public Page<Sinistre> searchByNature(String natureSinistre, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setNatureSinistre(natureSinistre);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }

    // ✅ MÉTHODE UTILITAIRE: Recherche par état
    public Page<Sinistre> searchByEtat(String libEtatSinistre, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setLibEtatSinistre(libEtatSinistre);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }

    // ✅ MÉTHODE UTILITAIRE: Recherche par année
    public Page<Sinistre> searchByAnnee(Integer anneeExercice, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setAnneeExercice(anneeExercice);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }
}


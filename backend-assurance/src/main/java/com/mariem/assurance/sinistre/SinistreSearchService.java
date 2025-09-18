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
            System.out.println("ðŸ” Recherche avec critÃ¨res: " + criteria);

            Specification<Sinistre> spec = createSpecification(criteria);
            Pageable pageable = createPageable(criteria);

            Page<Sinistre> results = sinistreRepository.findAll(spec, pageable);

            System.out.println("âœ… " + results.getTotalElements() + " sinistres trouvÃ©s");

            return results;
        } catch (Exception e) {
            System.err.println("âŒ Erreur dans la recherche: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private Specification<Sinistre> createSpecification(SinistreSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            try {
                // CritÃ¨res de texte exact
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

                // CritÃ¨res de dates - Date de dÃ©claration
                if (criteria.getDateDeclarationDebut() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateDeclaration"), criteria.getDateDeclarationDebut()));
                }
                if (criteria.getDateDeclarationFin() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateDeclaration"), criteria.getDateDeclarationFin()));
                }

                // CritÃ¨res de dates - Date de survenance
                if (criteria.getDateSurvenanceDebut() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateSurvenance"), criteria.getDateSurvenanceDebut()));
                }
                if (criteria.getDateSurvenanceFin() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateSurvenance"), criteria.getDateSurvenanceFin()));
                }

                // CritÃ¨res de dates - Date d'ouverture
                if (criteria.getDateOuvertureDebut() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateOuverture"), criteria.getDateOuvertureDebut()));
                }
                if (criteria.getDateOuvertureFin() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateOuverture"), criteria.getDateOuvertureFin()));
                }

                // CritÃ¨res de montants - Montant d'Ã©valuation
                if (criteria.getMontantEvaluationMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("montantEvaluation"), criteria.getMontantEvaluationMin()));
                }
                if (criteria.getMontantEvaluationMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("montantEvaluation"), criteria.getMontantEvaluationMax()));
                }

                // CritÃ¨res de montants - Total rÃ¨glement
                if (criteria.getTotalReglementMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalReglement"), criteria.getTotalReglementMin()));
                }
                if (criteria.getTotalReglementMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalReglement"), criteria.getTotalReglementMax()));
                }

                // CritÃ¨res numÃ©riques - Nombre de blessÃ©s
                if (criteria.getNombreBlessesMin() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("nombreBlesses"), criteria.getNombreBlessesMin()));
                }
                if (criteria.getNombreBlessesMax() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("nombreBlesses"), criteria.getNombreBlessesMax()));
                }

                // CritÃ¨res numÃ©riques - Nombre de dÃ©cÃ¨s
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

                    // Recherche dans les champs texte avec vÃ©rification de nullitÃ©
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
                System.err.println("âŒ Erreur dans la crÃ©ation des prÃ©dicats: " + e.getMessage());
                e.printStackTrace();
            }

            return predicates.isEmpty() ?
                    criteriaBuilder.conjunction() :
                    criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable createPageable(SinistreSearchCriteria criteria) {
        try {
            // Valeurs par dÃ©faut sÃ©curisÃ©es
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

            System.out.println("ðŸ“„ Pagination: page=" + page + ", size=" + size + ", sort=" + sortBy + " " + sortDirection);

            return PageRequest.of(page, size, sort);

        } catch (Exception e) {
            System.err.println("âŒ Erreur dans la crÃ©ation de la pagination: " + e.getMessage());
            e.printStackTrace();

            // Pagination par dÃ©faut en cas d'erreur
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dateDeclaration"));
        }
    }

    // âœ… MÃ‰THODE UTILITAIRE: Recherche simple par texte
    public Page<Sinistre> searchByText(String searchText, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setSearchText(searchText);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }

    // âœ… MÃ‰THODE UTILITAIRE: Recherche par nature
    public Page<Sinistre> searchByNature(String natureSinistre, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setNatureSinistre(natureSinistre);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }

    // âœ… MÃ‰THODE UTILITAIRE: Recherche par Ã©tat
    public Page<Sinistre> searchByEtat(String libEtatSinistre, int page, int size) {
        SinistreSearchCriteria criteria = new SinistreSearchCriteria();
        criteria.setLibEtatSinistre(libEtatSinistre);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy("dateDeclaration");
        criteria.setSortDirection("desc");

        return searchSinistres(criteria);
    }

    // âœ… MÃ‰THODE UTILITAIRE: Recherche par annÃ©e
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


package com.mariem.assurance.sinistre;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Date;
import java.util.List;

@Repository
public interface SinistreRepository extends JpaRepository<Sinistre, String>,JpaSpecificationExecutor<Sinistre> {

    // ✅ REQUÊTES OPTIMISÉES POUR LES COLONNES IMPORTANTES

    /**
     * Recherche par nature de sinistre
     */
    long countByNatureSinistre(String natureSinistre);
    Page<Sinistre> findByNatureSinistreContainingIgnoreCase(String natureSinistre, Pageable pageable);

    /**
     * Recherche par état de sinistre
     */
    long countByLibEtatSinistre(String libEtatSinistre);
    Page<Sinistre> findByLibEtatSinistreContainingIgnoreCase(String libEtatSinistre, Pageable pageable);

    /**
     * Recherche par type de sinistre
     */
    long countByTypeSinistre(String typeSinistre);
    Page<Sinistre> findByTypeSinistreContainingIgnoreCase(String typeSinistre, Pageable pageable);

    /**
     * Recherche par année d'exercice
     */
    Page<Sinistre> findByAnneeExercice(Integer anneeExercice, Pageable pageable);

    @Query("SELECT s.anneeExercice, COUNT(s) FROM Sinistre s GROUP BY s.anneeExercice ORDER BY s.anneeExercice DESC")
    List<Object[]> countByAnneeExercice();

    /**
     * Recherche par numéro de contrat
     */
    Page<Sinistre> findByNumContratContainingIgnoreCase(String numContrat, Pageable pageable);

    /**
     * Recherche par gouvernorat
     */
    Page<Sinistre> findByGouvernoratContainingIgnoreCase(String gouvernorat, Pageable pageable);

    /**
     * Recherche par lieu d'accident
     */
    Page<Sinistre> findByLieuAccidentContainingIgnoreCase(String lieuAccident, Pageable pageable);

    /**
     * Recherche par compagnie adverse
     */
    Page<Sinistre> findByCompagnieAdverseContainingIgnoreCase(String compagnieAdverse, Pageable pageable);

    /**
     * Recherche par usage
     */
    Page<Sinistre> findByUsageContainingIgnoreCase(String usage, Pageable pageable);

    /**
     * Recherche par type d'usage
     */
    Page<Sinistre> findByTypeUsageContainingIgnoreCase(String typeUsage, Pageable pageable);

    // ✅ REQUÊTES AVANCÉES POUR LES MONTANTS

    /**
     * Sinistres avec montant d'évaluation supérieur à un seuil
     */
    @Query("SELECT s FROM Sinistre s WHERE s.montantEvaluation > :montant ORDER BY s.montantEvaluation DESC")
    Page<Sinistre> findByMontantEvaluationGreaterThan(@Param("montant") Double montant, Pageable pageable);

    /**
     * Sinistres avec total règlement supérieur à un seuil
     */
    @Query("SELECT s FROM Sinistre s WHERE s.totalReglement > :montant ORDER BY s.totalReglement DESC")
    Page<Sinistre> findByTotalReglementGreaterThan(@Param("montant") Double montant, Pageable pageable);

    /**
     * Statistiques des montants par nature de sinistre
     */
    @Query("SELECT s.natureSinistre, COUNT(s), SUM(s.montantEvaluation), AVG(s.montantEvaluation), MAX(s.montantEvaluation) " +
            "FROM Sinistre s WHERE s.montantEvaluation IS NOT NULL " +
            "GROUP BY s.natureSinistre ORDER BY SUM(s.montantEvaluation) DESC")
    List<Object[]> getStatistiquesMontantsParNature();

    /**
     * Statistiques des montants par type de sinistre
     */
    @Query("SELECT s.typeSinistre, COUNT(s), SUM(s.totalReglement), AVG(s.totalReglement) " +
            "FROM Sinistre s WHERE s.totalReglement IS NOT NULL " +
            "GROUP BY s.typeSinistre ORDER BY SUM(s.totalReglement) DESC")
    List<Object[]> getStatistiquesMontantsParType();

    // ✅ REQUÊTES TEMPORELLES POUR LES DATES IMPORTANTES

    /**
     * Sinistres déclarés entre deux dates
     */
    @Query("SELECT s FROM Sinistre s WHERE s.dateDeclaration BETWEEN :dateDebut AND :dateFin ORDER BY s.dateDeclaration DESC")
    Page<Sinistre> findByDateDeclarationBetween(@Param("dateDebut") Date dateDebut, @Param("dateFin") Date dateFin, Pageable pageable);

    /**
     * Sinistres survenus entre deux dates
     */
    @Query("SELECT s FROM Sinistre s WHERE s.dateSurvenance BETWEEN :dateDebut AND :dateFin ORDER BY s.dateSurvenance DESC")
    Page<Sinistre> findByDateSurvenanceBetween(@Param("dateDebut") Date dateDebut, @Param("dateFin") Date dateFin, Pageable pageable);

    /**
     * Sinistres ouverts entre deux dates
     */
    @Query("SELECT s FROM Sinistre s WHERE s.dateOuverture BETWEEN :dateDebut AND :dateFin ORDER BY s.dateOuverture DESC")
    Page<Sinistre> findByDateOuvertureBetween(@Param("dateDebut") Date dateDebut, @Param("dateFin") Date dateFin, Pageable pageable);

    /**
     * Sinistres anciens (plus de X jours)
     */
    @Query("SELECT s FROM Sinistre s WHERE s.dateDeclaration < :dateLimit ORDER BY s.dateDeclaration ASC")
    Page<Sinistre> findSinistresAnciens(@Param("dateLimit") Date dateLimit, Pageable pageable);

    // ✅ REQUÊTES DE RECHERCHE GLOBALE

    /**
     * Recherche globale dans les colonnes importantes
     */
    @Query("SELECT s FROM Sinistre s WHERE " +
            "LOWER(s.numSinistre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.numContrat) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.natureSinistre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.typeSinistre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.libEtatSinistre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.lieuAccident) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.gouvernorat) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.compagnieAdverse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.usage) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.typeUsage) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "CAST(s.anneeExercice AS string) LIKE CONCAT('%', :searchTerm, '%') OR " +
            "CAST(s.codeIntermediaire AS string) LIKE CONCAT('%', :searchTerm, '%') OR " +
            "CAST(s.codeResponsabilite AS string) LIKE CONCAT('%', :searchTerm, '%')")
    Page<Sinistre> searchInColonnesImportantes(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Recherche avancée avec critères multiples
     */
    @Query("SELECT s FROM Sinistre s WHERE " +
            "(:natureSinistre IS NULL OR LOWER(s.natureSinistre) LIKE LOWER(CONCAT('%', :natureSinistre, '%'))) AND " +
            "(:typeSinistre IS NULL OR LOWER(s.typeSinistre) LIKE LOWER(CONCAT('%', :typeSinistre, '%'))) AND " +
            "(:libEtatSinistre IS NULL OR LOWER(s.libEtatSinistre) LIKE LOWER(CONCAT('%', :libEtatSinistre, '%'))) AND " +
            "(:gouvernorat IS NULL OR LOWER(s.gouvernorat) LIKE LOWER(CONCAT('%', :gouvernorat, '%'))) AND " +
            "(:anneeExercice IS NULL OR s.anneeExercice = :anneeExercice) AND " +
            "(:usage IS NULL OR LOWER(s.usage) LIKE LOWER(CONCAT('%', :usage, '%'))) AND " +
            "(:numContrat IS NULL OR LOWER(s.numContrat) LIKE LOWER(CONCAT('%', :numContrat, '%'))) AND " +
            "(:compagnieAdverse IS NULL OR LOWER(s.compagnieAdverse) LIKE LOWER(CONCAT('%', :compagnieAdverse, '%')))")
    Page<Sinistre> searchAvancee(
            @Param("natureSinistre") String natureSinistre,
            @Param("typeSinistre") String typeSinistre,
            @Param("libEtatSinistre") String libEtatSinistre,
            @Param("gouvernorat") String gouvernorat,
            @Param("anneeExercice") Integer anneeExercice,
            @Param("usage") String usage,
            @Param("numContrat") String numContrat,
            @Param("compagnieAdverse") String compagnieAdverse,
            Pageable pageable
    );

    // ✅ REQUÊTES POUR LES PROVISIONS ET RECOURS

    /**
     * Sinistres avec provisions de recours
     */
    @Query("SELECT s FROM Sinistre s WHERE s.provisionDeRecours > 0 ORDER BY s.provisionDeRecours DESC")
    Page<Sinistre> findWithProvisionDeRecours(Pageable pageable);

    /**
     * Sinistres avec prévisions de recours
     */
    @Query("SELECT s FROM Sinistre s WHERE s.previsionDeRecoursDomVeh IS NOT NULL AND s.previsionDeRecoursDomVeh != '' ORDER BY s.dateDeclaration DESC")
    Page<Sinistre> findWithPrevisionDeRecours(Pageable pageable);

    /**
     * Statistiques des provisions par état
     */
    @Query("SELECT s.libEtatSinistre, COUNT(s), SUM(s.provisionDeRecours), AVG(s.provisionDeRecours) " +
            "FROM Sinistre s WHERE s.provisionDeRecours IS NOT NULL AND s.provisionDeRecours > 0 " +
            "GROUP BY s.libEtatSinistre ORDER BY SUM(s.provisionDeRecours) DESC")
    List<Object[]> getStatistiquesProvisionsParEtat();

    // ✅ REQUÊTES POUR LES RÈGLEMENTS

    /**
     * Sinistres avec règlement RC
     */
    @Query("SELECT s FROM Sinistre s WHERE s.reglementRc > 0 ORDER BY s.reglementRc DESC")
    Page<Sinistre> findWithReglementRc(Pageable pageable);

    /**
     * Sinistres avec règlement défense et recours
     */
    @Query("SELECT s FROM Sinistre s WHERE s.reglementDefenseEtRecours > 0 ORDER BY s.reglementDefenseEtRecours DESC")
    Page<Sinistre> findWithReglementDefenseEtRecours(Pageable pageable);

    /**
     * Total des règlements par année
     */
    @Query("SELECT s.anneeExercice, SUM(s.totalReglement), COUNT(s) " +
            "FROM Sinistre s WHERE s.totalReglement IS NOT NULL " +
            "GROUP BY s.anneeExercice ORDER BY s.anneeExercice DESC")
    List<Object[]> getTotalReglementsParAnnee();

    // ✅ REQUÊTES POUR LES SINISTRES CORPORELS

    /**
     * Sinistres corporels avec blessés
     */
    @Query("SELECT s FROM Sinistre s WHERE s.nombreBlesses > 0 ORDER BY s.nombreBlesses DESC, s.dateDeclaration DESC")
    Page<Sinistre> findSinistresCorporelsAvecBlesses(Pageable pageable);

    /**
     * Sinistres corporels avec décès
     */
    @Query("SELECT s FROM Sinistre s WHERE s.nombreDeces > 0 ORDER BY s.nombreDeces DESC, s.dateDeclaration DESC")
    Page<Sinistre> findSinistresCorporelsAvecDeces(Pageable pageable);

    /**
     * Statistiques des sinistres corporels
     */
    @Query("SELECT COUNT(s), SUM(s.nombreBlesses), SUM(s.nombreDeces), AVG(s.montantEvaluation) " +
            "FROM Sinistre s WHERE s.natureSinistre = 'CORPOREL'")
    Object[] getStatistiquesSinistresCorporels();

    // ✅ REQUÊTES POUR LE DASHBOARD

    /**
     * Top 10 des sinistres par montant d'évaluation
     */
    @Query("SELECT s FROM Sinistre s WHERE s.montantEvaluation IS NOT NULL ORDER BY s.montantEvaluation DESC")
    Page<Sinistre> findTop10ByMontantEvaluation(Pageable pageable);

    /**
     * Sinistres récents (derniers 30 jours)
     */
    @Query("SELECT s FROM Sinistre s WHERE s.dateDeclaration >= :dateLimit ORDER BY s.dateDeclaration DESC")
    Page<Sinistre> findSinistresRecents(@Param("dateLimit") Date dateLimit, Pageable pageable);

    /**
     * Sinistres en attente (état = MISE A JOUR ou REPRISE)
     */
    @Query("SELECT s FROM Sinistre s WHERE s.libEtatSinistre IN ('MISE A JOUR', 'REPRISE') ORDER BY s.dateDeclaration ASC")
    Page<Sinistre> findSinistresEnAttente(Pageable pageable);

    /**
     * Résumé pour le dashboard
     */
    @Query("SELECT " +
            "COUNT(s) as total, " +
            "SUM(CASE WHEN s.natureSinistre = 'CORPOREL' THEN 1 ELSE 0 END) as corporel, " +
            "SUM(CASE WHEN s.natureSinistre = 'MATERIEL' THEN 1 ELSE 0 END) as materiel, " +
            "SUM(CASE WHEN s.libEtatSinistre = 'MISE A JOUR' THEN 1 ELSE 0 END) as enCours, " +
            "SUM(CASE WHEN s.libEtatSinistre = 'CLOTURE' THEN 1 ELSE 0 END) as clotures, " +
            "SUM(s.totalReglement) as totalReglements, " +
            "AVG(s.montantEvaluation) as montantMoyen " +
            "FROM Sinistre s")
    Object[] getResumeDashboard();
}

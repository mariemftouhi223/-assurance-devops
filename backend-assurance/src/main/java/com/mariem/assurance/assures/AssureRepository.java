package com.mariem.assurance.assures;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssureRepository extends JpaRepository<Assure, Long> {


    // JpaRepository fournit déjà findAll(Pageable) qui gère la pagination et le tri.
    // Aucune modification n'est nécessaire ici pour le problème de tri/pagination,
    // mais je fournis le fichier complet pour la clarté.

    // Méthodes de recherche personnalisées si nécessaire

    List<Assure> findByAnneeExercice(Integer anneeExercice);


    boolean existsByNumContrat(Long numContrat);
    Optional<Assure> findByNumContrat(Long numContrat);
    void deleteByNumContrat(Long numContrat);
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Assure a")
    boolean hasAnyData();
}
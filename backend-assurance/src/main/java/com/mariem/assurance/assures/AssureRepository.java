package com.mariem.assurance.assures;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssureRepository extends JpaRepository<Assure, Long> {


    // JpaRepository fournit dÃ©jÃ  findAll(Pageable) qui gÃ¨re la pagination et le tri.
    // Aucune modification n'est nÃ©cessaire ici pour le problÃ¨me de tri/pagination,
    // mais je fournis le fichier complet pour la clartÃ©.

    // MÃ©thodes de recherche personnalisÃ©es si nÃ©cessaire

    List<Assure> findByAnneeExercice(Integer anneeExercice);


    boolean existsByNumContrat(Long numContrat);
    Optional<Assure> findByNumContrat(Long numContrat);
    void deleteByNumContrat(Long numContrat);
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Assure a")
    boolean hasAnyData();
}

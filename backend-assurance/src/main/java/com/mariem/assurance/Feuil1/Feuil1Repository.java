package com.mariem.assurance.Feuil1;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Feuil1Repository extends JpaRepository<Feuil1, Long> {
    List<Feuil1> findAll();
}
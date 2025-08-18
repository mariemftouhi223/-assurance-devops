package com.mariem.assurance.Feuil1;

import jakarta.persistence.*;

@Entity
@Table(name = "Feuil1")
public class Feuil1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int annee;
    private int ANNEE_EXERCICE_PROD;
    private long NUM_CONTRAT;
    private String EFFET_CONTRAT;
    private String VALIDITE_DU;
    private String VALIDITE_AU;
    private String CODE_NATURE_CONTRAT;
    private String PROCHAIN_TERME;
    private String DATE_EXPIRATION;
    private int CODE_INTERMEDIAIRE;
    private String nom;
    private String Date_Naissance;
    private String sexe;
    private String ville;
    private int CODE_POSTAL;

    // Getters and Setters
}

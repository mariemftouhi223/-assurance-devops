package com.mariem.assurance.assures;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mariem.assurance.dto.fraud.FraudDetectionDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "assures")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Assure {

    @Id
    @Column(name = "num_contrat")
    private Long numContrat;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "ANNEE_EXERCICE_PROD")
    private Integer anneeExerciceProd;

    @Column(name = "annee_exercice")
    private Integer anneeExercice;

    @Column(name = "EFFET_CONTRAT")
    private String effetContrat;

    @Column(name = "VALIDITE_DU")
    private String validiteDu;

    @Column(name = "VALIDITE_AU")
    private String validiteAu;

    @Column(name = "PROCHAIN_TERME")
    private String prochainTerme;

    @Column(name = "CODE_NATURE_CONTRAT")
    private String codeNatureContrat;

    @Column(name = "DATE_EXPIRATION")
    private String dateExpiration;

    @Column(name = "CODE_INTERMEDIAIRE")
    private Integer codeIntermediaire;

    @Column(name = "Date_Naissance")
    private String dateNaissance;

    @Column(name = "sexe")
    private String sexe;

    @Column(name = "ville")
    private String ville;

    @Column(name = "CODE_POSTAL")
    private Integer codePostal;

    @Column(name = "IMMATRICULATION_VEHICULE")
    private String immatriculationVehicule;

    @Column(name = "PREMIERE_MISE_CIRCULATION")
    private String premiereMiseCirculation;

    @Column(name = "MARQUE_VEHICULE")
    private String marqueVehicule;

    @Column(name = "usage")
    private String usage;

    @Column(name = "Leasing")
    private String leasing;

    // "class" est un nom de colonne un peu risquÃ© ; mappÃ© sur champ Java "classeAssure"
    @Column(name = "class")
    private Integer classeAssure;

    @Column(name = "PERSONNE_PHYSIQUE")
    private Integer personnePhysique;

    @Column(name = "PERSONNE_MORALE")
    private Integer personneMorale;

    @Column(name = "num_quittance")
    private Integer numQuittance;

    @Column(name = "rc")
    private Double rc;

    @Column(name = "d_rec")
    private Double dRec;

    @Column(name = "incendie")
    private Double incendie;

    @Column(name = "vol")
    private Double vol;

    @Column(name = "DOMMAGES_AU_VEHICULE")
    private Double dommagesAuVehicule;

    @Column(name = "dommages_et_collision")
    private Double dommagesEtCollision;

    @Column(name = "bris_de_glaces")
    private Double brisDeGlaces;

    @Column(name = "pta")
    private Double pta;

    @Column(name = "individuelle_accident")
    private Double individuelleAccident;

    @Column(name = "catastrophe_naturelle")
    private Double catastropheNaturelle;

    @Column(name = "emeute_mouvement_populaire")
    private Double emeuteMouvementPopulaire;

    @Column(name = "vol_radio_cassette")
    private Double volRadioCassette;

    @Column(name = "Assistanceet_carglass")
    private Double assistanceEtCarglass;

    @Column(name = "carglass")
    private Double carglass;

    @Column(name = "TOTAL_TAXE")
    private Double totalTaxe;

    @Column(name = "frais")
    private Double frais;

    @Column(name = "total_prime_nette")
    private Double totalPrimeNette;

    @Column(name = "capitale_inc")
    private Double capitaleInc;

    @Column(name = "capitale_vol")
    private Double capitaleVol;

    @Column(name = "capitale_dv")
    private Double capitaleDv;

    @Column(name = "valeur_catalogue")
    private Double valeurCatalogue;

    @Column(name = "valeur_venale")
    private Double valeurVenale;

    @Column(name = "puissance")
    private String puissance;

    // ===== Score ML attachÃ© Ã  l'assurÃ© (non persistÃ©) =====
    @Transient
    private FraudDetectionDTO fraudDetection;

    public Assure() {}

    // ---- CompatibilitÃ© & correction du bug sur dRec ----
    // Certains appels utilisaient getDrec() (r minuscule) -> on le garde, mais correct.
    public Double getDrec() {
        return dRec; // IMPORTANT
    }
    public void setDrec(Double v) { this.dRec = v; }

    @Override
    public String toString() {
        return "Assure{" +
                "numContrat=" + numContrat +
                ", annee=" + annee +
                ", immatriculationVehicule='" + immatriculationVehicule + '\'' +
                ", marqueVehicule='" + marqueVehicule + '\'' +
                ", totalPrimeNette=" + totalPrimeNette +
                ", ville='" + ville + '\'' +
                ", sexe='" + sexe + '\'' +
                ", fraudDetection=" + (fraudDetection != null ? fraudDetection.getFraudScore() + "%" : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assure assure = (Assure) o;
        return numContrat != null && numContrat.equals(assure.numContrat);
    }

    @Override
    public int hashCode() {
        return numContrat != null ? numContrat.hashCode() : 0;
    }
}

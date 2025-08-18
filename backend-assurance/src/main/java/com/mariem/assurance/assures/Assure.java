package com.mariem.assurance.assures;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "assures")
public class Assure {

    @Id
    @Column(name = "num_contrat")
    private Long numContrat;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "ANNEE_EXERCICE_PROD")
    private Integer anneeExerciceProd;

    @Column(name = "EFFET_CONTRAT")
    private String effetContrat;

    @Column(name = "VALIDITE_DU")
    private String validiteDu;

    @Column(name = "VALIDITE_AU")
    private String validiteAu;

    @Column(name = "CODE_NATURE_CONTRAT")
    private String codeNatureContrat;

    @Column(name = "DATE_EXPIRATION")
    private String dateExpiration;

    @Column(name = "ville")
    private String ville;

    @Column(name = "CODE_POSTAL")
    private Integer codePostal;

    @Column(name = "IMMATRICULATION_VEHICULE")
    private String immatriculationVehicule;

    @Column(name = "MARQUE_VEHICULE")
    private String marqueVehicule;

    @Column(name = "PREMIERE_MISE_CIRCULATION")
    private String premiereMiseCirculation;

    @Column(name = "sexe")
    private String sexe;

    // Correction : Date_Naissance au lieu de DATE_NAISSANCE
    @Column(name = "Date_Naissance")
    private String dateNaissance;

    @Column(name = "PERSONNE_PHYSIQUE")
    private Integer personnePhysique;

    @Column(name = "PERSONNE_MORALE")
    private Integer personneMorale;

    @Column(name = "rc")
    private Double rc;

    @Column(name = "d_rec")
    private Double dRec;

    // Correction : colonnes séparées incendie et vol
    @Column(name = "incendie")
    private Double incendie;

    @Column(name = "vol")
    private Double vol;

    // Correction : DOMMAGES_AU_VEHICULE au lieu de DOMMAGES_AU_VEHICULES
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

    // Correction : Assistanceet_carglass au lieu de ASSISTANCE_ET_CARGLASS
    @Column(name = "Assistanceet_carglass")
    private Double assistanceEtCarglass;

    @Column(name = "carglass")
    private Double carglass;

    // Correction : TOTAL_TAXE au lieu de TAXE
    @Column(name = "TOTAL_TAXE")
    private Double taxe;

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

    @Column(name = "usage")
    private String usage;

    // Correction : class au lieu de CLASSE
    @Column(name = "class")
    private Integer classe;

    // Nouvelles colonnes importantes de la base
    @Column(name = "PROCHAIN_TERME")
    private String prochainTerme;

    @Column(name = "CODE_INTERMEDIAIRE")
    private Integer codeIntermediaire;

    @Column(name = "Leasing")
    private String leasing;

    @Column(name = "num_quittance")
    private Integer numQuittance;

    @Column(name = "annee_exercice")
    private Integer anneeExercice;

    // Propriétés calculées pour compatibilité avec l'ancien code
    public Double getIncendieVol() {
        Double incendieVal = this.incendie != null ? this.incendie : 0.0;
        Double volVal = this.vol != null ? this.vol : 0.0;
        return incendieVal + volVal;
    }

    public Double getDommagesAuVehicules() {
        return this.dommagesAuVehicule;
    }

    public Double getTotal() {
        // Calculer le total à partir des composants
        double total = 0.0;
        if (rc != null) total += rc;
        if (dRec != null) total += dRec;
        if (incendie != null) total += incendie;
        if (vol != null) total += vol;
        if (dommagesAuVehicule != null) total += dommagesAuVehicule;
        if (dommagesEtCollision != null) total += dommagesEtCollision;
        if (brisDeGlaces != null) total += brisDeGlaces;
        if (pta != null) total += pta;
        if (individuelleAccident != null) total += individuelleAccident;
        if (catastropheNaturelle != null) total += catastropheNaturelle;
        if (emeuteMouvementPopulaire != null) total += emeuteMouvementPopulaire;
        if (volRadioCassette != null) total += volRadioCassette;
        if (assistanceEtCarglass != null) total += assistanceEtCarglass;
        if (carglass != null) total += carglass;
        return total;
    }

    // Constructeurs
    public Assure() {}

    // toString amélioré
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

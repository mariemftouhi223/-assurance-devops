package com.mariem.assurance.sinistre;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "base_sinistre_1")
public class Sinistre {

    @Id
    @Column(name = "num_sinistre")
    private String numSinistre;

    // ✅ COLONNES IMPORTANTES DEMANDÉES PAR L'UTILISATEUR

    @Column(name = "ANNEE_EXERCICE")
    private Integer anneeExercice;

    @Column(name = "num_contrat")
    private String numContrat;

    @Column(name = "effet_contrat")
    @Temporal(TemporalType.DATE)
    private Date effetContrat;

    @Column(name = "DATE_EXPIRATION")
    private String dateExpiration;

    @Column(name = "PROCHAIN_TERME")
    private String prochainTerme;

    @Column(name = "usage")
    private String usage;

    @Column(name = "CODE_INTERMEDIAIRE")
    private Integer codeIntermediaire;

    @Column(name = "NATURE_SINISTRE")
    private String natureSinistre;

    @Column(name = "LIEU_ACCIDENT")
    private String lieuAccident;

    @Column(name = "TYPE_SINISTRE")
    private String typeSinistre;

    @Column(name = "COMPAGNIE_ADVERSE")
    private String compagnieAdverse;

    @Column(name = "CODE_RESPONSABILITE")
    private Integer codeResponsabilite;

    @Column(name = "date_declaration")
    @Temporal(TemporalType.DATE)
    private Date dateDeclaration;

    @Column(name = "date_ouverture")
    @Temporal(TemporalType.DATE)
    private Date dateOuverture;

    @Column(name = "date_survenance")
    @Temporal(TemporalType.DATE)
    private Date dateSurvenance;

    @Column(name = "LIB_ETAT_SINISTRE")
    private String libEtatSinistre;

    @Column(name = "etat_sin_année")
    private String etatSinAnnee;

    @Column(name = "MONTANT_EVALUATION")
    private Double montantEvaluation;

    @Column(name = "Total_REGLEMENT")
    private Double totalReglement;

    @Column(name = "REGLEMENT_RC")
    private Double reglementRc;

    @Column(name = "REGLEMENT_DEFENSE_ET_RECOURS")
    private Double reglementDefenseEtRecours;

    @Column(name = "total_sap_final")
    private Double totalSapFinal;

    @Column(name = "SAP_RC")
    private String sapRc;

    @Column(name = "SAP_DEFENSE_ET_RECOURS")
    private String sapDefenseEtRecours;

    @Column(name = "cumul_reglement")
    private String cumulReglement;

    @Column(name = "provision_de_recours")
    private Double provisionDeRecours;

    @Column(name = "PROVISION_DE_RECOURS_DEFENSE_ET_RECOURS")
    private String provisionDeRecoursDefenseEtRecours;

    @Column(name = "PREVISION_DE_RECOURS_Dom_veh")
    private String previsionDeRecoursDomVeh;

    @Column(name = "CUMUL_PREVISION_DE_RECOURS")
    private String cumulPrevisionDeRecours;

    // ✅ COLONNES SUPPLÉMENTAIRES UTILES POUR L'AFFICHAGE

    @Column(name = "gouvernorat")
    private String gouvernorat;

    @Column(name = "nombre_blesses")
    private Integer nombreBlesses;

    @Column(name = "nombre_deces")
    private Integer nombreDeces;

    @Column(name = "type_usage")
    private String typeUsage;

    // ✅ CONSTRUCTEURS

    public Sinistre() {}

    public Sinistre(String numSinistre) {
        this.numSinistre = numSinistre;
    }

    // ✅ MÉTHODES UTILITAIRES POUR L'AFFICHAGE

    /**
     * Retourne le montant d'évaluation formaté en dinars tunisiens
     */
    public String getMontantEvaluationFormate() {
        if (montantEvaluation == null) return "0,00 DT";
        return String.format("%.2f DT", montantEvaluation);
    }

    /**
     * Retourne le total règlement formaté en dinars tunisiens
     */
    public String getTotalReglementFormate() {
        if (totalReglement == null) return "0,00 DT";
        return String.format("%.2f DT", totalReglement);
    }

    /**
     * Retourne l'état du sinistre avec couleur pour l'affichage
     */
    public String getEtatAvecCouleur() {
        if (libEtatSinistre == null) return "NON DÉFINI";

        switch (libEtatSinistre.toUpperCase()) {
            case "MISE A JOUR":
                return "🔄 " + libEtatSinistre;
            case "REPRISE":
                return "▶️ " + libEtatSinistre;
            case "REOUVERTURE":
                return "🔓 " + libEtatSinistre;
            case "CLOTURE":
                return "✅ " + libEtatSinistre;
            default:
                return "📋 " + libEtatSinistre;
        }
    }

    /**
     * Retourne la nature du sinistre avec icône
     */
    public String getNatureAvecIcone() {
        if (natureSinistre == null) return "❓ NON DÉFINI";

        switch (natureSinistre.toUpperCase()) {
            case "CORPOREL":
                return "🏥 " + natureSinistre;
            case "MATERIEL":
                return "🚗 " + natureSinistre;
            case "MIXTE":
                return "⚡ " + natureSinistre;
            default:
                return "📋 " + natureSinistre;
        }
    }

    /**
     * Retourne le type de sinistre avec icône
     */
    public String getTypeAvecIcone() {
        if (typeSinistre == null) return "❓ NON DÉFINI";

        switch (typeSinistre.toUpperCase()) {
            case "COLLISION":
                return "💥 " + typeSinistre;
            case "VOL":
                return "🔒 " + typeSinistre;
            case "INCENDIE":
                return "🔥 " + typeSinistre;
            case "BRIS DE GLACE":
                return "🪟 " + typeSinistre;
            default:
                return "📋 " + typeSinistre;
        }
    }

    /**
     * Calcule l'âge du sinistre en jours
     */
    public long getAgeSinistreEnJours() {
        if (dateDeclaration == null) return 0;

        long diffInMillies = System.currentTimeMillis() - dateDeclaration.getTime();
        return diffInMillies / (24 * 60 * 60 * 1000);
    }

    /**
     * Retourne le statut de priorité basé sur l'âge et le montant
     */
    public String getPriorite() {
        long age = getAgeSinistreEnJours();
        double montant = montantEvaluation != null ? montantEvaluation : 0;

        if (age > 365 || montant > 50000) {
            return "🔴 HAUTE";
        } else if (age > 180 || montant > 20000) {
            return "🟡 MOYENNE";
        } else {
            return "🟢 NORMALE";
        }
    }

    // ✅ MÉTHODES STANDARD

    @Override
    public String toString() {
        return "Sinistre{" +
                "numSinistre='" + numSinistre + '\'' +
                ", anneeExercice=" + anneeExercice +
                ", numContrat='" + numContrat + '\'' +
                ", natureSinistre='" + natureSinistre + '\'' +
                ", typeSinistre='" + typeSinistre + '\'' +
                ", libEtatSinistre='" + libEtatSinistre + '\'' +
                ", montantEvaluation=" + montantEvaluation +
                ", totalReglement=" + totalReglement +
                ", dateDeclaration=" + dateDeclaration +
                ", dateSurvenance=" + dateSurvenance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sinistre)) return false;
        Sinistre sinistre = (Sinistre) o;
        return numSinistre != null && numSinistre.equals(sinistre.numSinistre);
    }

    @Override
    public int hashCode() {
        return numSinistre != null ? numSinistre.hashCode() : 0;
    }
}

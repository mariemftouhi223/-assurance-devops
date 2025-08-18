package com.mariem.assurance.sinistre;

import java.time.LocalDateTime;

/**
 * Classe DTO pour les critères de recherche avancée des sinistres
 */
public class SinistreSearchCriteria {

    // Critères de base
    private String numContrat;
    private Integer anneeExercice;
    private String natureSinistre;
    private String typeSinistre;
    private String libEtatSinistre;
    private String gouvernorat;
    private Integer codeIntermediaire;
    private String lieuAccident;
    private String compagnieAdverse;
    private Integer codeResponsabilite;

    // Critères de dates
    private LocalDateTime dateDeclarationDebut;
    private LocalDateTime dateDeclarationFin;
    private LocalDateTime dateSurvenanceDebut;
    private LocalDateTime dateSurvenanceFin;
    private LocalDateTime dateOuvertureDebut;
    private LocalDateTime dateOuvertureFin;

    // Critères de montants
    private Double montantEvaluationMin;
    private Double montantEvaluationMax;
    private Double totalReglementMin;
    private Double totalReglementMax;

    // Critères numériques
    private Integer nombreBlessesMin;
    private Integer nombreBlessesMax;
    private Integer nombreDecesMin;
    private Integer nombreDecesMax;

    // Recherche textuelle globale
    private String searchText;

    // Pagination et tri
    private String sortBy = "dateDeclaration";
    private String sortDirection = "desc";
    private Integer page = 0;
    private Integer size = 20;

    // Constructeurs
    public SinistreSearchCriteria() {}

    // Getters et Setters
    public String getNumContrat() {
        return numContrat;
    }

    public void setNumContrat(String numContrat) {
        this.numContrat = numContrat;
    }

    public Integer getAnneeExercice() {
        return anneeExercice;
    }

    public void setAnneeExercice(Integer anneeExercice) {
        this.anneeExercice = anneeExercice;
    }

    public String getNatureSinistre() {
        return natureSinistre;
    }

    public void setNatureSinistre(String natureSinistre) {
        this.natureSinistre = natureSinistre;
    }

    public String getTypeSinistre() {
        return typeSinistre;
    }

    public void setTypeSinistre(String typeSinistre) {
        this.typeSinistre = typeSinistre;
    }

    public String getLibEtatSinistre() {
        return libEtatSinistre;
    }

    public void setLibEtatSinistre(String libEtatSinistre) {
        this.libEtatSinistre = libEtatSinistre;
    }

    public String getGouvernorat() {
        return gouvernorat;
    }

    public void setGouvernorat(String gouvernorat) {
        this.gouvernorat = gouvernorat;
    }

    public Integer getCodeIntermediaire() {
        return codeIntermediaire;
    }

    public void setCodeIntermediaire(Integer codeIntermediaire) {
        this.codeIntermediaire = codeIntermediaire;
    }

    public String getLieuAccident() {
        return lieuAccident;
    }

    public void setLieuAccident(String lieuAccident) {
        this.lieuAccident = lieuAccident;
    }

    public String getCompagnieAdverse() {
        return compagnieAdverse;
    }

    public void setCompagnieAdverse(String compagnieAdverse) {
        this.compagnieAdverse = compagnieAdverse;
    }

    public Integer getCodeResponsabilite() {
        return codeResponsabilite;
    }

    public void setCodeResponsabilite(Integer codeResponsabilite) {
        this.codeResponsabilite = codeResponsabilite;
    }

    public LocalDateTime getDateDeclarationDebut() {
        return dateDeclarationDebut;
    }

    public void setDateDeclarationDebut(LocalDateTime dateDeclarationDebut) {
        this.dateDeclarationDebut = dateDeclarationDebut;
    }

    public LocalDateTime getDateDeclarationFin() {
        return dateDeclarationFin;
    }

    public void setDateDeclarationFin(LocalDateTime dateDeclarationFin) {
        this.dateDeclarationFin = dateDeclarationFin;
    }

    public LocalDateTime getDateSurvenanceDebut() {
        return dateSurvenanceDebut;
    }

    public void setDateSurvenanceDebut(LocalDateTime dateSurvenanceDebut) {
        this.dateSurvenanceDebut = dateSurvenanceDebut;
    }

    public LocalDateTime getDateSurvenanceFin() {
        return dateSurvenanceFin;
    }

    public void setDateSurvenanceFin(LocalDateTime dateSurvenanceFin) {
        this.dateSurvenanceFin = dateSurvenanceFin;
    }

    public LocalDateTime getDateOuvertureDebut() {
        return dateOuvertureDebut;
    }

    public void setDateOuvertureDebut(LocalDateTime dateOuvertureDebut) {
        this.dateOuvertureDebut = dateOuvertureDebut;
    }

    public LocalDateTime getDateOuvertureFin() {
        return dateOuvertureFin;
    }

    public void setDateOuvertureFin(LocalDateTime dateOuvertureFin) {
        this.dateOuvertureFin = dateOuvertureFin;
    }

    public Double getMontantEvaluationMin() {
        return montantEvaluationMin;
    }

    public void setMontantEvaluationMin(Double montantEvaluationMin) {
        this.montantEvaluationMin = montantEvaluationMin;
    }

    public Double getMontantEvaluationMax() {
        return montantEvaluationMax;
    }

    public void setMontantEvaluationMax(Double montantEvaluationMax) {
        this.montantEvaluationMax = montantEvaluationMax;
    }

    public Double getTotalReglementMin() {
        return totalReglementMin;
    }

    public void setTotalReglementMin(Double totalReglementMin) {
        this.totalReglementMin = totalReglementMin;
    }

    public Double getTotalReglementMax() {
        return totalReglementMax;
    }

    public void setTotalReglementMax(Double totalReglementMax) {
        this.totalReglementMax = totalReglementMax;
    }

    public Integer getNombreBlessesMin() {
        return nombreBlessesMin;
    }

    public void setNombreBlessesMin(Integer nombreBlessesMin) {
        this.nombreBlessesMin = nombreBlessesMin;
    }

    public Integer getNombreBlessesMax() {
        return nombreBlessesMax;
    }

    public void setNombreBlessesMax(Integer nombreBlessesMax) {
        this.nombreBlessesMax = nombreBlessesMax;
    }

    public Integer getNombreDecesMin() {
        return nombreDecesMin;
    }

    public void setNombreDecesMin(Integer nombreDecesMin) {
        this.nombreDecesMin = nombreDecesMin;
    }

    public Integer getNombreDecesMax() {
        return nombreDecesMax;
    }

    public void setNombreDecesMax(Integer nombreDecesMax) {
        this.nombreDecesMax = nombreDecesMax;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "SinistreSearchCriteria{" +
                "numContrat='" + numContrat + '\'' +
                ", anneeExercice=" + anneeExercice +
                ", natureSinistre='" + natureSinistre + '\'' +
                ", typeSinistre='" + typeSinistre + '\'' +
                ", libEtatSinistre='" + libEtatSinistre + '\'' +
                ", gouvernorat='" + gouvernorat + '\'' +
                ", codeIntermediaire=" + codeIntermediaire +
                ", lieuAccident='" + lieuAccident + '\'' +
                ", compagnieAdverse='" + compagnieAdverse + '\'' +
                ", codeResponsabilite=" + codeResponsabilite +
                ", dateDeclarationDebut=" + dateDeclarationDebut +
                ", dateDeclarationFin=" + dateDeclarationFin +
                ", dateSurvenanceDebut=" + dateSurvenanceDebut +
                ", dateSurvenanceFin=" + dateSurvenanceFin +
                ", dateOuvertureDebut=" + dateOuvertureDebut +
                ", dateOuvertureFin=" + dateOuvertureFin +
                ", montantEvaluationMin=" + montantEvaluationMin +
                ", montantEvaluationMax=" + montantEvaluationMax +
                ", totalReglementMin=" + totalReglementMin +
                ", totalReglementMax=" + totalReglementMax +
                ", nombreBlessesMin=" + nombreBlessesMin +
                ", nombreBlessesMax=" + nombreBlessesMax +
                ", nombreDecesMin=" + nombreDecesMin +
                ", nombreDecesMax=" + nombreDecesMax +
                ", searchText='" + searchText + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}


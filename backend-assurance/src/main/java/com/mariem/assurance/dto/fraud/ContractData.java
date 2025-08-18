package com.mariem.assurance.dto.fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContractData {
    private String contractId;
    private String clientId;

    // UTILISE Double (objet) au lieu de double (primitif)
    private Double amount;
    private String startDate;
    private String endDate;

    private Double rc;
    private Integer dRec;
    private Double incendie;
    private Double vol;
    private Integer dommagesAuVehicule;
    private Integer dommagesEtCollision;
    private Integer brisDeGlaces;
    private Integer pta;
    private Integer individuelleAccident;
    private Integer catastropheNaturelle;
    private Integer emeuteMouvementPopulaire;
    private Integer volRadioCassette;
    private Integer carglass;

    @JsonProperty("Assistanceet_carglass")
    private Integer assistanceEtCarglass;

    private Double totalTaxe;
    private Integer frais;
    private Double totalPrimeNette;
    private Double capitaleInc;
    private Double capitaleVol;
    private Integer capitaleDv;
    private Integer valeurCatalogue;
    private Integer valeurVenale;
}

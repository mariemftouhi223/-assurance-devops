package com.mariem.assurance.dto.fraud;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ContractData {
    private Double rc;
    private Double drec;                       // d_rec en DB -> drec en JSON
    private Double incendie;
    private Double vol;
    private Double dommagesAuVehicule;
    private Double dommagesEtCollision;
    private Double brisDeGlaces;
    private Double pta;
    private Double individuelleAccident;
    private Double catastropheNaturelle;
    private Double emeuteMouvementPopulaire;
    private Double volRadioCassette;
    private Double carglass;
    private Double assistanceEtCarglass;      // deviendra assistance_et_carglass en JSON
    private Double totalTaxe;
    private Double frais;
    private Double totalPrimeNette;
    private Double capitaleInc;
    private Double capitaleVol;
    private Double capitaleDv;
    private Double valeurCatalogue;
    private Double valeurVenale;
    private Integer puissance;                // "4" (String en DB) -> Integer ici
}

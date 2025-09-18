package com.mariem.assurance.dto.fraud;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)  // snake_case requis par Flask
public class ClientData {
    private Integer ageConducteur; // dÃ©rivÃ© de dateNaissance
    private String sexe;           // "M" / "F"
    private String ville;          // ex: "LE BARDO"
    private Integer codePostal;    // ex: 2000
}

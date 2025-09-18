package com.mariem.assurance.dto.fraud;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FraudPredictionRequest {

    private ClientData clientData;     // requis
    private ContractData contractData; // requis
    private String source;             // "ASSURE" | "SINISTRE"

    /** Fabrique conseillÃ©e : construit la requÃªte ML Ã  partir dâ€™un Assure */
    public static FraudPredictionRequest fromAssure(com.mariem.assurance.assures.Assure a) {
        if (a == null) return null;

        // ---- client ----
        ClientData client = new ClientData();
        client.setAgeConducteur(calcAge(a.getDateNaissance()));
        client.setSexe(a.getSexe());
        client.setVille(a.getVille());
        client.setCodePostal(a.getCodePostal());

        // ---- contrat ----
        ContractData c = new ContractData();
        c.setRc(nz(a.getRc()));
        c.setDrec(nz(a.getDrec()));                    // <â€” important : getDrec() (bug corrigÃ© cÃ´tÃ© Assure)
        c.setIncendie(nz(a.getIncendie()));
        c.setVol(nz(a.getVol()));
        c.setDommagesAuVehicule(nz(a.getDommagesAuVehicule()));
        c.setDommagesEtCollision(nz(a.getDommagesEtCollision()));
        c.setBrisDeGlaces(nz(a.getBrisDeGlaces()));
        c.setPta(nz(a.getPta()));
        c.setIndividuelleAccident(nz(a.getIndividuelleAccident()));
        c.setCatastropheNaturelle(nz(a.getCatastropheNaturelle()));
        c.setEmeuteMouvementPopulaire(nz(a.getEmeuteMouvementPopulaire()));
        c.setVolRadioCassette(nz(a.getVolRadioCassette()));
        c.setCarglass(nz(a.getCarglass()));
        c.setAssistanceEtCarglass(nz(a.getAssistanceEtCarglass()));
        c.setTotalTaxe(nz(a.getTotalTaxe()));
        c.setFrais(nz(a.getFrais()));
        c.setTotalPrimeNette(nz(a.getTotalPrimeNette()));
        c.setCapitaleInc(nz(a.getCapitaleInc()));
        c.setCapitaleVol(nz(a.getCapitaleVol()));
        c.setCapitaleDv(nz(a.getCapitaleDv()));
        c.setValeurCatalogue(nz(a.getValeurCatalogue()));
        c.setValeurVenale(nz(a.getValeurVenale()));
        c.setPuissance(parseIntSafe(a.getPuissance())); // "4" -> 4 (Integer)

        return new FraudPredictionRequest(client, c, "ASSURE");
    }

    // ---------- helpers ----------
    private static Double nz(Double v) { return v == null ? 0.0 : v; }

    private static Integer parseIntSafe(String s){
        try { return (s == null || s.isBlank()) ? null : Integer.parseInt(s.trim()); }
        catch(Exception e){ return null; }
    }

    private static Integer calcAge(String dateNaissance){
        try{
            if(dateNaissance == null || dateNaissance.isBlank()) return null;

            String[] parts;
            if (dateNaissance.contains("/")) parts = dateNaissance.split("/");
            else if (dateNaissance.contains("-")) parts = dateNaissance.split("-");
            else return null;

            if (parts.length != 3) return null;

            int day, month, year;
            // yyyy-MM-dd ?
            if (parts[0].length() == 4) {
                year = Integer.parseInt(parts[0]); month = Integer.parseInt(parts[1]); day = Integer.parseInt(parts[2]);
            } else {
                day = Integer.parseInt(parts[0]); month = Integer.parseInt(parts[1]); year = Integer.parseInt(parts[2]);
            }

            java.time.LocalDate birth = java.time.LocalDate.of(year, month, day);
            return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
        } catch(Exception e){
            System.err.println("Erreur calcul Ã¢ge pour date: " + dateNaissance + " - " + e.getMessage());
            return null;
        }
    }
}

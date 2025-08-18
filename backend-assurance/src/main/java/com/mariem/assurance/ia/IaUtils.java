package com.mariem.assurance.ia;

import com.mariem.assurance.assures.Assure;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public class IaUtils {

    public static DonneesInput fromAssure(Assure assure) {
        double taxe = assure.getTaxe() != null ? assure.getTaxe() : 0;
        double frais = assure.getFrais() != null ? assure.getFrais() : 0;
        double totalPrimeNette = assure.getTotalPrimeNette() != null ? assure.getTotalPrimeNette() : 0;
        double totalCost = taxe + frais + totalPrimeNette;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Calcul durée contrat
        long contractDurationDays = 0;
        try {
            LocalDate dateDebut = LocalDate.parse(assure.getValiditeDu(), formatter);
            LocalDate dateFin = LocalDate.parse(assure.getValiditeAu(), formatter);
            contractDurationDays = ChronoUnit.DAYS.between(dateDebut, dateFin);
        } catch (Exception e) {
            System.out.println("Erreur parsing dates contrat : " + e.getMessage());
        }

        // Calcul âge assuré
        long ageInsured = 0;
        try {
            LocalDate birthDate = LocalDate.parse(assure.getDateNaissance(), formatter);
            ageInsured = ChronoUnit.YEARS.between(birthDate, LocalDate.now());
        } catch (Exception e) {
            System.out.println("Erreur parsing date de naissance : " + e.getMessage());
        }

        return new DonneesInput(taxe, frais, totalPrimeNette, totalCost, contractDurationDays, ageInsured);
    }
}

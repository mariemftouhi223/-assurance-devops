package com.mariem.assurance.ia;

import lombok.Data;

@Data
public class DonneesInput {
    private double taxe;
    private double frais;
    private double totalPrimeNette;
    private double totalCost;
    private double contractDurationDays;
    private double ageInsured;

    public DonneesInput(double taxe, double frais, double totalPrimeNette, double totalCost, long contractDurationDays, long ageInsured) {
    }
}

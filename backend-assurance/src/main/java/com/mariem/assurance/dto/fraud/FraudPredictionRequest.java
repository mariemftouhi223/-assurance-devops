package com.mariem.assurance.dto.fraud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requête de prédiction de fraude contenant les données du contrat et du client")
public class FraudPredictionRequest {

    @NotNull(message = "Les données du contrat sont obligatoires")
    @Valid
    @Schema(description = "Données du contrat d'assurance")
    private ContractData contractData;

    @NotNull(message = "Les données du client sont obligatoires")
    @Valid
    @Schema(description = "Données du client")
    private ClientData clientData;

    // Constructeurs
    public FraudPredictionRequest() {}

    public FraudPredictionRequest(ContractData contractData, ClientData clientData) {
        this.contractData = contractData;
        this.clientData = clientData;
    }

    // Getters et Setters
    public ContractData getContractData() {
        return contractData;
    }

    public void setContractData(ContractData contractData) {
        this.contractData = contractData;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public void setClientData(ClientData clientData) {
        this.clientData = clientData;
    }

    @Override
    public String toString() {
        return "FraudPredictionRequest{" +
                "contractData=" + contractData +
                ", clientData=" + clientData +
                '}';
    }
}

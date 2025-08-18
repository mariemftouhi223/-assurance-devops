import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FraudDetectionService, FraudPredictionRequest, FraudPredictionResponse } from '../../services/fraud-detection.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-fraud-detection',
  templateUrl: './fraud-detection.component.html',
  styleUrls: ['./fraud-detection.component.scss']
})
export class FraudDetectionComponent implements OnInit {

  fraudForm: FormGroup;
  isLoading = false;
  lastResult: FraudPredictionResponse | null = null;
  alertGenerated = false;

  testStats = {
    total: 0,
    fraudDetected: 0
  };

  constructor(
    private fb: FormBuilder,
    private fraudService: FraudDetectionService,
    private notificationService: NotificationService
  ) {
    this.fraudForm = this.createForm();
  }

  ngOnInit(): void {
    this.loadTestData();
  }

  private createForm(): FormGroup {
    return this.fb.group({
      firstName: ['Jean', Validators.required],
      lastName: ['Dupont', Validators.required],
      contractId: ['TEST-' + Date.now(), Validators.required],
      rc: [1000, [Validators.required, Validators.min(0)]],
      dRec: [500, [Validators.required, Validators.min(0)]],
      incendie: [200, [Validators.required, Validators.min(0)]],
      vol: [300, [Validators.required, Validators.min(0)]],
      dommagesAuVehicule: [800, [Validators.required, Validators.min(0)]],
      dommagesEtCollision: [0, [Validators.required, Validators.min(0)]],
      brisDeGlaces: [100, [Validators.required, Validators.min(0)]],
      pta: [0, [Validators.required, Validators.min(0)]],
      individuelleAccident: [0, [Validators.required, Validators.min(0)]],
      catastropheNaturelle: [0, [Validators.required, Validators.min(0)]],
      emeuteMouvementPopulaire: [0, [Validators.required, Validators.min(0)]],
      volRadioCassette: [0, [Validators.required, Validators.min(0)]],
      assistanceEtCarglass: [50, [Validators.required, Validators.min(0)]],
      carglass: [0, [Validators.required, Validators.min(0)]],
      totalTaxe: [150, [Validators.required, Validators.min(0)]],
      frais: [50, [Validators.required, Validators.min(0)]],
      totalPrimeNette: [2000, [Validators.required, Validators.min(0)]],
      capitaleInc: [10000, [Validators.required, Validators.min(0)]],
      capitaleVol: [8000, [Validators.required, Validators.min(0)]],
      capitaleDv: [15000, [Validators.required, Validators.min(0)]],
      valeurCatalogue: [25000, [Validators.required, Validators.min(0)]],
      valeurVenale: [20000, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit(): void {
    if (this.fraudForm.valid) {
      this.isLoading = true;
      this.alertGenerated = false;

      const formValue = this.fraudForm.value;

      const request: FraudPredictionRequest = {
        clientData: {
          firstName: formValue.firstName,
          lastName: formValue.lastName,
          email: `${formValue.firstName.toLowerCase()}.${formValue.lastName.toLowerCase()}@test.com`,
          phone: '0123456789'
        },
        contractData: {
          contractId: formValue.contractId,
          rc: formValue.rc,
          dRec: formValue.dRec,
          incendie: formValue.incendie,
          vol: formValue.vol,
          dommagesAuVehicule: formValue.dommagesAuVehicule,
          dommagesEtCollision: formValue.dommagesEtCollision,
          brisDeGlaces: formValue.brisDeGlaces,
          pta: formValue.pta,
          individuelleAccident: formValue.individuelleAccident,
          catastropheNaturelle: formValue.catastropheNaturelle,
          emeuteMouvementPopulaire: formValue.emeuteMouvementPopulaire,
          volRadioCassette: formValue.volRadioCassette,
          assistanceEtCarglass: formValue.assistanceEtCarglass,
          carglass: formValue.carglass,
          totalTaxe: formValue.totalTaxe,
          frais: formValue.frais,
          totalPrimeNette: formValue.totalPrimeNette,
          capitaleInc: formValue.capitaleInc,
          capitaleVol: formValue.capitaleVol,
          capitaleDv: formValue.capitaleDv,
          valeurCatalogue: formValue.valeurCatalogue,
          valeurVenale: formValue.valeurVenale
        }
      };

      this.fraudService.analyzeFraud(request).subscribe({
        next: (response) => {
          this.lastResult = response;
          this.isLoading = false;
          this.testStats.total++;
          if (response.prediction.isFraud) {
            this.testStats.fraudDetected++;
            this.alertGenerated = true;
          }
          console.log('Résultat de l\'analyse:', response);
        },
        error: (error) => {
          console.error('Erreur lors de l\'analyse:', error);
          this.isLoading = false;
        }
      });
    }
  }

  loadTestData(): void {
    this.fraudForm.patchValue({
      contractId: 'TEST-NORMAL-' + Date.now(),
      rc: 1000,
      totalPrimeNette: 2000,
      valeurCatalogue: 25000,
      valeurVenale: 20000
    });
  }

  loadHighRiskData(): void {
    this.fraudForm.patchValue({
      contractId: 'TEST-FRAUD-' + Date.now(),
      rc: 5000,
      totalPrimeNette: 500,
      valeurCatalogue: 50000,
      valeurVenale: 10000,
      incendie: 1000,
      vol: 2000
    });
  }

  getResultHeaderClass(): string {
    if (!this.lastResult) return 'bg-secondary';
    return this.lastResult.prediction.isFraud ? 'bg-danger' : 'bg-success';
  }

  getResultIcon(): string {
    if (!this.lastResult) return 'fas fa-question';
    return this.lastResult.prediction.isFraud ? 'fas fa-exclamation-triangle' : 'fas fa-check-circle';
  }

  getProbabilityBarClass(): string {
    if (!this.lastResult) return 'bg-secondary';
    const prob = this.lastResult.prediction.fraudProbability;
    if (prob >= 0.8) return 'bg-danger';
    if (prob >= 0.6) return 'bg-warning';
    return 'bg-success';
  }

  getRiskLevelClass(): string {
    if (!this.lastResult) return 'bg-secondary';
    const level = this.lastResult.prediction.riskLevel;
    switch (level) {
      case 'VERY_HIGH': return 'bg-danger';
      case 'HIGH': return 'bg-warning';
      case 'MEDIUM': return 'bg-info';
      case 'LOW': return 'bg-success';
      default: return 'bg-secondary';
    }
  }
}

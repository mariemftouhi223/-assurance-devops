import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, delay } from 'rxjs/operators';

// Interfaces pour la détection de fraude
export interface FraudPredictionRequest {
  contractData: {
    contractId: number;
    rc: number;
    dRec: number;
    incendie: number;
    vol: number;
    dommagesAuVehicule: number;
    dommagesEtCollision: number;
    brisDeGlaces: number;
    pta: number;
    individuelleAccident: number;
    catastropheNaturelle: number;
    emeuteMouvementPopulaire: number;
    volRadioCassette: number;
    assistanceEtCarglass: number;
    carglass: number;
    totalTaxe: number;
    frais: number;
    totalPrimeNette: number;
    capitaleInc: number;
    capitaleVol: number;
    capitaleDv: number;
    valeurCatalogue: number;
    valeurVenale: number;
  };
  clientData: {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
  };
}

export interface FraudPredictionResult {
  isFraud: boolean;
  fraudProbability: number; // Entre 0 et 1
  riskLevel: string;
}

export interface FraudPredictionResponse {
  prediction: FraudPredictionResult;
  reason: string;
  confidence: number;
  riskFactors: string[];
  recommendation: string;
}

@Injectable({
  providedIn: 'root'
})
export class FraudDetectionService {
  private apiUrl = 'http://localhost:9099/api/v1/fraud-detection';

  // Mode de fonctionnement : 'api' pour utiliser l'API backend, 'mock' pour simulation
  private mode: 'api' | 'mock' = 'mock';

  // Compteur pour générer des cas de fraude de manière déterministe
  private analysisCounter = 0;

  constructor(private http: HttpClient) {
    console.log('🔧 FraudDetectionService initialisé en mode:', this.mode);
  }

  analyzeFraud(request: FraudPredictionRequest): Observable<FraudPredictionResponse> {
    if (this.mode === 'api') {
      return this.callFraudApi(request);
    } else {
      return this.mockFraudAnalysis(request);
    }
  }

  private callFraudApi(request: FraudPredictionRequest): Observable<FraudPredictionResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.post<FraudPredictionResponse>(`${this.apiUrl}/analyze`, request, { headers })
      .pipe(
        catchError(error => this.handleError(error))
      );
  }

  private mockFraudAnalysis(request: FraudPredictionRequest): Observable<FraudPredictionResponse> {
    // Simulation d'un modèle de détection de fraude amélioré
    const contractData = request.contractData;
    this.analysisCounter++;

    // Calcul du score de fraude basé sur des règles améliorées
    let fraudScore = 0;
    const riskFactors: string[] = [];

    // Règle 1: Prime très élevée par rapport à la valeur du véhicule
    const primeRatio = contractData.totalPrimeNette / Math.max(contractData.valeurVenale, 1);
    if (primeRatio > 0.25) {
      fraudScore += 0.35;
      riskFactors.push('Prime anormalement élevée par rapport à la valeur du véhicule');
    } else if (primeRatio > 0.15) {
      fraudScore += 0.2;
      riskFactors.push('Prime élevée par rapport à la valeur du véhicule');
    }

    // Règle 2: Combinaison suspecte de garanties
    const guaranteeCount = [
      contractData.rc, contractData.incendie, contractData.vol,
      contractData.dommagesAuVehicule, contractData.brisDeGlaces,
      contractData.dommagesEtCollision, contractData.pta
    ].filter(g => g && g > 0).length;

    if (guaranteeCount >= 5 && contractData.valeurVenale < 15000) {
      fraudScore += 0.3;
      riskFactors.push('Trop de garanties pour un véhicule de faible valeur');
    } else if (guaranteeCount >= 4 && contractData.valeurVenale < 10000) {
      fraudScore += 0.25;
      riskFactors.push('Garanties nombreuses pour véhicule de faible valeur');
    }

    // Règle 3: Valeurs incohérentes
    if (contractData.valeurCatalogue > 0 && contractData.valeurVenale > contractData.valeurCatalogue * 1.1) {
      fraudScore += 0.25;
      riskFactors.push('Valeur vénale supérieure à la valeur catalogue');
    }

    // Règle 4: Frais anormalement élevés
    if (contractData.frais > contractData.totalPrimeNette * 0.4) {
      fraudScore += 0.2;
      riskFactors.push('Frais disproportionnés par rapport à la prime');
    } else if (contractData.frais > contractData.totalPrimeNette * 0.25) {
      fraudScore += 0.15;
      riskFactors.push('Frais élevés par rapport à la prime');
    }

    // Règle 5: Garanties vol/incendie élevées pour véhicule ancien
    const currentYear = new Date().getFullYear();
    const vehicleAge = currentYear - 2010; // Estimation basée sur les données
    if (vehicleAge > 10 && (contractData.vol > 1000 || contractData.incendie > 1000)) {
      fraudScore += 0.2;
      riskFactors.push('Garanties vol/incendie élevées pour véhicule ancien');
    }

    // Règle 6: Simulation déterministe pour générer des cas de fraude
    // Génère un cas de fraude tous les 3-4 contrats pour démonstration
    if (this.analysisCounter % 3 === 0 || this.analysisCounter % 4 === 0) {
      fraudScore += 0.3;
      riskFactors.push('Profil client à risque élevé détecté par IA');
    }

    // Règle 7: Détection basée sur l'ID du contrat (pour démonstration)
    const contractIdStr = contractData.contractId.toString();
    if (contractIdStr.includes('000001') || contractIdStr.includes('000003') ||
      contractIdStr.includes('000005') || contractIdStr.includes('000007') ||
      contractIdStr.includes('000009')) {
      fraudScore += 0.4;
      riskFactors.push('Contrat identifié comme suspect par analyse historique');
    }

    // Règle 8: Montants suspects
    if (contractData.totalPrimeNette < 500 && contractData.valeurVenale > 30000) {
      fraudScore += 0.35;
      riskFactors.push('Prime très faible pour véhicule de haute valeur');
    }

    // Limitation du score entre 0 et 1
    fraudScore = Math.min(fraudScore, 1);

    // Ajout d'une variation aléatoire pour rendre la simulation plus réaliste
    const randomVariation = (Math.random() - 0.5) * 0.1; // ±5%
    fraudScore = Math.max(0, Math.min(1, fraudScore + randomVariation));

    // Détermination du statut frauduleux (seuil abaissé pour plus de détections)
    const isFraudulent = fraudScore > 0.45;

    // Génération de la raison principale
    let reason = 'Aucun risque significatif détecté';
    if (isFraudulent) {
      reason = riskFactors.length > 0 ? riskFactors[0] : 'Combinaison de facteurs de risque élevés';
    } else if (fraudScore > 0.3) {
      reason = 'Facteurs de risque modérés détectés';
    }

    // Détermination du niveau de risque
    let riskLevel = 'Faible';
    if (fraudScore > 0.8) {
      riskLevel = 'Très Élevé';
    } else if (fraudScore > 0.6) {
      riskLevel = 'Élevé';
    } else if (fraudScore > 0.4) {
      riskLevel = 'Modéré';
    }

    // Recommandation
    let recommendation = 'Contrat approuvé automatiquement';
    if (fraudScore > 0.8) {
      recommendation = 'REJET RECOMMANDÉ - Risque de fraude très élevé';
    } else if (fraudScore > 0.6) {
      recommendation = 'VÉRIFICATION MANUELLE OBLIGATOIRE - Risque élevé';
    } else if (fraudScore > 0.45) {
      recommendation = 'Surveillance renforcée recommandée - Risque modéré à élevé';
    } else if (fraudScore > 0.3) {
      recommendation = 'Surveillance standard recommandée - Risque modéré';
    }

    const response: FraudPredictionResponse = {
      prediction: {
        isFraud: isFraudulent,
        fraudProbability: fraudScore,
        riskLevel: riskLevel
      },
      reason,
      confidence: 0.82 + (Math.random() * 0.18), // Confiance entre 82% et 100%
      riskFactors,
      recommendation
    };

    console.log(`🔍 Analyse fraude pour contrat ${contractData.contractId}:`, {
      isFraud: isFraudulent,
      score: (fraudScore * 100).toFixed(1) + '%',
      reason: reason,
      riskFactors: riskFactors.length
    });

    // Simulation d'un délai d'API réaliste
    return of(response).pipe(delay(Math.random() * 800 + 300));
  }

  // Méthode pour changer le mode de fonctionnement
  setMode(mode: 'api' | 'mock'): void {
    this.mode = mode;
    console.log('🔧 Mode FraudDetectionService changé vers:', mode);
  }

  // Méthode pour réinitialiser le compteur (utile pour les tests)
  resetCounter(): void {
    this.analysisCounter = 0;
    console.log('🔄 Compteur d\'analyse réinitialisé');
  }

  // Méthode pour obtenir le mode actuel
  getCurrentMode(): string {
    return this.mode;
  }

  // Méthode pour tester la connexion à l'API
  testConnection(): Observable<any> {
    if (this.mode === 'mock') {
      return of({
        status: 'mock',
        message: 'Service en mode simulation - Détection de fraude active',
        analysisCount: this.analysisCounter
      });
    }

    const headers = new HttpHeaders({
      'Accept': 'application/json'
    });

    return this.http.get(`${this.apiUrl}/health`, { headers })
      .pipe(
        catchError(error => this.handleError(error))
      );
  }

  // Méthode pour obtenir les statistiques de fraude
  getFraudStatistics(): Observable<any> {
    if (this.mode === 'mock') {
      const mockStats = {
        totalAnalyzed: this.analysisCounter,
        fraudDetected: Math.floor(this.analysisCounter * 0.25), // ~25% de fraudes détectées
        averageScore: '0.35',
        lastUpdate: new Date().toISOString(),
        mode: 'simulation'
      };
      return of(mockStats).pipe(delay(300));
    }

    const headers = new HttpHeaders({
      'Accept': 'application/json'
    });

    return this.http.get(`${this.apiUrl}/statistics`, { headers })
      .pipe(
        catchError(error => this.handleError(error))
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Erreur inconnue lors de l\'analyse de fraude';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur client: ${error.error.message}`;
    } else {
      switch (error.status) {
        case 0:
          errorMessage = 'Impossible de contacter le service de détection de fraude. Vérifiez que l\'API est démarrée.';
          break;
        case 400:
          errorMessage = 'Données de contrat invalides pour l\'analyse de fraude.';
          break;
        case 401:
          errorMessage = 'Authentification requise pour le service de fraude.';
          break;
        case 403:
          errorMessage = 'Accès interdit au service de détection de fraude.';
          break;
        case 404:
          errorMessage = 'Service de détection de fraude non trouvé.';
          break;
        case 500:
          errorMessage = 'Erreur interne du service de détection de fraude.';
          break;
        case 503:
          errorMessage = 'Service de détection de fraude temporairement indisponible.';
          break;
        default:
          errorMessage = `Erreur service fraude: ${error.status} - ${error.message}`;
      }
    }

    console.error('❌ Erreur FraudDetectionService:', {
      status: error.status,
      message: error.message,
      url: error.url,
      error: error.error
    });

    return throwError(() => new Error(errorMessage));
  }
}

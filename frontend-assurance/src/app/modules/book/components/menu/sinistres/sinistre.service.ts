import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

// ✅ INTERFACES POUR LA DÉTECTION DE FRAUDE ML
export interface FraudDetection {
  isFraud: boolean;
  confidence: number;
  riskLevel: string;
  reason: string;
  riskFactors?: string[];
  recommendation?: string;
  fraudScore: number;
  alertLevel: string;
  alertIcon: string;
  alertColor: string;
}

export interface SinistreAvecML {
  // Données de base du sinistre
  numSinistre: string;
  anneeExercice: number;
  numContrat: string;
  effetContrat: string;
  dateExpiration: string;
  prochainTerme: string;
  dateDeclaration: string;
  dateOuverture: string;
  dateSurvenance: string;
  usage: string;
  typeUsage: string;
  codeIntermediaire: number;
  natureSinistre: string;
  natureAvecIcone: string;
  lieuAccident: string;
  gouvernorat: string;
  typeSinistre: string;
  typeAvecIcone: string;
  compagnieAdverse: string;
  codeResponsabilite: number;
  libEtatSinistre: string;
  etatAvecCouleur: string;
  etatSinAnnee: string;
  priorite: string;
  ageSinistreEnJours: number;
  montantEvaluation: string;
  montantEvaluationBrut: number;
  totalReglement: string;
  totalReglementBrut: number;
  reglementRc: number;
  reglementDefenseEtRecours: number;
  totalSapFinal: number;
  sapRc: string;
  sapDefenseEtRecours: string;
  cumulReglement: string;
  provisionDeRecours: number;
  provisionDeRecoursDefenseEtRecours: string;
  previsionDeRecoursDomVeh: string;
  cumulPrevisionDeRecours: string;
  nombreBlesses: number;
  nombreDeces: number;

  // Données ML de détection de fraude
  fraudDetection: FraudDetection;
}

export interface ApiResponseML {
  content: SinistreAvecML[];
  data: SinistreAvecML[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
  hasNext: boolean;
  hasPrevious: boolean;
  status: string;
  message: string;
}

export interface SinistreSearchCriteriaML {
  searchText?: string;
  natureSinistre?: string;
  typeSinistre?: string;
  libEtatSinistre?: string;
  gouvernorat?: string;
  anneeExercice?: number;
  usage?: string;
  numContrat?: string;
  compagnieAdverse?: string;
  fraudOnly?: boolean; // Nouveau filtre pour les cas de fraude uniquement
  riskLevel?: string;   // Nouveau filtre par niveau de risque
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface FraudPredictionRequest {
  sinistreData: {
    numSinistre: string;
    montantEvaluation?: number;
    natureSinistre?: string;
    typeSinistre?: string;
    lieuAccident?: string;
    compagnieAdverse?: string;
    dateDeclaration?: string;
    dateSurvenance?: string;
    totalReglement?: number;
    provisionDeRecours?: number;
    ageSinistreEnJours?: number;
  };
  contractData?: {
    numContrat: string;
    usage?: string;
    gouvernorat?: string;
  };
}

export interface FraudPredictionResponse {
  isFraud: boolean;
  confidence: number;
  riskLevel: string;
  reason: string;
  riskFactors: string[];
  recommendation: string;
}

export interface FraudStatistics {
  totalSinistres: number;
  fraudulentCount: number;
  fraudPercentage: number;
  highRiskCount: number;
  mediumRiskCount: number;
  totalFraudAmount: number;
  averageFraudAmount: number;
}

@Injectable({
  providedIn: 'root'
})
export class SinistreServiceAvecML {

  private readonly API_BASE_URL = 'http://localhost:9099/api/v1/sinistres';

  constructor(
    private http: HttpClient,
    private keycloakService: any // Remplacez par votre service Keycloak
  ) {}

  // ✅ MÉTHODE POUR OBTENIR LES HEADERS AVEC TOKEN KEYCLOAK
  private async getAuthHeaders(): Promise<HttpHeaders> {
    try {
      const token = await this.keycloakService.getToken();

      return new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json'
      });

    } catch (error) {
      console.error('❌ Erreur lors de la récupération du token:', error);

      // Headers sans authentification en cas d'erreur
      return new HttpHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      });
    }
  }

  // ✅ MÉTHODE PRINCIPALE - RÉCUPÉRER TOUS LES SINISTRES AVEC DÉTECTION ML
  async getAllSinistresWithML(params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  } = {}): Promise<ApiResponseML> {

    try {
      console.log('🔍 Service ML: Récupération des sinistres avec détection de fraude', params);

      const headers = await this.getAuthHeaders();

      let httpParams = new HttpParams()
        .set('page', (params.page || 0).toString())
        .set('size', (params.size || 20).toString())
        .set('sortBy', params.sortBy || 'dateDeclaration')
        .set('sortDirection', params.sortDirection || 'desc');

      const response = await this.http.get<ApiResponseML>(
        `${this.API_BASE_URL}/all`,
        { headers, params: httpParams }
      ).toPromise();

      if (!response) {
        throw new Error('Aucune réponse du serveur');
      }

      console.log('✅ Service ML: Sinistres récupérés avec analyse de fraude', {
        total: response.totalElements,
        page: response.currentPage,
        elements: response.content?.length || response.data?.length || 0,
        fraudulent: this.countFraudulentSinistres(response.content || response.data || [])
      });

      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de la récupération des sinistres:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODE DE RECHERCHE AVANCÉE AVEC ML
  async searchSinistresWithML(criteria: SinistreSearchCriteriaML): Promise<ApiResponseML> {

    try {
      console.log('🔍 Service ML: Recherche avancée des sinistres avec ML', criteria);

      const headers = await this.getAuthHeaders();

      const response = await this.http.post<ApiResponseML>(
        `${this.API_BASE_URL}/search`,
        criteria,
        { headers }
      ).toPromise();

      if (!response) {
        throw new Error('Aucune réponse du serveur pour la recherche');
      }

      console.log('✅ Service ML: Recherche terminée avec succès', {
        resultats: response.totalElements,
        fraudulent: this.countFraudulentSinistres(response.content || response.data || []),
        criteres: criteria
      });

      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de la recherche:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODE SPÉCIALISÉE POUR L'ANALYSE DE FRAUDE ML
  async analyzeFraudSinistre(request: FraudPredictionRequest): Promise<FraudPredictionResponse> {

    try {
      console.log('🤖 Service ML: Analyse de fraude pour sinistre', request.sinistreData.numSinistre);

      const headers = await this.getAuthHeaders();

      const response = await this.http.post<FraudPredictionResponse>(
        `${this.API_BASE_URL}/analyze-fraud`,
        request,
        { headers }
      ).toPromise();

      if (!response) {
        throw new Error('Aucune réponse de l\'analyse de fraude');
      }

      console.log('✅ Service ML: Analyse de fraude terminée', {
        sinistre: request.sinistreData.numSinistre,
        fraude: response.isFraud,
        confiance: Math.round(response.confidence * 100) + '%',
        niveau: response.riskLevel
      });

      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de l\'analyse de fraude:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODE POUR RÉCUPÉRER LES STATISTIQUES DE FRAUDE
  async getFraudStatistics(): Promise<FraudStatistics> {

    try {
      console.log('📊 Service ML: Récupération des statistiques de fraude');

      const headers = await this.getAuthHeaders();

      const response = await this.http.get<FraudStatistics>(
        `${this.API_BASE_URL}/fraud-statistics`,
        { headers }
      ).toPromise();

      if (!response) {
        throw new Error('Impossible de récupérer les statistiques de fraude');
      }

      console.log('✅ Service ML: Statistiques de fraude récupérées', {
        total: response.totalSinistres,
        fraudulent: response.fraudulentCount,
        pourcentage: response.fraudPercentage + '%'
      });

      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de la récupération des statistiques:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODE POUR RÉCUPÉRER UN SINISTRE SPÉCIFIQUE AVEC ML
  async getSinistreByIdWithML(numSinistre: string): Promise<SinistreAvecML> {

    try {
      console.log('🔍 Service ML: Récupération du sinistre avec ML', numSinistre);

      const headers = await this.getAuthHeaders();

      const response = await this.http.get<SinistreAvecML>(
        `${this.API_BASE_URL}/${numSinistre}`,
        { headers }
      ).toPromise();

      if (!response) {
        throw new Error(`Sinistre ${numSinistre} non trouvé`);
      }

      console.log('✅ Service ML: Sinistre récupéré avec analyse ML', {
        sinistre: numSinistre,
        fraude: response.fraudDetection?.isFraud || false,
        score: response.fraudDetection?.fraudScore || 0
      });

      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de la récupération du sinistre:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODE POUR ANALYSER TOUS LES SINISTRES EN LOT
  async analyzeBatchFraud(sinistres: string[]): Promise<Map<string, FraudPredictionResponse>> {

    try {
      console.log('🤖 Service ML: Analyse de fraude en lot', { count: sinistres.length });

      const results = new Map<string, FraudPredictionResponse>();

      // Traitement par lots de 10 pour éviter la surcharge
      const batchSize = 10;
      for (let i = 0; i < sinistres.length; i += batchSize) {
        const batch = sinistres.slice(i, i + batchSize);

        const batchPromises = batch.map(async (numSinistre) => {
          try {
            const request: FraudPredictionRequest = {
              sinistreData: { numSinistre }
            };

            const result = await this.analyzeFraudSinistre(request);
            results.set(numSinistre, result);

          } catch (error) {
            console.error(`❌ Erreur analyse ML pour ${numSinistre}:`, error);
            // Continuer avec les autres même en cas d'erreur
          }
        });

        await Promise.all(batchPromises);

        // Pause entre les lots pour éviter la surcharge
        if (i + batchSize < sinistres.length) {
          await new Promise(resolve => setTimeout(resolve, 100));
        }
      }

      console.log('✅ Service ML: Analyse en lot terminée', {
        traités: results.size,
        total: sinistres.length,
        fraudulent: Array.from(results.values()).filter(r => r.isFraud).length
      });

      return results;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de l\'analyse en lot:', error);
      throw new Error('Erreur lors de l\'analyse en lot: ' + error.message);
    }
  }

  // ✅ MÉTHODE POUR FILTRER LES SINISTRES PAR STATUT DE FRAUDE
  async getSinistresByFraudStatus(fraudStatus: 'all' | 'fraud' | 'safe' | 'high-risk' | 'medium-risk', params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  } = {}): Promise<ApiResponseML> {

    try {
      console.log('🔍 Service ML: Filtrage par statut de fraude', { fraudStatus, params });

      const criteria: SinistreSearchCriteriaML = {
        ...params,
        fraudOnly: fraudStatus === 'fraud',
        riskLevel: fraudStatus === 'high-risk' ? 'HIGH' :
          fraudStatus === 'medium-risk' ? 'MEDIUM' : undefined
      };

      return await this.searchSinistresWithML(criteria);

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors du filtrage par fraude:', error);
      throw new Error('Erreur lors du filtrage: ' + error.message);
    }
  }

  // ✅ MÉTHODE DE TEST DE CONNEXION AVEC ML
  async testConnexionML(): Promise<any> {

    try {
      console.log('🔧 Service ML: Test de connexion API avec ML');

      const headers = await this.getAuthHeaders();

      const response = await this.http.get<any>(
        `${this.API_BASE_URL}/test`,
        { headers }
      ).toPromise();

      if (!response) {
        throw new Error('Pas de réponse du serveur de test');
      }

      console.log('✅ Service ML: Test de connexion réussi', {
        status: response.status,
        mlEnabled: response.mlEnabled,
        fraudModelVersion: response.fraudModelVersion
      });

      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Test de connexion échoué:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODE DE VÉRIFICATION DE SANTÉ AVEC ML
  async healthCheckML(): Promise<string> {

    try {
      console.log('🏥 Service ML: Vérification de santé avec ML');

      const headers = await this.getAuthHeaders();

      const response = await this.http.get(
        `${this.API_BASE_URL}/health`,
        { headers, responseType: 'text' }
      ).toPromise();

      if (!response) {
        throw new Error('Pas de réponse du health check');
      }

      console.log('✅ Service ML: Health check réussi', response);
      return response;

    } catch (error: any) {
      console.error('❌ Service ML: Health check échoué:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  // ✅ MÉTHODES UTILITAIRES

  /**
   * Compte le nombre de sinistres frauduleux dans une liste
   */
  private countFraudulentSinistres(sinistres: SinistreAvecML[]): number {
    return sinistres.filter(s => s.fraudDetection?.isFraud).length;
  }

  /**
   * Extrait le message d'erreur approprié
   */
  private extractErrorMessage(error: any): string {
    if (error?.error?.message) {
      return error.error.message;
    }

    if (error?.error?.error) {
      return error.error.error;
    }

    if (error?.message) {
      return error.message;
    }

    if (error?.status === 0) {
      return 'Impossible de se connecter au serveur. Vérifiez que le backend avec ML est démarré.';
    }

    if (error?.status === 401) {
      return 'Non autorisé. Veuillez vous reconnecter.';
    }

    if (error?.status === 403) {
      return 'Accès interdit. Vous n\'avez pas les permissions nécessaires.';
    }

    if (error?.status === 404) {
      return 'Ressource non trouvée.';
    }

    if (error?.status === 500) {
      return 'Erreur interne du serveur ML. Contactez l\'administrateur.';
    }

    return 'Une erreur inattendue s\'est produite lors de l\'analyse ML.';
  }

  /**
   * Formate un montant en dinars tunisiens
   */
  formatMontant(montant: number | null): string {
    if (montant === null || montant === undefined) return '0,00 DT';

    return new Intl.NumberFormat('fr-TN', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 2
    }).format(montant).replace('TND', 'DT');
  }

  /**
   * Formate une date
   */
  formatDate(dateStr: string | Date): string {
    if (!dateStr) return 'Non défini';

    try {
      const date = typeof dateStr === 'string' ? new Date(dateStr) : dateStr;
      return date.toLocaleDateString('fr-FR');
    } catch {
      return 'Date invalide';
    }
  }

  /**
   * Formate un nombre
   */
  formatNombre(nombre: number | null): string {
    if (nombre === null || nombre === undefined) return '0';
    return new Intl.NumberFormat('fr-FR').format(nombre);
  }

  /**
   * Détermine la classe CSS selon le niveau de fraude
   */
  getFraudClass(fraudDetection: FraudDetection): string {
    if (!fraudDetection.isFraud) return 'fraud-safe';

    if (fraudDetection.confidence > 0.8) return 'fraud-critical';
    if (fraudDetection.confidence > 0.6) return 'fraud-high';
    if (fraudDetection.confidence > 0.4) return 'fraud-medium';
    return 'fraud-low';
  }

  /**
   * Obtient l'icône appropriée pour le niveau de fraude
   */
  getFraudIcon(fraudDetection: FraudDetection): string {
    if (!fraudDetection.isFraud) return 'fas fa-check-circle';

    if (fraudDetection.confidence > 0.8) return 'fas fa-exclamation-triangle';
    if (fraudDetection.confidence > 0.6) return 'fas fa-exclamation-circle';
    if (fraudDetection.confidence > 0.4) return 'fas fa-info-circle';
    return 'fas fa-question-circle';
  }

  /**
   * Obtient la couleur appropriée pour le niveau de fraude
   */
  getFraudColor(fraudDetection: FraudDetection): string {
    if (!fraudDetection.isFraud) return '#10b981'; // Vert

    if (fraudDetection.confidence > 0.8) return '#dc2626'; // Rouge
    if (fraudDetection.confidence > 0.6) return '#f59e0b'; // Orange
    if (fraudDetection.confidence > 0.4) return '#3b82f6'; // Bleu
    return '#6b7280'; // Gris
  }

  // ✅ MÉTHODES POUR L'EXPORT AVEC DONNÉES ML

  /**
   * Exporter les données avec informations ML en CSV
   */
  async exportToCsvWithML(criteria?: SinistreSearchCriteriaML): Promise<Blob> {

    try {
      console.log('📊 Service ML: Export des données avec ML en CSV');

      // Récupérer toutes les données correspondant aux critères
      const searchCriteria = {
        ...criteria,
        page: 0,
        size: 10000 // Grande taille pour récupérer toutes les données
      };

      const response = await this.searchSinistresWithML(searchCriteria);

      // Convertir en CSV avec données ML
      const csvContent = this.convertToCsvWithML(response.content || response.data || []);
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });

      console.log('✅ Service ML: Export CSV avec ML terminé');
      return blob;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de l\'export CSV:', error);

      const errorMessage = this.extractErrorMessage(error);
      throw new Error(errorMessage);
    }
  }

  /**
   * Convertit les données avec ML en format CSV
   */
  private convertToCsvWithML(data: SinistreAvecML[]): string {
    if (!data || data.length === 0) return '';

    // En-têtes CSV avec colonnes ML
    const headers = [
      'Numéro Sinistre', 'Année Exercice', 'Numéro Contrat', 'Date Déclaration',
      'Nature Sinistre', 'Type Sinistre', 'État Sinistre', 'Montant Évaluation',
      'Total Règlement', 'Lieu Accident', 'Gouvernorat', 'Compagnie Adverse',
      'Usage', 'Priorité', 'Âge (jours)',
      // Colonnes ML
      'Fraude Détectée', 'Score Fraude (%)', 'Niveau Risque', 'Raison Fraude',
      'Confiance ML (%)', 'Recommandation'
    ];

    // Données CSV avec informations ML
    const rows = data.map(sinistre => [
      sinistre.numSinistre,
      sinistre.anneeExercice,
      sinistre.numContrat,
      sinistre.dateDeclaration,
      sinistre.natureSinistre,
      sinistre.typeSinistre,
      sinistre.libEtatSinistre,
      sinistre.montantEvaluationBrut,
      sinistre.totalReglementBrut,
      sinistre.lieuAccident,
      sinistre.gouvernorat,
      sinistre.compagnieAdverse,
      sinistre.usage,
      sinistre.priorite,
      sinistre.ageSinistreEnJours,
      // Données ML
      sinistre.fraudDetection?.isFraud ? 'OUI' : 'NON',
      sinistre.fraudDetection?.fraudScore || 0,
      sinistre.fraudDetection?.riskLevel || 'UNKNOWN',
      sinistre.fraudDetection?.reason || 'Aucune analyse',
      Math.round((sinistre.fraudDetection?.confidence || 0) * 100),
      sinistre.fraudDetection?.recommendation || 'Aucune recommandation'
    ]);

    // Combiner en-têtes et données
    const csvArray = [headers, ...rows];

    // Convertir en chaîne CSV
    return csvArray.map(row =>
      row.map(field => `"${String(field || '').replace(/"/g, '""')}"`).join(',')
    ).join('\n');
  }

  // ✅ MÉTHODES POUR LA GESTION DES ALERTES ML

  /**
   * Obtient les alertes de fraude en temps réel
   */
  async getFraudAlerts(limit: number = 10): Promise<any[]> {

    try {
      console.log('🚨 Service ML: Récupération des alertes de fraude');

      const headers = await this.getAuthHeaders();

      let httpParams = new HttpParams()
        .set('limit', limit.toString())
        .set('fraudOnly', 'true')
        .set('sortBy', 'dateDeclaration')
        .set('sortDirection', 'desc');

      const response = await this.http.get<ApiResponseML>(
        `${this.API_BASE_URL}/all`,
        { headers, params: httpParams }
      ).toPromise();

      if (!response) {
        throw new Error('Impossible de récupérer les alertes');
      }

      const alerts = (response.content || response.data || [])
        .filter(s => s.fraudDetection?.isFraud)
        .slice(0, limit)
        .map(s => ({
          id: s.numSinistre,
          title: `Fraude détectée - Sinistre ${s.numSinistre}`,
          message: s.fraudDetection.reason,
          score: s.fraudDetection.fraudScore,
          riskLevel: s.fraudDetection.riskLevel,
          timestamp: new Date(s.dateDeclaration),
          sinistre: s
        }));

      console.log('✅ Service ML: Alertes de fraude récupérées', { count: alerts.length });

      return alerts;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors de la récupération des alertes:', error);
      throw new Error('Erreur lors de la récupération des alertes: ' + error.message);
    }
  }

  /**
   * Marque une alerte comme traitée
   */
  async markAlertAsProcessed(sinistreId: string): Promise<boolean> {

    try {
      console.log('✅ Service ML: Marquage alerte comme traitée', sinistreId);

      // Implémentez la logique selon vos besoins
      // Par exemple, mettre à jour un statut dans la base de données

      return true;

    } catch (error: any) {
      console.error('❌ Service ML: Erreur lors du marquage de l\'alerte:', error);
      return false;
    }
  }
}

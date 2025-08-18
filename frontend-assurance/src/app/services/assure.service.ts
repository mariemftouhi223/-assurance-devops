
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { KeycloakService } from 'app/services/keycloak/keycloak.service';

export interface Assure {
  annee?: number;
  anneeExerciceProd?: number;
  anneeExercice?: number;
  numContrat: number;
  effetContrat?: string | Date;
  validiteDu?: string | Date;
  validiteAu?: string | Date;
  prochainTerme?: string | Date;
  codeIntermediaire?: number;
  dateNaissance?: string | Date;
  sexe?: string;
  ville?: string;
  codePostal?: number;
  immatriculationVehicule?: string;
  premiereMiseCirculation?: string | Date;
  leasing?: string;
  personnePhysique?: number;
  personneMorale?: number;
  numQuittance?: number;
  rc?: number;
  dRec?: number;
  incendie?: number;
  vol?: number;
  dommagesAuVehicule?: number;
  dommagesEtCollision?: number;
  brisDeGlaces?: number;
  pta?: number;
  individuelleAccident?: number;
  catastropheNaturelle?: number;
  emeuteMouvementPopulaire?: number;
  volRadioCassette?: number;
  assistanceEtCarglass?: number;
  carglass?: number;
  totalTaxe?: number;
  frais?: number;
  totalPrimeNette?: number;
  capitaleInc?: number;
  capitaleVol?: number;
  capitaleDv?: number;
  valeurCatalogue?: number;
  valeurVenale?: number;
  usage?: string;
  marqueVehicule?: string;
  classeAssure?: number;
  // ML optionnel (si tu l’utilises déjà)
  fraudDetection?: {
    prediction: { isFraud: boolean; fraudProbability: number; };
    fraudScore: number;
    riskLevel: 'CRITICAL'|'HIGH'|'MEDIUM'|'LOW'|'NORMAL';
    reason: string;
    alertIcon: string;
    alertColor: string;
  };
}


export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;    // page actuelle
  size: number;      // taille page
  first?: boolean;
  last?: boolean;
  numberOfElements?: number;
  empty?: boolean;
}

export interface AssureSearchCriteria {
  numContrat?: number;
  annee?: number;
  ville?: string;
  marqueVehicule?: string;
  immatriculationVehicule?: string;
  dateDebutValidite?: string;
  dateFinValidite?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AssureService {
  private apiUrl = 'http://localhost:9099/api/v1/assures';

  constructor(
    private http: HttpClient,
    private keycloakService: KeycloakService
  ) {
    console.log('🔧 AssureService initialisé avec Keycloak');
  }

  getAssures(token: string, page: number = 0, size: number = 20): Observable<PagedResponse<Assure>> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PagedResponse<Assure>>(this.apiUrl, { headers, params }).pipe(
      retry(1),
      catchError(error => this.handleError(error))
    );
  }

  async getAssuresWithKeycloak(page: number = 0, size: number = 20): Promise<Observable<PagedResponse<Assure>>> {
    try {
      const token = await this.keycloakService.getToken();
      return this.getAssures(token, page, size);
    } catch (error) {
      console.error('❌ Erreur récupération token:', error);
      throw new Error('Impossible de récupérer le token d\'authentification');
    }
  }

  getAssureByNumContrat(token: string, numContrat: number): Observable<Assure> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get<Assure>(`${this.apiUrl}/${numContrat}`, { headers }).pipe(
      catchError(error => this.handleError(error))
    );
  }

  searchAssures(token: string, criteria: AssureSearchCriteria, page: number = 0, size: number = 20): Observable<PagedResponse<Assure>> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (criteria.numContrat) params = params.set('numContrat', criteria.numContrat.toString());
    if (criteria.annee) params = params.set('annee', criteria.annee.toString());
    if (criteria.ville) params = params.set('ville', criteria.ville);
    if (criteria.marqueVehicule) params = params.set('marqueVehicule', criteria.marqueVehicule);
    if (criteria.immatriculationVehicule) params = params.set('immatriculationVehicule', criteria.immatriculationVehicule);

    return this.http.get<PagedResponse<Assure>>(`${this.apiUrl}/search`, { headers, params }).pipe(
      catchError(error => this.handleError(error))
    );
  }

  async testConnectionWithKeycloak(): Promise<Observable<any>> {
    try {
      const token = await this.keycloakService.getToken();
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json'
      });
      return this.http.get(`${this.apiUrl}?page=0&size=1`, { headers }).pipe(
        catchError(error => this.handleError(error))
      );
    } catch (error) {
      console.error('❌ Erreur test connexion:', error);
      throw error;
    }
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur inconnue s\'est produite';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Erreur client: ${error.error.message}`;
    } else {
      switch (error.status) {
        case 0:
          errorMessage = 'Impossible de contacter le serveur. Vérifiez que Spring Boot est démarré sur le port 9099.';
          break;
        case 400:
          errorMessage = 'Requête invalide. Vérifiez les paramètres envoyés.';
          break;
        case 401:
          errorMessage = 'Token d\'authentification invalide ou expiré. Veuillez vous reconnecter.';
          break;
        case 403:
          errorMessage = 'Accès interdit. Vérifiez vos permissions.';
          break;
        case 404:
          errorMessage = 'Endpoint API non trouvé. Vérifiez l\'URL de l\'API.';
          break;
        case 500:
          errorMessage = 'Erreur interne du serveur. Problème de base de données ou de mapping.';
          break;
        case 503:
          errorMessage = 'Service temporairement indisponible. Réessayez plus tard.';
          break;
        default:
          errorMessage = `Erreur serveur: ${error.status} - ${error.message}`;
      }
    }
    console.error('❌ Erreur AssureService:', {
      status: error.status,
      message: error.message,
      url: error.url,
      error: error.error
    });
    return throwError(() => new Error(errorMessage));
  }

  async diagnostic(): Promise<any> {
    const diagnostic = {
      timestamp: new Date().toISOString(),
      apiUrl: this.apiUrl,
      keycloak: {
        isLoggedIn: this.keycloakService.isLoggedIn(),
        hasToken: !!this.keycloakService.keycloak.token,
        tokenLength: this.keycloakService.keycloak.token?.length || 0,
        authenticated: this.keycloakService.keycloak.authenticated,
        profile: this.keycloakService.profile
      },
      browser: {
        userAgent: navigator.userAgent,
        url: window.location.href
      }
    };
    console.log('🔍 Diagnostic AssureService:', diagnostic);
    return diagnostic;
  }

  // Create
  createAssure(assure: Assure, token: string) {
    return this.http.post<Assure>(
      `${this.apiUrl}/add`,
      assure,
      { headers: { Authorization: `Bearer ${token}`, 'Content-Type':'application/json', 'Accept':'application/json' } }
    );
  }

// Update
  updateAssure(numContrat: number, assure: Assure, token: string) {
    return this.http.put<Assure>(
      `${this.apiUrl}/update/${numContrat}`,
      assure,
      { headers: { Authorization: `Bearer ${token}`, 'Content-Type':'application/json', 'Accept':'application/json' } }
    );
  }

// Delete
  deleteAssure(numContrat: number, token: string) {
    return this.http.delete(
      `${this.apiUrl}/delete/${numContrat}`,
      {
        headers: new HttpHeaders({
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/json'
        })
      }
    );
  }




}


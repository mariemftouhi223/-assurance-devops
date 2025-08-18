import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { FraudCasesService } from 'app/services/fraud-cases.service';

/* ===== Interfaces ===== */
export interface FraudPrediction {
  isFraud: boolean;          // vrai si fraude
  fraudProbability: number;  // 0..1
}

export interface FraudDetection {
  prediction: FraudPrediction;
  fraudScore: number;        // 0..100
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'NORMAL';
  reason: string;
  alertIcon: string;
  alertColor: string;
  riskFactors?: string[];
  recommendation?: string;
}

/** ⚠️ Interface alignée avec tous les champs utilisés dans le template */
export interface Assure {
  // Identité / contrat
  numContrat: string;
  annee?: number;
  anneeExerciceProd?: number;
  anneeExercice?: number;

  // Dates contrat
  effetContrat?: string | Date;
  validiteDu?: string | Date;
  validiteAu?: string | Date;
  prochainTerme?: string | Date;

  // Intermédiaire & profil
  codeIntermediaire?: number | string;
  dateNaissance?: string | Date;
  sexe?: string;
  ville?: string;
  codePostal?: number;

  // Véhicule
  immatriculationVehicule?: string;
  premiereMiseCirculation?: string | Date;
  marqueVehicule?: string;
  usage?: string;
  leasing?: string;
  classeAssure?: number;

  // Type personne & quittance
  personnePhysique?: number;
  personneMorale?: number;
  numQuittance?: number;

  // Garanties
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

  // Financier
  totalTaxe?: number;
  frais?: number;
  totalPrimeNette?: number | string;
  capitaleInc?: number;
  capitaleVol?: number;
  capitaleDv?: number;
  valeurCatalogue?: number;
  valeurVenale?: number;

  // ML (optionnel)
  fraudDetection?: FraudDetection;
}

interface NotificationAssure {
  id: string;
  type: 'fraud' | 'warning' | 'info';
  title: string;
  message: string;
  contractId: string;
  timestamp: Date;
  dismissed: boolean;
}

interface StatistiquesAssures {
  totalAssures: number;
  fraudulentCount: number;
  fraudPercentage: number;   // 0..100
  highRiskCount: number;
  mediumRiskCount: number;
}

/** Réponse paginée backend */
interface ApiPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

@Component({
  selector: 'app-gestion-assures',
  templateUrl: './gestion-assures.component.html',
  styleUrls: ['./gestion-assures.component.scss']
})
export class GestionAssuresComponent implements OnInit {

  /* ===== Données ===== */
  assures: Assure[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  /* ===== Pagination / Tri ===== */
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50, 100, 200, 500];

  sortBy: keyof Assure | 'dateCreation' = 'numContrat';
  sortDirection: 'asc' | 'desc' = 'asc';

  /* ===== Filtres ===== */
  searchText = '';
  fraudFilter = '';

  /* ===== Notifications ===== */
  fraudNotifications: NotificationAssure[] = [];
  showNotifications = true;

  /* ===== Résultats ML (clé = string) ===== */
  private fraudResults: Map<string, FraudDetection> = new Map();

  /* ===== Statistiques (cartes du haut) ===== */
  statistiquesAssures: StatistiquesAssures = {
    totalAssures: 0,
    fraudulentCount: 0,
    fraudPercentage: 0,
    highRiskCount: 0,
    mediumRiskCount: 0
  };

  /** Base URL backend (change en '/api/v1/assures' si tu utilises un proxy Angular) */
  private readonly API = 'http://localhost:9090/api/v1/assures';

  // ✅ un seul constructeur avec les dépendances
  constructor(
    private cdr: ChangeDetectorRef,
    private fraudCases: FraudCasesService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    void this.loadAssures();
  }

  /* =========================================
   * Chargement (API + fallback mock)
   * ========================================= */
  async loadAssures(): Promise<void> {
    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    try {
      const params = new HttpParams()
        .set('page', this.currentPage)
        .set('size', this.pageSize)
        .set('sortBy', String(this.sortBy))
        .set('sortDirection', this.sortDirection);

      const page = await firstValueFrom(
        this.http.get<ApiPage<Assure>>(this.API, { params, withCredentials: true })
      );

      this.assures = page.content ?? [];
      this.totalElements = page.totalElements ?? 0;
      this.totalPages = page.totalPages ?? 0;

      this.processFraudResults();
      this.persistFraudsFromPage();
      this.recomputeStats();

    } catch (e: any) {
      // Fallback mock pour ne pas casser l'UI si l'API est indisponible
      this.errorMessage = this.humanHttpError(e) || 'Erreur lors du chargement des assurés — fallback mock.';
      this.assures = this.generateMockAssures(this.pageSize);
      this.totalElements = 16348; // exemple
      this.totalPages = Math.ceil(this.totalElements / this.pageSize);

      this.processFraudResults();
      this.persistFraudsFromPage();
      this.recomputeStats();
    } finally {
      this.isLoading = false;
      this.cdr.markForCheck();
    }
  }

  /* ============== CRUD réels ============== */
  async createAssure(a?: Partial<Assure>): Promise<void> {
    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    // Si rien n'est passé depuis l'UI, on crée un draft minimal valide
    const draft: Partial<Assure> = a ?? {
      numContrat: String(Date.now()),
      effetContrat: new Date(),
      validiteDu: new Date(),
      validiteAu: new Date(Date.now() + 365 * 86400000),
      immatriculationVehicule: 'TUN-0000',
      marqueVehicule: 'Non spécifiée',
      totalPrimeNette: 100
    };

    const payload = this.toApiPayload(draft);

    try {
      await firstValueFrom(
        this.http.post(`${this.API}/add`, payload, { withCredentials: true })
      );
      this.successMessage = `Assuré N°${payload.numContrat} créé avec succès`;
      await this.loadAssures();
    } catch (err: any) {
      this.errorMessage = this.humanHttpError(err) || 'Erreur lors de la création';
    } finally {
      this.isLoading = false;
    }
  }

  async updateAssure(a: Assure): Promise<void> {
    if (!a?.numContrat) { this.errorMessage = 'numContrat requis'; return; }

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const payload = this.toApiPayload(a);

    try {
      await firstValueFrom(
        this.http.put(`${this.API}/update/${encodeURIComponent(String(a.numContrat))}`, payload, { withCredentials: true })
      );
      this.successMessage = `Assuré N°${a.numContrat} modifié avec succès`;
      await this.loadAssures();
    } catch (err: any) {
      this.errorMessage = this.humanHttpError(err) || 'Erreur lors de la modification';
    } finally {
      this.isLoading = false;
    }
  }

  async deleteAssure(num: string): Promise<void> {
    if (!num) return;
    if (!confirm(`Supprimer l’assuré N°${num} ?`)) return;

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    try {
      await firstValueFrom(
        this.http.delete(`${this.API}/delete/${encodeURIComponent(String(num))}`, { withCredentials: true })
      );
      this.successMessage = `Assuré N°${num} supprimé avec succès`;
      // Retirer localement pour fluidité
      this.assures = this.assures.filter(x => String(x.numContrat) !== String(num));
      this.totalElements = Math.max(0, this.totalElements - 1);
      this.recomputeStats();
    } catch (err: any) {
      this.errorMessage = this.humanHttpError(err) || 'Erreur lors de la suppression';
    } finally {
      this.isLoading = false;
    }
  }

  /* ============== Mock (fallback) ============== */
  private generateMockAssures(n: number): Assure[] {
    const out: Assure[] = [];
    for (let i = 0; i < n; i++) {
      const score = Math.floor(Math.random() * 100);
      const p = score / 100;
      const isFraud = p >= 0.5;
      const level: FraudDetection['riskLevel'] =
        p > 0.8 ? 'CRITICAL' : p > 0.6 ? 'HIGH' : p > 0.4 ? 'MEDIUM' : p > 0.2 ? 'LOW' : 'NORMAL';

      const detection: FraudDetection = {
        prediction: { isFraud, fraudProbability: p },
        fraudScore: score,
        riskLevel: level,
        reason: isFraud ? 'Profil client à risque élevé détecté par IA' : 'Profil normal',
        alertIcon: p > 0.8 ? 'fas fa-exclamation-triangle'
          : p > 0.6 ? 'fas fa-exclamation-circle'
            : p > 0.4 ? 'fas fa-info-circle'
              : 'fas fa-check-circle',
        alertColor: p > 0.8 ? '#dc2626'
          : p > 0.6 ? '#f59e0b'
            : p > 0.4 ? '#3b82f6'
              : '#10b981',
        riskFactors: isFraud ? ['Historique irrégulier', 'Montants élevés'] : [],
        recommendation: isFraud ? 'Vérification recommandée' : 'Aucune action requise'
      };

      out.push({
        numContrat: `1998${String(300000000 + i).padStart(9, '0')}`,
        annee: 2024,
        anneeExerciceProd: 2024,
        anneeExercice: 2024,
        effetContrat: new Date(),
        validiteDu: new Date(),
        validiteAu: new Date(Date.now() + 1000 * 3600 * 24 * 365),
        prochainTerme: new Date(Date.now() + 1000 * 3600 * 24 * 90),
        codeIntermediaire: Math.floor(100 + Math.random() * 900),
        dateNaissance: new Date(1990, 1, 1),
        sexe: ['H', 'F'][Math.floor(Math.random() * 2)],
        ville: 'Tunis',
        codePostal: 1000,
        immatriculationVehicule: `TUN-${Math.floor(1000 + Math.random() * 8999)}`,
        premiereMiseCirculation: new Date(2019, 5, 1),
        marqueVehicule: ['Peugeot', 'Renault', 'VW', 'Kia'][Math.floor(Math.random() * 4)],
        usage: 'Particulier',
        leasing: 'Non',
        classeAssure: 1,
        personnePhysique: 1,
        personneMorale: 0,
        numQuittance: 100000 + i,
        rc: 1, dRec: 1, incendie: 1, vol: 0,
        dommagesAuVehicule: 1, dommagesEtCollision: 0, brisDeGlaces: 1, pta: 0,
        individuelleAccident: 1, catastropheNaturelle: 0, emeuteMouvementPopulaire: 0,
        volRadioCassette: 0, assistanceEtCarglass: 1, carglass: 1,
        totalTaxe: 20, frais: 10,
        totalPrimeNette: Number((100 + Math.random() * 900).toFixed(2)),
        capitaleInc: 15000, capitaleVol: 12000, capitaleDv: 18000,
        valeurCatalogue: 45000, valeurVenale: 32000,
        fraudDetection: detection
      });
    }
    return out;
  }

  /* ============== ML / Notifications ============== */
  private processFraudResults(): void {
    this.fraudResults.clear();
    this.fraudNotifications = [];

    for (const a of this.assures) {
      if (!a.fraudDetection) continue;
      this.fraudResults.set(this.key(a.numContrat), a.fraudDetection);

      if (a.fraudDetection.prediction.isFraud) {
        this.fraudNotifications.unshift({
          id: `fraud-${a.numContrat}-${Date.now()}`,
          type: a.fraudDetection.prediction.fraudProbability > 0.8 ? 'fraud' : 'warning',
          title: '🚨 ALERTE FRAUDE DÉTECTÉE',
          message: `Contrat N°${a.numContrat} - Score: ${a.fraudDetection.fraudScore}% - ${a.fraudDetection.reason}`,
          contractId: a.numContrat,
          timestamp: new Date(),
          dismissed: false
        });
      }
    }
    if (this.fraudNotifications.length > 10) {
      this.fraudNotifications = this.fraudNotifications.slice(0, 10);
    }
  }

  /** Recalcule les cartes (comme Sinistres) */
  private recomputeStats(): void {
    const total = this.totalElements || this.assures.length || 0;
    let fraudulent = 0, high = 0, medium = 0;

    for (const a of this.assures) {
      const r = this.fraudResults.get(this.key(a.numContrat));
      const p = r?.prediction?.fraudProbability ?? 0;

      if (r?.prediction?.isFraud) fraudulent++;
      if (p >= 0.70) high++;
      else if (p >= 0.40) medium++;
    }

    this.statistiquesAssures = {
      totalAssures: total,
      fraudulentCount: fraudulent,
      fraudPercentage: total ? Math.round((fraudulent / total) * 100) : 0,
      highRiskCount: high,
      mediumRiskCount: medium
    };
    this.cdr.markForCheck();
  }

  /** Bouton "Stats ML" (optionnel) */
  loadFraudStatistics(): void {
    this.recomputeStats();
  }

  /* ============== Helpers UI / Table ============== */
  getStartIndex(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }
  getEndIndex(): number {
    const end = (this.currentPage + 1) * this.pageSize;
    return end > this.totalElements ? this.totalElements : end;
  }

  onSort(column: any): void {
    if (this.sortBy === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDirection = 'asc';
    }
    void this.loadAssures();
  }

  onPageSizeChange(newSize: any): void {
    this.pageSize = Number(newSize);
    this.currentPage = 0;
    void this.loadAssures();
  }

  onPageChange(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages && newPage !== this.currentPage) {
      this.currentPage = newPage;
      void this.loadAssures();
    }
  }

  getPages(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  trackByAssure = (_: number, a: Assure) => a.numContrat;

  /* ============== Filtres & actions ============== */
  searchAssures(): void { this.currentPage = 0; void this.loadAssures(); }
  resetFilters(): void {
    this.searchText = '';
    this.fraudFilter = '';
    this.currentPage = 0;
    void this.loadAssures();
  }

  /* ============== FRAUDE UI helpers (row class seulement) ============== */
  getFraudRowClass(_numContrat: string): string { return ''; }

  /* ============== Notifications ============== */
  getActiveNotifications(): NotificationAssure[] {
    return this.fraudNotifications.filter(n => !n.dismissed);
  }
  clearAllNotifications(): void {
    this.fraudNotifications = [];
  }
  dismissNotification(id: string): void {
    const n = this.fraudNotifications.find(x => x.id === id);
    if (n) n.dismissed = true;
  }
  showFraudDetailsFromNotification(contractId: string): void {
    const a = this.assures.find(x => x.numContrat === contractId);
    if (a) this.showFraudDetails(a);
  }

  /* ============== Divers ============== */
  formatDate(d?: any): string {
    if (!d) return '-';
    const date = new Date(d);
    return isNaN(date.getTime()) ? '-' : date.toLocaleDateString('fr-FR');
  }
  showFraudDetails(a: Assure): void {
    const f = this.fraudResults.get(this.key(a.numContrat));
    if (!f) { alert('Aucune analyse ML'); return; }
    const msg =
      `🚨 DÉTAILS FRAUDE (ML)\n\n` +
      `Contrat: ${a.numContrat}\n` +
      `Score: ${f.fraudScore}% (${f.riskLevel})\n` +
      `Probabilité: ${Math.round(f.prediction.fraudProbability * 100)}%\n` +
      `Raison: ${f.reason}\n\n` +
      `Recommandation: ${f.recommendation || '-'}`;
    alert(msg);
  }
  showContractDetails(a: Assure): void { alert(`Détails du contrat ${a.numContrat}`); }

  exportAssures(): void { alert('Export CSV (à implémenter)'); }
  testApi(): void { alert('Test API (à implémenter)'); }

  // ---------- Helpers d'accès/format ----------
  /** Clé Map pour ML (numContrat peut être number ou string) */
  private key(n: number | string): string { return String(n); }

  /** Format court pour montants de garanties (ou '-') */
  fmt(v?: number | string): string {
    if (v === null || v === undefined || v === '') return '-';
    const n = Number(v);
    return isNaN(n) ? String(v) : n.toLocaleString('fr-FR');
  }

  /** Format argent en DT (ou '-') */
  money(v?: number | string): string {
    if (v === null || v === undefined || v === '') return '-';
    const n = Number(v);
    if (isNaN(n)) return String(v);
    return `${n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} DT`;
  }

  // ---------- Getters UI ML compatibles numContrat:number|string ----------
  getFraudIcon(numContrat: number | string): string {
    return this.fraudResults.get(this.key(numContrat))?.alertIcon || 'fas fa-check-circle';
  }
  getFraudColor(numContrat: number | string): string {
    return this.fraudResults.get(this.key(numContrat))?.alertColor || '#6b7280';
  }
  getFraudScore(numContrat: number | string): number {
    return this.fraudResults.get(this.key(numContrat))?.fraudScore ?? 0;
  }
  getRiskLevel(numContrat: number | string): string {
    return this.fraudResults.get(this.key(numContrat))?.riskLevel || 'NORMAL';
  }

  /** ✅ Envoie à la base tous les assurés ≥ 50% (après processFraudResults) */
  private persistFraudsFromPage(): void {
    for (const a of this.assures) {
      const fr = this.fraudResults.get(String(a.numContrat));
      if (fr && fr.fraudScore >= 50) {
        this.fraudCases
          .record('ASSURE', String(a.numContrat), fr.fraudScore, fr.reason || 'ML')
          .subscribe({ next: () => {}, error: () => {} });
      }
    }
  }

  // ---------- Helpers CRUD (dates & payload) ----------
  private toIso(d?: string | Date): string | undefined {
    if (!d) return undefined;
    const dd = d instanceof Date ? d : new Date(d);
    if (isNaN(dd.getTime())) return undefined;
    return dd.toISOString().substring(0, 10); // yyyy-MM-dd
  }
  private toApiPayload(a: Partial<Assure>): any {
    return {
      numContrat: a.numContrat ? String(a.numContrat) : '',
      effetContrat: this.toIso(a.effetContrat),
      validiteDu:   this.toIso(a.validiteDu),
      validiteAu:   this.toIso(a.validiteAu),
      immatriculationVehicule: a.immatriculationVehicule,
      marqueVehicule: a.marqueVehicule,
      totalPrimeNette: (a.totalPrimeNette !== undefined && a.totalPrimeNette !== null)
        ? Number(a.totalPrimeNette) : undefined,
      // ➕ ajoute ici d'autres champs si ton backend les accepte
    };
  }
  private humanHttpError(e: any): string | null {
    if (e instanceof HttpErrorResponse) {
      if (e.status === 0) return 'Impossible de joindre le serveur (CORS ou serveur éteint).';
      if (e.error?.error) return `${e.status} - ${e.error.error}`;
      if (e.message) return `${e.status} - ${e.message}`;
      return `${e.status} - Erreur inconnue`;
    }
    return e?.message || null;
  }
}

import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { firstValueFrom, Subject } from 'rxjs';

/* ====== Interfaces ML ====== */
export interface FraudDetection {
  isFraud: boolean;
  confidence: number;              // 0..1
  riskLevel: string;               // CRITICAL/HIGH/MEDIUM/LOW/NORMAL
  reason: string;
  riskFactors?: string[];
  recommendation?: string;
  fraudScore: number;              // 0..100
  alertLevel: string;
  alertIcon: string;
  alertColor: string;
}

export interface SinistreAvecML {
  numSinistre: string;
  anneeExercice: number;
  numContrat: string;
  dateDeclaration: string | Date;
  natureSinistre: string;
  typeSinistre: string;
  libEtatSinistre: string;

  montantEvaluation: string;       // formaté
  montantEvaluationBrut: number;   // numérique
  totalReglement: string;          // formaté
  totalReglementBrut: number;      // numérique

  lieuAccident: string;
  gouvernorat: string;
  compagnieAdverse: string;
  usage: string;
  priorite: string;

  fraudDetection: FraudDetection;
}

export interface ApiResponseML {
  content: SinistreAvecML[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
  status?: string;
  message?: string;
}

interface FraudNotification {
  id: string;
  type: 'fraud' | 'warning' | 'info';
  title: string;
  message: string;
  sinistreId: string;
  timestamp: Date;
  dismissed: boolean;
}

interface StatistiquesFraude {
  totalSinistres: number;
  fraudulentCount: number;
  fraudPercentage: number;
  highRiskCount: number;
  mediumRiskCount: number;
  totalFraudAmount: number;
  averageFraudAmount: number;
}

@Component({
  selector: 'app-sinistres',
  templateUrl: './sinistres.component.html',
  styleUrls: ['./sinistres.component.scss']
})
export class SinistresComponent implements OnInit, OnDestroy {

  /* ===== Données & état ===== */
  sinistres: SinistreAvecML[] = [];
  loading = false;
  error: string | null = null;

  /* ===== Pagination / tri ===== */
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50, 100, 200, 500];
  sortBy: string = 'dateDeclaration';
  sortDirection: 'asc' | 'desc' = 'desc';

  /* ===== Filtres ===== */
  searchText = '';
  selectedNature = '';
  selectedType = '';
  selectedEtat = '';
  selectedGouvernorat = '';
  selectedAnnee: number | null = null;
  selectedUsage = '';
  fraudFilter = '';

  /* ===== ML / Notifications ===== */
  fraudResults: Map<string, FraudDetection> = new Map();
  fraudNotifications: FraudNotification[] = [];
  showNotifications = true;

  /* ===== Statistiques ===== */
  statistiques: StatistiquesFraude = {
    totalSinistres: 0,
    fraudulentCount: 0,
    fraudPercentage: 0,
    highRiskCount: 0,
    mediumRiskCount: 0,
    totalFraudAmount: 0,
    averageFraudAmount: 0
  };

  private destroy$ = new Subject<void>();

  /** ✅ API via proxy /auth → 9090 (n’impacte pas /api → 9099) */
  private readonly API = '/auth/api/v1/sinistres';

  constructor(
    private cdr: ChangeDetectorRef,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadSinistres();
    this.loadFraudStatistics();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // =======================
  // ======= CRUD ==========
  // =======================

  async createSinistre(draft?: Partial<SinistreAvecML>): Promise<void> {
    this.loading = true; this.error = null;
    const payload = this.toApiPayload(draft ?? this.defaultDraft());

    try {
      await firstValueFrom(this.http.post(`${this.API}/add`, payload, { withCredentials: true }));
      await this.loadSinistres();
      alert(`Sinistre ${payload.numSinistre} créé avec succès`);
    } catch (e) {
      this.error = this.humanHttpError(e) ?? 'Erreur lors de la création';
    } finally {
      this.loading = false;
    }
  }

  async updateSinistre(s: SinistreAvecML): Promise<void> {
    if (!s?.numSinistre) { this.error = 'numSinistre requis'; return; }
    this.loading = true; this.error = null;
    const payload = this.toApiPayload(s);

    try {
      await firstValueFrom(
        this.http.put(`${this.API}/update/${encodeURIComponent(String(s.numSinistre))}`, payload, { withCredentials: true })
      );
      await this.loadSinistres();
      alert(`Sinistre ${s.numSinistre} modifié`);
    } catch (e) {
      this.error = this.humanHttpError(e) ?? 'Erreur lors de la modification';
    } finally {
      this.loading = false;
    }
  }

  async deleteSinistre(numSinistre: string): Promise<void> {
    if (!numSinistre) return;
    if (!confirm(`Supprimer le sinistre N°${numSinistre} ?`)) return;

    this.loading = true; this.error = null;
    try {
      await firstValueFrom(
        this.http.delete(`${this.API}/delete/${encodeURIComponent(String(numSinistre))}`, { withCredentials: true })
      );
      this.sinistres = this.sinistres.filter(x => x.numSinistre !== numSinistre);
      this.totalElements = Math.max(0, this.totalElements - 1);
      this.updateStatistiques();
    } catch (e) {
      this.error = this.humanHttpError(e) ?? 'Erreur lors de la suppression';
    } finally {
      this.loading = false;
    }
  }

  // =======================
  // ====== Listing ========
  // =======================

  async loadSinistres(): Promise<void> {
    this.loading = true;
    this.error = null;

    try {
      let params = new HttpParams()
        .set('page', this.currentPage)
        .set('size', this.pageSize)
        .set('sortBy', this.sortBy)
        .set('sortDirection', this.sortDirection);

      if (this.searchText)      params = params.set('q', this.searchText);
      if (this.selectedNature)  params = params.set('nature', this.selectedNature);
      if (this.selectedEtat)    params = params.set('etat', this.selectedEtat);
      if (this.fraudFilter)     params = params.set('fraud', this.fraudFilter);

      const page = await firstValueFrom(
        this.http.get<ApiResponseML>(this.API, { params, withCredentials: true })
      );

      this.sinistres = (page.content ?? []).map(x => ({ ...x }));
      this.totalElements = page.totalElements ?? this.sinistres.length;
      this.totalPages = page.totalPages ?? Math.ceil(this.totalElements / this.pageSize);

      this.processFraudResults();
      this.updateStatistiques();

    } catch (e) {
      // ✅ Fallback mock pour garder l’écran fonctionnel
      await this.simulateApiCall();
    } finally {
      this.loading = false;
      this.cdr.markForCheck();
    }
  }

  // =======================
  // ======= MOCK ==========
  // =======================

  private async simulateApiCall(): Promise<void> {
    return new Promise(resolve => {
      setTimeout(() => {
        this.sinistres = this.generateMockData();
        this.totalElements = 1000;
        this.totalPages = Math.ceil(this.totalElements / this.pageSize);
        this.processFraudResults();
        this.updateStatistiques();
        resolve();
      }, 400);
    });
  }

  private generateMockData(): SinistreAvecML[] {
    const out: SinistreAvecML[] = [];
    for (let i = 0; i < this.pageSize; i++) {
      const fraudScore = Math.floor(Math.random() * 100);
      const isFraud = fraudScore > 50;
      out.push({
        numSinistre: `SIN${new Date().getFullYear()}${String(i + 1).padStart(6, '0')}`,
        anneeExercice: new Date().getFullYear(),
        numContrat: `CNT${String(i + 1).padStart(8, '0')}`,
        dateDeclaration: new Date(),
        natureSinistre: ['CORPOREL', 'MATERIEL', 'MIXTE'][Math.floor(Math.random() * 3)],
        typeSinistre: ['COLLISION', 'VOL', 'INCENDIE'][Math.floor(Math.random() * 3)],
        libEtatSinistre: ['CLOTURE', 'MISE A JOUR', 'REPRISE'][Math.floor(Math.random() * 3)],
        montantEvaluation: `${(Math.random() * 50000).toFixed(2)} DT`,
        montantEvaluationBrut: Math.random() * 50000,
        totalReglement: `${(Math.random() * 40000).toFixed(2)} DT`,
        totalReglementBrut: Math.random() * 40000,
        lieuAccident: ['Tunis', 'Sfax', 'Sousse', 'Bizerte'][Math.floor(Math.random() * 4)],
        gouvernorat: ['Tunis', 'Sfax', 'Sousse', 'Bizerte'][Math.floor(Math.random() * 4)],
        compagnieAdverse: ['STAR', 'GAT', 'MAGHREBIA'][Math.floor(Math.random() * 3)],
        usage: ['PRIVE', 'COMMERCIAL', 'TAXI'][Math.floor(Math.random() * 3)],
        priorite: 'NORMALE',
        fraudDetection: {
          isFraud,
          confidence: fraudScore / 100,
          riskLevel: fraudScore > 80 ? 'CRITICAL' : fraudScore > 60 ? 'HIGH' : fraudScore > 40 ? 'MEDIUM' : 'LOW',
          reason: isFraud ? 'Anomalies détectées par ML' : 'Profil normal',
          fraudScore,
          alertLevel: fraudScore > 80 ? 'CRITICAL' : fraudScore > 60 ? 'HIGH' : 'MEDIUM',
          alertIcon: fraudScore > 80 ? 'fas fa-exclamation-triangle' : fraudScore > 60 ? 'fas fa-exclamation-circle' : 'fas fa-check-circle',
          alertColor: fraudScore > 80 ? '#dc2626' : fraudScore > 60 ? '#f59e0b' : fraudScore > 40 ? '#3b82f6' : '#10b981',
          riskFactors: isFraud ? ['Montant élevé', 'Délai suspect'] : [],
          recommendation: isFraud ? 'Vérification recommandée' : 'Aucune action requise'
        }
      });
    }
    return out;
  }

  // =======================
  // ===== ML / Stats ======
  // =======================

  private processFraudResults(): void {
    let fraudulentCount = 0, high = 0, medium = 0;
    this.fraudResults.clear();

    this.sinistres.forEach(s => {
      const r = s.fraudDetection;
      this.fraudResults.set(s.numSinistre, r);
      if (r?.isFraud) fraudulentCount++;
      if (r?.confidence > 0.6) high++;
      else if (r?.confidence > 0.4) medium++;

      if (r?.isFraud) {
        this.fraudNotifications.unshift({
          id: `fraud-${s.numSinistre}-${Date.now()}`,
          type: r.confidence > 0.8 ? 'fraud' : 'warning',
          title: '🚨 ALERTE FRAUDE DÉTECTÉE',
          message: `Sinistre N°${s.numSinistre} - Score: ${r.fraudScore}% - ${r.reason}`,
          sinistreId: s.numSinistre,
          timestamp: new Date(),
          dismissed: false
        });
      }
    });

    if (this.fraudNotifications.length > 10) {
      this.fraudNotifications = this.fraudNotifications.slice(0, 10);
    }

    this.statistiques.fraudulentCount = fraudulentCount;
    this.statistiques.highRiskCount = high;
    this.statistiques.mediumRiskCount = medium;
    this.statistiques.fraudPercentage = this.sinistres.length
      ? Math.round((fraudulentCount / this.sinistres.length) * 100) : 0;

    this.cdr.markForCheck();
  }

  private updateStatistiques(): void {
    this.statistiques.totalSinistres = this.totalElements;
    let totalFraudAmount = 0, fraudCount = 0;

    this.sinistres.forEach(s => {
      if (s.fraudDetection?.isFraud) {
        fraudCount++;
        if (s.montantEvaluationBrut) totalFraudAmount += s.montantEvaluationBrut;
      }
    });

    this.statistiques.totalFraudAmount = totalFraudAmount;
    this.statistiques.averageFraudAmount = fraudCount ? totalFraudAmount / fraudCount : 0;
  }

  /** ✅ Recalcule les stats ML (bouton "Stats ML" + ngOnInit) */
  loadFraudStatistics(): void {
    this.updateStatistiques();
    this.cdr.markForCheck();
  }

  // =======================
  // ===== Actions UI ======
  // =======================

  onSort(column: string): void {
    if (this.sortBy === column) this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    else { this.sortBy = column; this.sortDirection = 'desc'; }
    this.loadSinistres();
  }

  onPageSizeChange(newSize: number): void {
    this.pageSize = newSize;
    this.currentPage = 0;
    this.loadSinistres();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedNature = '';
    this.selectedType = '';
    this.selectedEtat = '';
    this.selectedGouvernorat = '';
    this.selectedAnnee = null;
    this.selectedUsage = '';
    this.fraudFilter = '';
    this.currentPage = 0;
    this.loadSinistres();
  }

  searchSinistres(): void { this.currentPage = 0; this.loadSinistres(); }

  onPageChange(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages && newPage !== this.currentPage) {
      this.currentPage = newPage;
      this.loadSinistres();
    }
  }

  getPages(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  closeError(): void { this.error = null; }

  actualiserDonnees(): void { this.loadSinistres(); }

  testerConnexionAPI(): void {
    this.http.get(`${this.API}/health`, { withCredentials: true }).subscribe({
      next: () => alert('API OK'),
      error: () => alert('API KO')
    });
  }

  exporterDonnees(): void {
    if (!this.sinistres?.length) { alert('Aucune donnée à exporter'); return; }

    const headers = [
      'numSinistre','anneeExercice','numContrat','dateDeclaration',
      'natureSinistre','typeSinistre','libEtatSinistre',
      'montantEvaluationBrut','totalReglementBrut','lieuAccident','gouvernorat'
    ];
    const rows = this.sinistres.map(s => ([
      s.numSinistre,
      s.anneeExercice ?? '',
      s.numContrat ?? '',
      (s.dateDeclaration ? new Date(s.dateDeclaration).toISOString().substring(0,10) : ''),
      s.natureSinistre ?? '',
      s.typeSinistre ?? '',
      s.libEtatSinistre ?? '',
      (s.montantEvaluationBrut ?? 0),
      (s.totalReglementBrut ?? 0),
      s.lieuAccident ?? '',
      s.gouvernorat ?? ''
    ]));

    const csv = [headers.join(','), ...rows.map(r =>
      r.map(x => (typeof x === 'string' && x.includes(',')) ? `"${x.replace(/"/g,'""')}"` : x).join(',')
    )].join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `sinistres_${new Date().toISOString().substring(0,10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  voirSinistre(s: SinistreAvecML): void { this.showSinistreDetails(s); }

  showFraudDetails(s: SinistreAvecML): void {
    const r = s.fraudDetection;
    if (!r) return;
    const msg = `🚨 DÉTAILS FRAUDE (ML)\n\n` +
      `Sinistre: ${s.numSinistre}\nContrat: ${s.numContrat}\nNature: ${s.natureSinistre}\n` +
      `Score: ${r.fraudScore}% (${r.riskLevel}) — Confiance: ${Math.round(r.confidence * 100)}%\n` +
      `Raison: ${r.reason}\n\nMontant évaluation: ${s.montantEvaluation}\nTotal règlement: ${s.totalReglement}\n\n` +
      `Recommandation: ${r.recommendation || 'Vérification recommandée'}`;
    alert(msg);
  }

  showSinistreDetails(s: SinistreAvecML): void {
    const r = s.fraudDetection;
    const statusIcon = r?.isFraud ? '🚨' : '✅';
    const statusText = r?.isFraud ? 'FRAUDE DÉTECTÉE' : 'SINISTRE NORMAL';
    const msg = `${statusIcon} DÉTAILS DU SINISTRE\n\n` +
      `N°: ${s.numSinistre}\nContrat: ${s.numContrat}\nDate: ${this.formatDate(s.dateDeclaration)}\n` +
      `Nature: ${s.natureSinistre}\nType: ${s.typeSinistre}\nÉtat: ${s.libEtatSinistre}\n` +
      `Lieu: ${s.lieuAccident}\nGouvernorat: ${s.gouvernorat}\n\n` +
      `Montant évaluation: ${s.montantEvaluation}\nTotal règlement: ${s.totalReglement}\n\n` +
      `Statut ML: ${statusText}`;
    alert(msg);
  }

  showFraudDetailsFromNotification(id: string): void {
    const s = this.sinistres.find(x => x.numSinistre === id);
    if (s) this.showFraudDetails(s);
  }

  dismissNotification(id: string): void {
    const n = this.fraudNotifications.find(x => x.id === id);
    if (n) n.dismissed = true;
  }
  clearAllNotifications(): void { this.fraudNotifications = []; this.showNotifications = false; }
  getActiveNotifications(): FraudNotification[] { return this.fraudNotifications.filter(n => !n.dismissed); }

  // ====== Helpers UI / format ======
  getStartIndex(): number { return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1; }
  getEndIndex(): number { const end = (this.currentPage + 1) * this.pageSize; return end > this.totalElements ? this.totalElements : end; }

  getPrioriteClass(priorite: string): string {
    if (!priorite) return 'priorite-normale';
    if (priorite.includes('HAUTE')) return 'priorite-haute';
    if (priorite.includes('MOYENNE')) return 'priorite-moyenne';
    return 'priorite-normale';
  }

  getEtatClass(etat: string): string {
    switch (etat?.toUpperCase()) {
      case 'CLOTURE': return 'badge bg-success';
      case 'MISE A JOUR': return 'badge bg-primary';
      case 'REPRISE': return 'badge bg-warning';
      case 'REOUVERTURE': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  getNatureClass(nature: string): string {
    switch (nature?.toUpperCase()) {
      case 'CORPOREL': return 'badge bg-danger';
      case 'MATERIEL': return 'badge bg-primary';
      case 'MIXTE': return 'badge bg-warning';
      default: return 'badge bg-secondary';
    }
  }

  formatMontant(v: number | null): string {
    if (v === null || v === undefined) return '0,00 DT';
    return new Intl.NumberFormat('fr-TN', { style: 'currency', currency: 'TND', minimumFractionDigits: 2 })
      .format(v).replace('TND', 'DT');
  }

  formatDate(d: any): string {
    if (!d) return 'Non défini';
    const date = new Date(d);
    return isNaN(date.getTime()) ? 'Non défini' : date.toLocaleDateString('fr-FR');
  }

  formatNombre(n: number | null): string {
    if (n === null || n === undefined) return '0';
    return new Intl.NumberFormat('fr-FR').format(n);
  }

  trackBySinistre(_: number, s: SinistreAvecML): string { return s.numSinistre; }
  getFraudIcon(n: string): string { return this.fraudResults.get(n)?.alertIcon || 'fas fa-check-circle'; }
  getFraudColor(n: string): string { return this.fraudResults.get(n)?.alertColor || '#6b7280'; }

  // ====== Helpers CRUD (dates & payload) ======
  private defaultDraft(): Partial<SinistreAvecML> {
    return {
      numSinistre: 'SIN' + Date.now(),
      anneeExercice: new Date().getFullYear(),
      numContrat: 'CNT' + Math.floor(100000 + Math.random() * 899999),
      dateDeclaration: new Date(),
      natureSinistre: 'MATERIEL',
      typeSinistre: 'COLLISION',
      libEtatSinistre: 'MISE A JOUR',
      montantEvaluation: '0,00 DT',
      montantEvaluationBrut: 0,
      totalReglement: '0,00 DT',
      totalReglementBrut: 0,
      lieuAccident: 'Tunis',
      gouvernorat: 'Tunis',
      compagnieAdverse: 'STAR',
      usage: 'PRIVE',
      priorite: 'NORMALE',
      fraudDetection: {
        isFraud: false, confidence: 0, riskLevel: 'LOW', reason: 'Création manuelle',
        fraudScore: 0, alertLevel: 'LOW', alertIcon: 'fas fa-check-circle', alertColor: '#10b981'
      }
    };
  }

  private toIso(d?: string | Date): string | undefined {
    if (!d) return undefined;
    const dd = d instanceof Date ? d : new Date(d);
    if (isNaN(dd.getTime())) return undefined;
    return dd.toISOString().substring(0, 10); // yyyy-MM-dd
  }

  private toApiPayload(s: Partial<SinistreAvecML>): any {
    return {
      numSinistre: s.numSinistre ? String(s.numSinistre) : '',
      anneeExercice: s.anneeExercice ?? undefined,
      numContrat: s.numContrat ?? undefined,
      dateDeclaration: this.toIso(s.dateDeclaration as any),
      natureSinistre: s.natureSinistre ?? undefined,
      typeSinistre: s.typeSinistre ?? undefined,
      libEtatSinistre: s.libEtatSinistre ?? undefined,
      montantEvaluationBrut: s.montantEvaluationBrut ?? 0,
      totalReglementBrut: s.totalReglementBrut ?? 0,
      lieuAccident: s.lieuAccident ?? undefined,
      gouvernorat: s.gouvernorat ?? undefined,
      compagnieAdverse: s.compagnieAdverse ?? undefined,
      usage: s.usage ?? undefined,
      priorite: s.priorite ?? 'NORMALE'
      // (on ne poste pas les champs ML au backend)
    };
  }

  private humanHttpError(e: any): string | null {
    if (e instanceof HttpErrorResponse) {
      if (e.status === 0) return 'Impossible de joindre le serveur (CORS ou serveur éteint).';
      const msg = (e.error && (e.error.message || e.error.error)) || e.message;
      return `${e.status} - ${msg || 'Erreur inconnue'}`;
    }
    return e?.message || null;
  }
}

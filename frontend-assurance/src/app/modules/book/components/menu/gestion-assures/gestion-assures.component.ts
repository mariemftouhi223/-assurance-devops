import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { firstValueFrom, Subscription } from 'rxjs';
import { FraudCasesService, FraudCase } from 'app/services/fraud-cases.service';
import { NotificationService, FraudAlert } from 'app/services/notification.service';

/* ===== Interfaces ===== */
export interface FraudPrediction {
  isFraud: boolean;
  fraudProbability: number;
}

export interface FraudDetection {
  prediction: FraudPrediction;
  fraudScore: number;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'NORMAL';
  reason: string;
  alertIcon: string;
  alertColor: string;
  riskFactors?: string[];
  recommendation?: string;
}

/** Interface alignée avec l'entité Java */
export interface Assure {
  numContrat: string;
  annee?: number;
  anneeExerciceProd?: number;
  anneeExercice?: number;
  effetContrat?: string | Date;
  validiteDu?: string | Date;
  validiteAu?: string | Date;
  prochainTerme?: string | Date;
  codeIntermediaire?: number | string;
  dateNaissance?: string | Date;
  sexe?: string;
  ville?: string;
  codePostal?: number;
  immatriculationVehicule?: string;
  premiereMiseCirculation?: string | Date;
  marqueVehicule?: string;
  usage?: string;
  leasing?: string;
  classeAssure?: number;
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
  totalPrimeNette?: number | string;
  capitaleInc?: number;
  capitaleVol?: number;
  capitaleDv?: number;
  valeurCatalogue?: number;
  valeurVenale?: number;
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
  fraudPercentage: number;
  highRiskCount: number;
  mediumRiskCount: number;
}

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
export class GestionAssuresComponent implements OnInit, OnDestroy {

  /** Seuil strictement > 50% pour alerter */
  private readonly ALERT_MIN_SCORE = 50;

  assures: Assure[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50, 100, 200, 500];

  sortBy: keyof Assure | 'dateCreation' = 'numContrat';
  sortDirection: 'asc' | 'desc' = 'asc';

  searchText = '';
  fraudFilter = '';

  fraudNotifications: NotificationAssure[] = [];
  showNotifications = true;

  private alertsSub?: Subscription; // Abonnement WS

  statistiquesAssures: StatistiquesAssures = {
    totalAssures: 0,
    fraudulentCount: 0,
    fraudPercentage: 0,
    highRiskCount: 0,
    mediumRiskCount: 0
  };

  /** URL backend */
  private readonly API = 'http://localhost:9099/api/v1/assures';

  constructor(
    private cdr: ChangeDetectorRef,
    private fraudCases: FraudCasesService,
    private http: HttpClient,
    private notifSvc: NotificationService
  ) {}

  ngOnInit(): void {
    // 1) Alertes persistées (ex: cas ouverts) au chargement
    this.loadGlobalAlerts();

    // 2) Abonnement aux alertes temps réel (WS)
    this.alertsSub = this.notifSvc.getAlerts().subscribe((alerts: FraudAlert[]) => {
      const fromWs = alerts.map(a => this.mapAlertToUi(a));
      this.mergeNotifications(fromWs);
      this.recomputeStats();
      this.cdr.markForCheck();
    });

    // 3) Charger la page courante
    void this.loadAssures();
  }

  ngOnDestroy(): void {
    this.alertsSub?.unsubscribe();
  }

  /** Charge les alertes persistées côté backend (score > 50) */
  private loadGlobalAlerts(): void {
    this.fraudCases.list('ASSURE', this.ALERT_MIN_SCORE, 'OPEN')
      .subscribe({
        next: (cases: FraudCase[]) => {
          const notifs = (cases || []).map(c => this.mapCaseToNotif(c));
          this.mergeNotifications(notifs);
          this.cdr.markForCheck();
        },
        error: () => { /* silencieux */ }
      });
  }

  /** Clic sur la case "Afficher score ≥ 50%" */
  onHighToggle(): void {
    this.currentPage = 0;
    void this.loadAssures();
  }

  async loadAssures(): Promise<void> {
    this.isLoading = true;
    this.errorMessage = null;

    try {
      let params = new HttpParams()
        .set('page', this.currentPage)
        .set('size', this.pageSize)
        .set('sortBy', String(this.sortBy))
        .set('sortDirection', this.sortDirection);

      // (Optionnel) si ton backend gère la recherche/filtrage
      if (this.searchText?.trim()) {
        params = params.set('q', this.searchText.trim());
      }
      if (this.fraudFilter) {
        params = params.set('filter', this.fraudFilter);
      }

      // ⬇️ IMPORTANT : si la case est cochée, on tente un filtre côté backend
      if (this.showOnlyHigh) {
        params = params
          .set('minScore', String(this.riskThreshold)) // ex. 50
          .set('onlyWithFraud', 'true');
      }

      const page = await firstValueFrom(
        this.http.get<ApiPage<Assure>>(this.API, { params, withCredentials: true })
      );

      this.assures = page.content ?? [];
      this.totalElements = page.totalElements ?? 0;
      this.totalPages = page.totalPages ?? 0;

      // Notifs construites depuis la page courante
      this.processFraudResults();
      this.persistFraudsFromPage();
      this.recomputeStats();

    } catch (e: any) {
      this.errorMessage = this.humanHttpError(e) || 'Erreur lors du chargement des assurés';
      this.assures = [];
      this.totalElements = 0;
      this.totalPages = 0;
    } finally {
      this.isLoading = false;
      this.cdr.markForCheck();
    }
  }

  /** Construit des notifs à partir de la page et les fusionne (sans écraser les autres) */
  private processFraudResults(): void {
    const toAdd: NotificationAssure[] = [];

    for (const a of this.assures) {
      const f = a.fraudDetection;
      if (!f) continue;

      const prob = f.prediction?.fraudProbability ?? 0;
      const score = f.fraudScore ?? Math.round(prob * 100);

      let risk: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'NORMAL' =
        score >= 80 ? 'CRITICAL' :
          score >= 60 ? 'HIGH' :
            score >  50 ? 'MEDIUM' : 'NORMAL';

      if (f.riskLevel) risk = f.riskLevel;

      if (score >= this.ALERT_MIN_SCORE) {
        toAdd.push({
          id: `page-${a.numContrat}-${score}`,
          type: risk === 'CRITICAL' ? 'fraud' : risk === 'HIGH' ? 'warning' : 'info',
          title: risk === 'CRITICAL' ? '🚨 ALERTE FRAUDE (CRITIQUE)'
            : risk === 'HIGH'     ? '⚠️ Risque élevé détecté'
              : 'ℹ️ Risque moyen détecté',
          message: `Contrat ${a.numContrat} — Score ${score}% — ${f.reason || 'Analyse ML'}`,
          contractId: String(a.numContrat),
          timestamp: new Date(),
          dismissed: false
        });

        // (optionnel) notif navigateur
        if ('Notification' in window && Notification.permission === 'granted') {
          new Notification(
            risk === 'CRITICAL' ? '🚨 Alerte fraude critique'
              : risk === 'HIGH' ? '⚠️ Risque élevé'
                : 'ℹ️ Risque moyen',
            { body: `Contrat ${a.numContrat} — Score ${score}%`, tag: `assure-${a.numContrat}` }
          );
        }
      }
    }

    this.mergeNotifications(toAdd);
  }

  private recomputeStats(): void {
    const total = this.totalElements || this.assures.length || 0;
    let fraudulent = 0, high = 0, medium = 0;

    for (const a of this.assures) {
      const fraud = a.fraudDetection;
      if (!fraud) continue;

      const prob = fraud.prediction?.fraudProbability ?? 0;

      if (fraud.prediction?.isFraud) fraudulent++;
      if (prob >= 0.60) high++;
      else if (prob > 0.50) medium++;
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

  // ====== Helpers affichage ML ======
  getFraudIcon(numContrat: number | string): string {
    const assure = this.assures.find(a => String(a.numContrat) === String(numContrat));
    return assure?.fraudDetection?.alertIcon || 'fas fa-check-circle';
  }

  getFraudColor(numContrat: number | string): string {
    const assure = this.assures.find(a => String(a.numContrat) === String(numContrat));
    return assure?.fraudDetection?.alertColor || '#10b981';
  }

  getFraudScore(numContrat: number | string): number {
    const assure = this.assures.find(a => String(a.numContrat) === String(numContrat));
    return assure?.fraudDetection?.fraudScore ?? 0;
  }

  getRiskLevel(numContrat: number | string): string {
    const assure = this.assures.find(a => String(a.numContrat) === String(numContrat));
    return assure?.fraudDetection?.riskLevel || 'NORMAL';
  }

  // Ancien alert(), on le garde si besoin mais on n’en dépend plus
  showFraudDetails(a: Assure): void {
    const f = a.fraudDetection;
    if (!f) {
      alert('Aucune analyse ML disponible pour cet assuré');
      return;
    }
    const msg =
      `🚨 DÉTAILS FRAUDE (ML)\n\n` +
      `Contrat: ${a.numContrat}\n` +
      `Score: ${f.fraudScore}% (${f.riskLevel})\n` +
      `Probabilité: ${Math.round((f.prediction?.fraudProbability ?? 0) * 100)}%\n` +
      `Fraude détectée: ${f.prediction?.isFraud ? 'OUI' : 'NON'}\n` +
      `Raison: ${f.reason}\n\n` +
      `Recommandation: ${f.recommendation || 'Aucune'}`;
    alert(msg);
  }

  /** Ouvrir la fiche depuis une carte-notif (assuré peut ne pas être dans la page courante) */
  showFraudDetailsFromNotification(contractId: string): void {
    void this.openDetailsByContractId(contractId);
  }

  /** Récupère l’assuré par id si absent de la page, puis ouvre le modal */
  private async openDetailsByContractId(contractId: string): Promise<void> {
    // 1) Essaie d'abord dans la page en mémoire
    let a = this.assures.find(x => String(x.numContrat) === String(contractId));

    // 2) Sinon, va le récupérer côté backend (GET /assures/{id})
    if (!a) {
      try {
        a = await firstValueFrom(
          this.http.get<Assure>(`${this.API}/${contractId}`, { withCredentials: true })
        );
      } catch {
        alert(`Assuré ${contractId} introuvable.`);
        return;
      }
    }

    this.openDetails(a);
  }

  private persistFraudsFromPage(): void {
    for (const a of this.assures) {
      const fraud = a.fraudDetection;
      if (fraud && fraud.fraudScore >= this.ALERT_MIN_SCORE) {
        this.fraudCases
          .record('ASSURE', String(a.numContrat), fraud.fraudScore, fraud.reason || 'ML')
          .subscribe({ next: () => {}, error: () => {} });
      }
    }
  }

  // ====== Table / pagination ======
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

  searchAssures(): void {
    this.currentPage = 0;
    void this.loadAssures();
  }

  resetFilters(): void {
    this.searchText = '';
    this.fraudFilter = '';
    this.showOnlyHigh = false;
    this.currentPage = 0;
    void this.loadAssures();
  }

  // ====== Notifications list ======
  getActiveNotifications(): NotificationAssure[] {
    return this.fraudNotifications.filter(n => !n.dismissed);
  }
  clearAllNotifications(): void { this.fraudNotifications = []; }
  dismissNotification(id: string): void {
    const n = this.fraudNotifications.find(x => x.id === id);
    if (n) n.dismissed = true;
  }

  // ====== Divers ======
  showContractDetails(a: Assure): void { this.openDetails(a); } // réutilise le modal
  exportAssures(): void { alert('Export CSV (à implémenter)'); }
  testApi(): void { alert('Test API (à implémenter)'); }
  loadFraudStatistics(): void { this.recomputeStats(); }

  fmt(v?: number | string): string {
    if (v === null || v === undefined || v === '') return '-';
    const n = Number(v);
    return isNaN(n) ? String(v) : n.toLocaleString('fr-FR');
  }
  money(v?: number | string): string {
    if (v === null || v === undefined || v === '') return '-';
    const n = Number(v);
    if (isNaN(n)) return String(v);
    return `${n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} DT`;
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

  // --------- Dates (robustes) ----------
  private parseDateLoose(d: any): Date | null {
    if (d === null || d === undefined) return null;
    if (typeof d === 'string') {
      const s = d.trim();
      if (!s || s.startsWith('0000') || s.toLowerCase() === 'null') return null;
      const mIso = s.match(/^(\d{4})[-/](\d{2})[-/](\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/);
      if (mIso) {
        const [ , yy, mm, dd, hh='0', mi='0', ss='0'] = mIso;
        return new Date(+yy, +mm - 1, +dd, +hh, +mi, +ss);
      }
      const mFr = s.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
      if (mFr) {
        const [ , dd, mm, yy] = mFr;
        return new Date(+yy, +mm - 1, +dd);
      }
    }
    if (typeof d === 'number') {
      if (d <= 0) return null;
      return new Date(d);
    }
    const dt = new Date(d);
    return isNaN(dt.getTime()) ? null : dt;
  }
  formatDate(d?: any): string {
    const dt = this.parseDateLoose(d);
    return dt ? dt.toLocaleDateString('fr-FR') : '-';
  }

  // --------- Helpers notifs ----------
  private mapAlertToUi(a: FraudAlert): NotificationAssure {
    return {
      id: `ws-${a.id}`,
      type: a.priority === 'CRITICAL' ? 'fraud' : a.priority === 'HIGH' ? 'warning' : 'info',
      title: a.priority === 'CRITICAL' ? '🚨 ALERTE FRAUDE CRITIQUE' : '⚠️ Alerte fraude',
      message:
        `Contrat ${a.contractId} — ` +
        `Score: ${Math.round((a.fraudProbability ?? 0) * 100)}% — ` +
        (a.detailedMessage || 'Alerte ML'),
      contractId: a.contractId,
      timestamp: new Date(a.timestamp),
      dismissed: false
    };
  }

  private mapCaseToNotif(c: FraudCase): NotificationAssure {
    return {
      id: `case-${c.id ?? `${c.entity_id}-${c.score}`}`,
      type: c.risk_level === 'CRITICAL' ? 'fraud'
        : c.risk_level === 'HIGH'     ? 'warning'
          : 'info',
      title: c.risk_level === 'CRITICAL' ? '🚨 ALERTE FRAUDE (CRITIQUE)' : '⚠️ Risque élevé détecté',
      message: `Contrat ${c.entity_id} — Score ${c.score}% — ${c.reason || 'Analyse ML'}`,
      contractId: String(c.entity_id),
      timestamp: new Date(c.detected_at),
      dismissed: false
    };
  }

  private mergeNotifications(incoming: NotificationAssure[]): void {
    const byId = new Map(this.fraudNotifications.map(n => [n.id, n] as const));
    for (const n of incoming) {
      if (!byId.has(n.id)) {
        this.fraudNotifications.unshift(n);
        byId.set(n.id, n);
      }
    }
    this.fraudNotifications = Array.from(byId.values()).slice(0, 10);
  }

  // --- Etat & helpers fiche DÉTAILS ---
  detailOpen = false;
  selected?: Assure | null;

  openDetails(a: Assure): void {
    this.selected = a;
    this.detailOpen = true;
    this.cdr.markForCheck();
  }
  closeDetails(): void {
    this.detailOpen = false;
    this.selected = null;
    this.cdr.markForCheck();
  }
  getSelectedScore(): number {
    const f = this.selected?.fraudDetection;
    return f ? (f.fraudScore ?? Math.round((f.prediction?.fraudProbability ?? 0) * 100)) : 0;
  }
  riskClass(level?: string): string {
    switch ((level || 'NORMAL').toUpperCase()) {
      case 'CRITICAL': return 'bg-danger';
      case 'HIGH':     return 'bg-warning';
      case 'MEDIUM':   return 'bg-info';
      default:         return 'bg-success';
    }
  }
  createCaseFromSelection(): void {
    const a = this.selected;
    const f = a?.fraudDetection;
    if (!a || !f) return;
    this.fraudCases
      .record('ASSURE', String(a.numContrat), (f.fraudScore ?? 0), f.reason || 'Analyse ML')
      .subscribe({ next: () => {}, error: () => {} });
  }

  // --- Filtre tableau score >= 50 (fallback front) ---
  riskThreshold = 50;
  showOnlyHigh = false;
  get tableData(): Assure[] {
    // Si backend ne filtre pas, on filtre au moins la page courante en front
    if (!this.showOnlyHigh) return this.assures;
    return this.assures.filter(a => {
      const f = a.fraudDetection;
      if (!f) return false;
      const score = f.fraudScore ?? Math.round((f.prediction?.fraudProbability ?? 0) * 100);
      return score >= this.riskThreshold;
    });
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, forkJoin } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface FraudCase {
  id?: number;
  entity_type: 'ASSURE' | 'SINISTRE';
  entity_id: string;
  score: number; // 0..100
  risk_level: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'NORMAL';
  reason?: string;
  risk_factors?: string[];          // ← explications détaillées
  status: 'OPEN' | 'REVIEWED' | 'DISMISSED' | 'CONFIRMED' | 'RESOLVED';
  detected_at: string | Date;
  updated_at?: string | Date;
}

@Injectable({ providedIn: 'root' })
export class FraudCasesService {
  private readonly base =
    (window.location.port === '4200' ? 'http://localhost:9099' : '') + '/api/v1/fraud';

  private readonly storageKey = 'demo_fraud_cases';

  constructor(private http: HttpClient) {}

  /**
   * entity:
   *  - 'ASSURE'    => seulement assurés
   *  - 'SINISTRE'  => seulement sinistres
   *  - 'ALL'       => assurés + sinistres (agrégation)
   */
  getCases(
    minScore = 50,
    status: FraudCase['status'] = 'OPEN',
    entity: 'ALL' | FraudCase['entity_type'] = 'ALL',
  ): Observable<FraudCase[]> {

    if (entity === 'ALL') {
      return forkJoin([
        this.fetchCases$(minScore, status, 'ASSURE'),
        this.fetchCases$(minScore, status, 'SINISTRE')
      ]).pipe(
        map(([assures, sinistres]) =>
          [...assures, ...sinistres].sort((a, b) => (b.score || 0) - (a.score || 0))
        )
      );
    }

    return this.fetchCases$(minScore, status, entity);
  }

  /** alias si tu l’utilises ailleurs */
  list(type?: 'ASSURE' | 'SINISTRE', minScore = 50, status: FraudCase['status'] = 'OPEN') {
    return this.getCases(minScore, status, type ?? 'ALL');
  }

  /** PATCH /cases/{id} -> mark reviewed */
  markReviewed(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/cases/${id}`, {}).pipe(
      catchError(() => {
        const all = this.readLocal();
        const i = all.findIndex(c => c.id === id);
        if (i >= 0) {
          all[i] = { ...all[i], status: 'REVIEWED', updated_at: new Date().toISOString() };
          this.writeLocal(all);
        }
        return of(void 0);
      })
    );
  }

  /** POST /cases/record (création) */
  record(
    entity_type: FraudCase['entity_type'],
    entity_id: string,
    score: number,
    reason = 'ML'
  ): Observable<void> {
    const body = { entityType: entity_type, entityId: entity_id, score, reason };
    return this.http.post<void>(`${this.base}/cases/record`, body).pipe(
      catchError(() => {
        const all = this.readLocal();
        const newItem: FraudCase = {
          id: this.nextId(all),
          entity_type,
          entity_id,
          score,
          risk_level:
            score >= 80 ? 'CRITICAL'
              : score >= 60 ? 'HIGH'
                : score >= 40 ? 'MEDIUM'
                  : score >= 20 ? 'LOW' : 'NORMAL',
          reason,
          status: 'OPEN',
          detected_at: new Date().toISOString(),
          risk_factors: [] // libre
        };
        all.unshift(newItem);
        this.writeLocal(all);
        return of(void 0);
      })
    );
  }

  // ------------------- internes -------------------

  /** Appel unitaire (un seul type) + fallback local si vide/erreur */
  private fetchCases$(
    minScore: number,
    status: FraudCase['status'],
    entity: FraudCase['entity_type']
  ): Observable<FraudCase[]> {

    let params = new HttpParams()
      .set('minScore', String(minScore))
      .set('status', status)
      .set('entity', entity);

    return this.http.get<FraudCase[]>(`${this.base}/cases`, { params }).pipe(
      map(res => (res && res.length ? res : this.fallback(minScore, status, entity))),
      catchError(() => of(this.fallback(minScore, status, entity)))
    );
  }

  /** Fallback localStorage (dev/démo) */
  private fallback(
    minScore: number,
    status: FraudCase['status'],
    entity?: FraudCase['entity_type']
  ): FraudCase[] {
    this.seedIfEmpty();
    const all = this.readLocal();
    return all.filter(c =>
      c.score >= minScore &&
      c.status === status &&
      (!entity || c.entity_type === entity)
    );
  }

  // ---------- local demo store ----------
  private seedIfEmpty(): void {
    const all = this.readLocal();
    if (all.length > 0) return;

    const now = new Date().toISOString();
    const demo: FraudCase[] = [
      // === ASSURÉS ===
      {
        id: 1, entity_type: 'ASSURE', entity_id: '1998300000014', score: 72, risk_level: 'HIGH',
        reason: 'Score de risque élevé détecté par IA',
        risk_factors: [
          '3 sinistres déclarés sur 90 jours',
          'Contrat récent (< 30 jours) au moment du sinistre',
          'Paiements en espèces inhabituels'
        ],
        status: 'OPEN', detected_at: now
      },
      {
        id: 6, entity_type: 'ASSURE', entity_id: '1999300000473', score: 61, risk_level: 'HIGH',
        reason: 'Score de risque moyen/élevé',
        risk_factors: [
          'Historique d’adresses multiples en peu de temps',
          'Tiers récurrents impliqués sur plusieurs dossiers'
        ],
        status: 'OPEN', detected_at: now
      },

      // === SINISTRES ===
      {
        id: 2, entity_type: 'SINISTRE', entity_id: 'SIN2024000001', score: 85, risk_level: 'CRITICAL',
        reason: 'Multiples indicateurs de fraude détectés - Vérification urgente requise',
        risk_factors: [
          'Déclaration tardive (> 30 jours)',
          'Montant d’évaluation >> moyenne du segment',
          'Lieu de l’accident incohérent avec la domiciliation'
        ],
        status: 'OPEN', detected_at: now
      },
      {
        id: 3, entity_type: 'SINISTRE', entity_id: '20123100158', score: 70, risk_level: 'HIGH',
        reason: 'Indicateurs de fraude significatifs - Investigation recommandée',
        risk_factors: [
          'Deux versions contradictoires recueillies',
          'Réparateur non agrée fréquemment cité'
        ],
        status: 'OPEN', detected_at: now
      },
      {
        id: 4, entity_type: 'SINISTRE', entity_id: '20123100151', score: 60, risk_level: 'MEDIUM',
        reason: 'Anomalies détectées - Surveillance renforcée',
        risk_factors: [
          'Déclaration multi-canal répétée',
          'Photos métadonnées modifiées'
        ],
        status: 'OPEN', detected_at: now
      },
    ];
    this.writeLocal(demo);
  }

  private readLocal(): FraudCase[] {
    try { return JSON.parse(localStorage.getItem(this.storageKey) || '[]'); }
    catch { return []; }
  }
  private writeLocal(all: FraudCase[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(all));
  }
  private nextId(list: FraudCase[]): number {
    return (list.reduce((m, c) => Math.max(m, c.id || 0), 0) || 0) + 1;
  }
}

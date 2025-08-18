import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface FraudCase {
  id?: number;
  entity_type: 'ASSURE' | 'SINISTRE';
  entity_id: string;
  score: number;
  risk_level: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'NORMAL';
  reason?: string;
  status: 'OPEN' | 'REVIEWED' | 'DISMISSED' | 'CONFIRMED' | 'RESOLVED';
  detected_at: string | Date;
  updated_at?: string | Date;
}

@Injectable({ providedIn: 'root' })
export class FraudCasesService {
  private readonly base = '/api/v1/fraud';
  private readonly storageKey = 'demo_fraud_cases';

  constructor(private http: HttpClient) {}

  /** Charge depuis l’API; en cas d’erreur 405/500/etc → fallback localStorage (avec seed si vide). */
  getCases(
    minScore = 50,
    status: FraudCase['status'] = 'OPEN',
    entity: 'ALL' | FraudCase['entity_type'] = 'ALL',
  ): Observable<FraudCase[]> {
    let params = new HttpParams().set('minScore', String(minScore)).set('status', status);
    if (entity !== 'ALL') params = params.set('entity', entity);

    return this.http.get<FraudCase[]>(`${this.base}/cases`, { params }).pipe(
      map(res => {
        // ✅ Fallback aussi si l’API répond 200 avec [] (vide)
        if (!res || res.length === 0) {
          this.seedIfEmpty();
          const all = this.readLocal();
          return all.filter(c =>
            c.score >= minScore &&
            c.status === status &&
            (entity === 'ALL' || c.entity_type === entity)
          );
        }
        return res;
      }),
      catchError(() => {
        // ✅ Fallback en cas d'erreur HTTP
        this.seedIfEmpty();
        const all = this.readLocal();
        return of(all.filter(c =>
          c.score >= minScore &&
          c.status === status &&
          (entity === 'ALL' || c.entity_type === entity)
        ));
      })
    );
  }

  /** Ajoute 2 lignes de démo si le stockage local est vide (utile quand l’API répond 405). */
  private seedIfEmpty(): void {
    const all = this.readLocal();
    if (all.length > 0) return;

    const now = new Date().toISOString();
    const demo: FraudCase[] = [
      {
        id: 1,
        entity_type: 'ASSURE',
        entity_id: '1998300000014',
        score: 72,
        risk_level: 'HIGH',
        reason: 'Test PFE',
        status: 'OPEN',
        detected_at: now
      },
      {
        id: 2,
        entity_type: 'SINISTRE',
        entity_id: 'SIN2024000001',
        score: 85,
        risk_level: 'CRITICAL',
        reason: 'Anomalies multiples',
        status: 'OPEN',
        detected_at: now
      }
    ];
    this.writeLocal(demo);
  }

  /** ---- Fallback localStorage ---- */
  private readLocal(): FraudCase[] {
    try {
      return JSON.parse(localStorage.getItem(this.storageKey) || '[]');
    } catch {
      return [];
    }
  }

  private writeLocal(all: FraudCase[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(all));
  }

  private nextId(list: FraudCase[]): number {
    return (list.reduce((m, c) => Math.max(m, c.id || 0), 0) || 0) + 1;
  }

  /** Marque “REVIEWED” côté API; en fallback on met à jour le localStorage. */
  markReviewed(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/cases/${id}/review`, {}).pipe(
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

  /** Enregistrement d’un cas (utilisé quand score ≥ 50 dans Assurés/Sinistres). */
  record(
    entity_type: FraudCase['entity_type'],
    entity_id: string,
    score: number,
    reason = 'ML'
  ): Observable<void> {
    const body = { entity_type, entity_id, score, reason };
    return this.http.post<void>(`${this.base}/cases`, body).pipe(
      catchError(() => {
        const all = this.readLocal();
        const newItem: FraudCase = {
          id: this.nextId(all),
          entity_type,
          entity_id,
          score,
          risk_level:
            score >= 80 ? 'CRITICAL' :
              score >= 60 ? 'HIGH' :
                score >= 40 ? 'MEDIUM' :
                  score >= 20 ? 'LOW' : 'NORMAL',
          reason,
          status: 'OPEN',
          detected_at: new Date().toISOString()
        };
        all.unshift(newItem);
        this.writeLocal(all);
        return of(void 0);
      })
    );
  }

  /** Alias compat : certaines pages appellent encore .list(...) */
  list(type?: 'ASSURE' | 'SINISTRE', minScore = 50, status: FraudCase['status'] = 'OPEN') {
    return this.getCases(minScore, status, type ?? 'ALL');
  }




}

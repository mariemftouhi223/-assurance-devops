import { Component, OnInit } from '@angular/core';
import { FraudCasesService, FraudCase } from 'app/services/fraud-cases.service';

@Component({
  selector: 'app-fraud-cases',
  templateUrl: './fraud-cases.component.html',
  styleUrls: []
})
export class FraudCasesComponent implements OnInit {
  rows: FraudCase[] = [];
  isLoading = false;
  error: string | null = null;

  // Filtres UI
  filterEntity: 'ALL' | 'ASSURE' | 'SINISTRE' = 'ALL'; // ← PAR DÉFAUT: TOUS
  status: 'OPEN' | 'REVIEWED' | 'DISMISSED' | 'CONFIRMED' | 'RESOLVED' = 'OPEN';
  minScore = 50;

  // Modal détails
  detailOpen = false;
  selected: FraudCase | null = null;

  constructor(private svc: FraudCasesService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading = true;
    this.error = null;

    this.svc.getCases(this.minScore, this.status, this.filterEntity)
      .subscribe({
        next: (data) => {
          this.rows = data || [];
          this.isLoading = false;
        },
        error: () => {
          this.error = 'Erreur de chargement';
          this.rows = [];
          this.isLoading = false;
        }
      });
  }

  review(row: FraudCase): void {
    if (!row?.id) return;
    this.svc.markReviewed(row.id).subscribe({
      next: () => this.load(),
      error: () => this.load()
    });
  }

  openDetails(row: FraudCase): void {
    this.selected = row;
    this.detailOpen = true;
  }
  closeDetails(): void {
    this.detailOpen = false;
    this.selected = null;
  }

  riskBadgeClass(level?: FraudCase['risk_level']): string {
    switch (level) {
      case 'CRITICAL': return 'bg-danger text-white';
      case 'HIGH':     return 'bg-warning text-dark';
      case 'MEDIUM':   return 'bg-primary text-white';
      case 'LOW':      return 'bg-success text-white';
      default:         return 'bg-success text-white';
    }
  }

  trackById = (_: number, r: FraudCase) =>
    r.id ?? `${r.entity_type}-${r.entity_id}-${r.detected_at}`;
}

import { Component, OnInit } from '@angular/core';
import { FraudCasesService, FraudCase } from 'app/services/fraud-cases.service';

@Component({
  selector: 'app-fraud-cases',
  templateUrl: './fraud-cases.component.html',
  // on enlève le SCSS pour éviter l’erreur de loader
  styleUrls: []
})
export class FraudCasesComponent implements OnInit {
  rows: FraudCase[] = [];
  isLoading = false;
  error: string | null = null;

  // Filtres UI
  filterEntity: 'ALL' | 'ASSURE' | 'SINISTRE' = 'ALL';
  status: 'OPEN' | 'REVIEWED' | 'DISMISSED' | 'CONFIRMED' | 'RESOLVED' = 'OPEN';
  minScore = 50;

  constructor(private svc: FraudCasesService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.error = null;

    // ⚠️ utilise getCases (pas list)
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

  // On passe la LIGNE complète depuis le template pour éviter le "number | undefined"
  review(row: FraudCase): void {
    if (!row?.id) { return; } // en fallback localStorage, id est généré; si absent, on ignore
    this.svc.markReviewed(row.id).subscribe({
      next: () => this.load(),
      error: () => this.load()
    });
  }

  // trackBy pour le *ngFor
  trackById = (_: number, r: FraudCase) => r.id ?? `${r.entity_type}-${r.entity_id}-${r.detected_at}`;
}

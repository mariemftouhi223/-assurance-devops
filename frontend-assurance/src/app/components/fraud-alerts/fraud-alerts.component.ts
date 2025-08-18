import { Component, OnInit } from '@angular/core';
import { NotificationService, FraudAlert } from '../../services/notification.service';

@Component({
  selector: 'app-fraud-alerts',
  template: `
    <div class="container-fluid">
      <h2 class="mb-4">
        <i class="fas fa-exclamation-triangle me-2 text-danger"></i>
        Alertes de Fraude
      </h2>

      <!-- Statistiques rapides -->
      <div class="row mb-4">
        <div class="col-md-3">
          <div class="card bg-danger text-white">
            <div class="card-body">
              <h5>{{ getCriticalAlerts().length }}</h5>
              <p class="mb-0">Alertes Critiques</p>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card bg-warning text-white">
            <div class="card-body">
              <h5>{{ getNewAlerts().length }}</h5>
              <p class="mb-0">Nouvelles Alertes</p>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card bg-info text-white">
            <div class="card-body">
              <h5>{{ alerts.length }}</h5>
              <p class="mb-0">Total Alertes</p>
            </div>
          </div>
        </div>
        <div class="col-md-3">
          <div class="card bg-success text-white">
            <div class="card-body">
              <h5>{{ getClosedAlerts().length }}</h5>
              <p class="mb-0">Alertes Traitées</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Liste des alertes -->
      <div class="card">
        <div class="card-header">
          <h5 class="mb-0">Liste des Alertes</h5>
        </div>
        <div class="card-body">
          <div class="table-responsive">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Date</th>
                  <th>Contrat</th>
                  <th>Client</th>
                  <th>Probabilité</th>
                  <th>Priorité</th>
                  <th>Statut</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let alert of alerts"
                    [class.table-danger]="alert.priority === 'CRITICAL'"
                    [class.table-warning]="alert.priority === 'HIGH'">
                  <td>{{ alert.id }}</td>
                  <td>{{ formatDate(alert.timestamp) }}</td>
                  <td>{{ alert.contractId }}</td>
                  <td>{{ alert.clientName }}</td>
                  <td>
                    <span class="badge"
                          [class.bg-danger]="alert.fraudProbability >= 0.9"
                          [class.bg-warning]="alert.fraudProbability >= 0.75 && alert.fraudProbability < 0.9"
                          [class.bg-info]="alert.fraudProbability < 0.75">
                      {{ (alert.fraudProbability * 100).toFixed(1) }}%
                    </span>
                  </td>
                  <td>
                    <span class="badge"
                          [class.bg-danger]="alert.priority === 'CRITICAL'"
                          [class.bg-warning]="alert.priority === 'HIGH'"
                          [class.bg-info]="alert.priority === 'MEDIUM'"
                          [class.bg-secondary]="alert.priority === 'LOW'">
                      {{ alert.priority }}
                    </span>
                  </td>
                  <td>
                    <span class="badge"
                          [class.bg-success]="alert.alertStatus === 'CLOSED'"
                          [class.bg-primary]="alert.alertStatus === 'IN_REVIEW'"
                          [class.bg-warning]="alert.alertStatus === 'NEW'">
                      {{ alert.alertStatus }}
                    </span>
                  </td>
                  <td>
                    <button class="btn btn-sm btn-primary me-1"
                            (click)="viewDetails(alert)">
                      <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-success"
                            (click)="updateStatus(alert)"
                            *ngIf="alert.alertStatus === 'NEW'">
                      <i class="fas fa-check"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .card {
      border: none;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }

    .table th {
      border-top: none;
      font-weight: 600;
    }

    .badge {
      font-size: 0.75rem;
    }
  `]
})
export class FraudAlertsComponent implements OnInit {

  alerts: FraudAlert[] = [];

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationService.getAlerts().subscribe(alerts => {
      this.alerts = alerts;
    });
  }

  getCriticalAlerts(): FraudAlert[] {
    return this.alerts.filter(alert => alert.priority === 'CRITICAL');
  }

  getNewAlerts(): FraudAlert[] {
    return this.alerts.filter(alert => alert.alertStatus === 'NEW');
  }

  getClosedAlerts(): FraudAlert[] {
    return this.alerts.filter(alert => alert.alertStatus === 'CLOSED');
  }

  formatDate(timestamp: string): string {
    return new Date(timestamp).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  viewDetails(alert: FraudAlert): void {
    // Implémentation pour voir les détails
    console.log('Voir détails de l\'alerte:', alert);
  }

  updateStatus(alert: FraudAlert): void {
    // Implémentation pour mettre à jour le statut
    console.log('Mettre à jour le statut de l\'alerte:', alert);
  }
}

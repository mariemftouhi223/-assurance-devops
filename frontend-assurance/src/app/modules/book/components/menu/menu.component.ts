import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService, FraudAlert } from '@app/services/notification.service';
import { KeycloakService } from '../../../../services/keycloak/keycloak.service';

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss']
})
export class MenuComponent implements OnInit, OnDestroy {

  criticalAlertsCount = 0;
  isWebSocketConnected = false;
  private subscriptions: Subscription[] = [];

  constructor(
    private router: Router,
    private notificationService: NotificationService,
    private keycloakService: KeycloakService
  ) {}

  ngOnInit(): void {
    // Initialisation des styles de navigation
    this.setupNavLinks();

    // Initialisation des notifications WebSocket
    this.initializeNotifications();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private setupNavLinks(): void {
    const linkColor = document.querySelectorAll('.nav-link');
    linkColor.forEach(link => {
      if (window.location.href.endsWith(link.getAttribute('href') || '')) {
        link.classList.add('active');
      }
      link.addEventListener('click', () => {
        linkColor.forEach(l => l.classList.remove('active'));
        link.classList.add('active');
      });
    });
  }

  private initializeNotifications(): void {
    // Abonnement au statut de connexion WebSocket
    const connectionSub = this.notificationService.getConnectionStatus().subscribe(
      (connected: boolean) => {
        this.isWebSocketConnected = connected;
      }
    );

    // Abonnement aux alertes pour compter les critiques
    const alertsSub = this.notificationService.getAlerts().subscribe(
      (alerts: FraudAlert[]) => {
        this.criticalAlertsCount = alerts.filter(alert =>
          alert.priority === 'CRITICAL' &&
          (alert.alertStatus === 'NEW' || alert.alertStatus === 'IN_REVIEW')
        ).length;
      }
    );

    this.subscriptions.push(connectionSub, alertsSub);
  }

  getWebSocketStatusText(): string {
    return this.isWebSocketConnected ? 'WebSocket connecté' : 'WebSocket déconnecté';
  }

  async logout(): Promise<void> {  // Correction ici
    this.notificationService.disconnect();
    await this.keycloakService.logout();
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}

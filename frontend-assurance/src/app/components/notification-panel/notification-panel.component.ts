import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { NotificationService, NotificationMessage, FraudAlert } from '../../services/notification.service';
@Component({
  selector: 'app-notification-panel',
  templateUrl: './notification-panel.component.html',
  styleUrls: ['./notification-panel.component.css']
})
export class NotificationPanelComponent implements OnInit, OnDestroy {

  // Propriétés pour les notifications
  notifications: NotificationMessage[] = [];
  alerts: FraudAlert[] = [];
  isConnected = false;
  showNotificationPanel = false;
  unreadCount = 0;

  // Filtres et tri
  selectedFilter = 'all'; // all, critical, high, medium, low
  selectedType = 'all'; // all, fraud_alert, status_update, statistics

  private subscriptions: Subscription[] = [];

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeNotifications();
    this.requestNotificationPermission();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Initialise les abonnements aux notifications
   */
  private initializeNotifications(): void {
    // Abonnement aux nouvelles notifications
    const notificationSub = this.notificationService.getNotifications().subscribe(
      (notification: NotificationMessage) => {
        this.handleNewNotification(notification);
      }
    );

    // Abonnement au statut de connexion
    const connectionSub = this.notificationService.getConnectionStatus().subscribe(
      (connected: boolean) => {
        this.isConnected = connected;
        if (connected) {
          this.loadExistingNotifications();
        }
      }
    );

    // Abonnement aux alertes
    const alertsSub = this.notificationService.getAlerts().subscribe(
      (alerts: FraudAlert[]) => {
        this.alerts = alerts;
      }
    );

    this.subscriptions.push(notificationSub, connectionSub, alertsSub);
  }

  /**
   * Charge les notifications existantes
   */
  private loadExistingNotifications(): void {
    this.notifications = this.notificationService.getAllNotifications();
    this.alerts = this.notificationService.getAllAlerts();
    this.updateUnreadCount();
  }

  /**
   * Traite une nouvelle notification
   */
  private handleNewNotification(notification: NotificationMessage): void {
    // Ajouter à la liste
    this.notifications.unshift(notification);

    // Limiter le nombre affiché
    if (this.notifications.length > 50) {
      this.notifications = this.notifications.slice(0, 50);
    }

    this.updateUnreadCount();

    // Afficher automatiquement le panneau pour les alertes critiques
    if (notification.priority === 'CRITICAL') {
      this.showNotificationPanel = true;
    }

    // Jouer un son pour les alertes importantes
    this.playNotificationSound(notification.priority);
  }

  /**
   * Met à jour le compteur de notifications non lues
   */
  private updateUnreadCount(): void {
    this.unreadCount = this.notificationService.getUnreadNotificationsCount();
  }

  /**
   * Joue un son de notification
   */
  private playNotificationSound(priority: string): void {
    try {
      let audioFile = '';
      switch (priority) {
        case 'CRITICAL':
          audioFile = '/assets/sounds/critical-alert.mp3';
          break;
        case 'HIGH':
          audioFile = '/assets/sounds/high-alert.mp3';
          break;
        default:
          audioFile = '/assets/sounds/notification.mp3';
      }

      const audio = new Audio(audioFile);
      audio.volume = 0.5;
      audio.play().catch(e => {
        console.log('Impossible de jouer le son de notification:', e);
      });
    } catch (error) {
      console.log('Erreur lors de la lecture du son:', error);
    }
  }

  /**
   * Demande la permission pour les notifications du navigateur
   */
  private async requestNotificationPermission(): Promise<void> {
    try {
      const permission = await this.notificationService.requestNotificationPermission();
      if (permission === 'granted') {
        console.log('✅ Permission de notification accordée');
      } else {
        console.log('❌ Permission de notification refusée');
      }
    } catch (error) {
      console.error('Erreur lors de la demande de permission:', error);
    }
  }

  // ===== MÉTHODES PUBLIQUES =====

  /**
   * Bascule l'affichage du panneau de notifications
   */
  toggleNotificationPanel(): void {
    this.showNotificationPanel = !this.showNotificationPanel;
  }

  /**
   * Ferme le panneau de notifications
   */
  closeNotificationPanel(): void {
    this.showNotificationPanel = false;
  }

  /**
   * Navigue vers une alerte spécifique
   */
  navigateToAlert(notification: NotificationMessage): void {
    if (notification.actionUrl) {
      this.router.navigate([notification.actionUrl]);
      this.closeNotificationPanel();
    }
  }

  /**
   * Marque une notification comme lue
   */
  markAsRead(notification: NotificationMessage): void {
    this.notificationService.markNotificationAsRead(notification);
    this.updateUnreadCount();
  }

  /**
   * Supprime une notification
   */
  removeNotification(notification: NotificationMessage): void {
    this.notificationService.removeNotification(notification);
    const index = this.notifications.indexOf(notification);
    if (index > -1) {
      this.notifications.splice(index, 1);
    }
    this.updateUnreadCount();
  }

  /**
   * Marque toutes les notifications comme lues
   */
  markAllAsRead(): void {
    this.notifications.forEach(notification => {
      this.notificationService.markNotificationAsRead(notification);
    });
    this.updateUnreadCount();
  }

  /**
   * Supprime toutes les notifications
   */
  clearAllNotifications(): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer toutes les notifications ?')) {
      this.notifications.forEach(notification => {
        this.notificationService.removeNotification(notification);
      });
      this.notifications = [];
      this.updateUnreadCount();
    }
  }

  /**
   * Filtre les notifications selon les critères sélectionnés
   */
  getFilteredNotifications(): NotificationMessage[] {
    let filtered = [...this.notifications];

    // Filtre par priorité
    if (this.selectedFilter !== 'all') {
      filtered = filtered.filter(n => n.priority.toLowerCase() === this.selectedFilter);
    }

    // Filtre par type
    if (this.selectedType !== 'all') {
      filtered = filtered.filter(n => n.type.toLowerCase().includes(this.selectedType.toLowerCase()));
    }

    return filtered;
  }

  /**
   * Obtient la classe CSS pour une notification selon sa priorité
   */
  getNotificationClass(notification: NotificationMessage): string {
    const baseClass = 'notification-item';
    switch (notification.priority) {
      case 'CRITICAL':
        return `${baseClass} critical`;
      case 'HIGH':
        return `${baseClass} high`;
      case 'MEDIUM':
        return `${baseClass} medium`;
      default:
        return `${baseClass} low`;
    }
  }

  /**
   * Obtient l'icône pour une notification
   */
  getNotificationIcon(notification: NotificationMessage): string {
    switch (notification.type) {
      case 'FRAUD_ALERT':
        return 'fas fa-exclamation-triangle';
      case 'ALERT_STATUS_UPDATE':
        return 'fas fa-info-circle';
      case 'STATISTICS_UPDATE':
        return 'fas fa-chart-bar';
      case 'WELCOME':
        return 'fas fa-check-circle';
      default:
        return 'fas fa-bell';
    }
  }

  /**
   * Formate la date d'une notification
   */
  formatNotificationDate(timestamp: string): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) {
      return 'À l\'instant';
    } else if (diffMins < 60) {
      return `Il y a ${diffMins} min`;
    } else if (diffHours < 24) {
      return `Il y a ${diffHours}h`;
    } else if (diffDays < 7) {
      return `Il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
    } else {
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  }

  /**
   * Reconnecte le service WebSocket
   */
  reconnect(): void {
    this.notificationService.reconnect();
  }

  /**
   * Obtient le statut de connexion sous forme de texte
   */
  getConnectionStatusText(): string {
    return this.isConnected ? 'Connecté' : 'Déconnecté';
  }

  /**
   * Obtient les alertes critiques
   */
  getCriticalAlerts(): FraudAlert[] {
    return this.notificationService.getCriticalAlerts();
  }

  /**
   * Navigue vers le détail d'une alerte
   */
  viewAlertDetails(alert: FraudAlert): void {
    this.router.navigate(['/books/fraud-alerts', alert.id]);
    this.closeNotificationPanel();
  }

  /**
   * Obtient le texte de priorité en français
   */
  getPriorityText(priority: string): string {
    switch (priority) {
      case 'CRITICAL':
        return 'Critique';
      case 'HIGH':
        return 'Élevée';
      case 'MEDIUM':
        return 'Moyenne';
      case 'LOW':
        return 'Faible';
      default:
        return 'Inconnue';
    }
  }

  /**
   * Obtient le texte de type en français
   */
  getTypeText(type: string): string {
    switch (type) {
      case 'FRAUD_ALERT':
        return 'Alerte de fraude';
      case 'ALERT_STATUS_UPDATE':
        return 'Mise à jour d\'alerte';
      case 'STATISTICS_UPDATE':
        return 'Mise à jour statistiques';
      case 'WELCOME':
        return 'Bienvenue';
      default:
        return 'Notification';
    }
  }

  /**
   * TrackBy function pour optimiser les performances de *ngFor
   */
  trackByNotification(index: number, notification: NotificationMessage): string {
    return notification.timestamp + notification.type;
  }
}

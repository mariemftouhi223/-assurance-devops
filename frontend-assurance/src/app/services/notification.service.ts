import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

export interface NotificationMessage {
  type: string;
  title: string;
  message: string;
  data?: any;
  timestamp: string;
  priority: string;
  actionUrl?: string;
}

export interface FraudAlert {
  id: number;
  contractId: string;
  clientName: string;
  fraudProbability: number;
  riskLevel: string;
  priority: string;
  alertStatus: string;
  timestamp: string;
  detailedMessage?: string;
  suspiciousIndicators?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 3000; // 3 secondes

  // Observables pour les notifications
  private notificationsSubject = new Subject<NotificationMessage>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  private alertsSubject = new BehaviorSubject<FraudAlert[]>([]);

  // Stockage local des notifications et alertes
  private notifications: NotificationMessage[] = [];
  private alerts: FraudAlert[] = [];

  constructor() {
    this.connect();
    this.requestNotificationPermission();
  }

  /**
   * ✅ CORRECTION : URL WebSocket corrigée pour correspondre à votre backend
   */
  private connect(): void {
    try {
      // ✅ URL WebSocket corrigée - ajustez le port selon votre configuration Spring Boot
      const wsUrl = `ws://localhost:9099/ws/notifications?userId=${this.getCurrentUserId()}`;

      console.log('🔌 Tentative de connexion WebSocket:', wsUrl);
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = (event) => {
        console.log('✅ Connexion WebSocket établie');
        this.connectionStatusSubject.next(true);
        this.reconnectAttempts = 0;

        // Envoyer un message de souscription
        this.sendMessage('subscribe');
      };

      this.socket.onmessage = (event) => {
        try {
          console.log('📨 Message WebSocket reçu:', event.data);
          const notification: NotificationMessage = JSON.parse(event.data);
          this.handleNotification(notification);
        } catch (error) {
          console.error('❌ Erreur lors du parsing du message WebSocket:', error);
        }
      };

      this.socket.onclose = (event) => {
        console.log('❌ Connexion WebSocket fermée:', event.code, event.reason);
        this.connectionStatusSubject.next(false);
        this.attemptReconnect();
      };

      this.socket.onerror = (error) => {
        console.error('❌ Erreur WebSocket:', error);
        this.connectionStatusSubject.next(false);
      };

    } catch (error) {
      console.error('❌ Erreur lors de la connexion WebSocket:', error);
      this.attemptReconnect();
    }
  }

  /**
   * Tente de se reconnecter automatiquement
   */
  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`🔄 Tentative de reconnexion ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);

      setTimeout(() => {
        this.connect();
      }, this.reconnectInterval);
    } else {
      console.error('❌ Nombre maximum de tentatives de reconnexion atteint');
    }
  }

  /**
   * ✅ AMÉLIORATION : Traitement amélioré des notifications avec plus de types
   */
  private handleNotification(notification: NotificationMessage): void {
    console.log('📨 Notification reçue:', notification);

    // Ajouter à la liste des notifications
    this.notifications.unshift(notification);

    // Limiter le nombre de notifications stockées
    if (this.notifications.length > 100) {
      this.notifications = this.notifications.slice(0, 100);
    }

    // Traitement spécifique selon le type
    switch (notification.type) {
      case 'FRAUD_ALERT':
        this.handleFraudAlert(notification);
        break;
      case 'ALERT_STATUS_UPDATE':
        this.handleAlertStatusUpdate(notification);
        break;
      case 'STATISTICS_UPDATE':
        this.handleStatisticsUpdate(notification);
        break;
      case 'WELCOME':
        console.log('👋 Message de bienvenue reçu');
        break;
      case 'FRAUD_DETECTION':
        this.handleFraudDetection(notification);
        break;
      default:
        console.log('📋 Notification générique reçue:', notification.type);
    }

    // Émettre la notification
    this.notificationsSubject.next(notification);

    // Afficher une notification visuelle
    this.showVisualNotification(notification);
  }

  /**
   * ✅ AMÉLIORATION : Traitement amélioré des alertes de fraude
   */
  private handleFraudAlert(notification: NotificationMessage): void {
    if (notification.data) {
      const alert: FraudAlert = {
        id: notification.data.id || notification.data.alertId,
        contractId: notification.data.contractId || 'UNKNOWN',
        clientName: notification.data.clientName || 'Client inconnu',
        fraudProbability: notification.data.fraudProbability || 0,
        riskLevel: notification.data.riskLevel || 'UNKNOWN',
        priority: notification.data.priority || 'MEDIUM',
        alertStatus: notification.data.alertStatus || 'NEW',
        timestamp: notification.data.timestamp || notification.timestamp,
        detailedMessage: notification.data.detailedMessage,
        suspiciousIndicators: notification.data.suspiciousIndicators || []
      };

      // Ajouter à la liste des alertes
      this.alerts.unshift(alert);
      this.alertsSubject.next([...this.alerts]);

      console.log('🚨 Nouvelle alerte de fraude:', alert);

      // ✅ NOUVEAU : Afficher une notification spéciale pour les alertes critiques
      if (alert.priority === 'CRITICAL') {
        this.showCriticalAlert(alert);
      }
    }
  }

  /**
   * ✅ NOUVEAU : Traitement spécifique pour les détections de fraude
   */
  private handleFraudDetection(notification: NotificationMessage): void {
    console.log('🔍 Détection de fraude reçue:', notification);

    // Créer une alerte à partir de la détection
    if (notification.data && notification.data.prediction && notification.data.prediction.isFraud) {
      const alert: FraudAlert = {
        id: Date.now(), // ID temporaire
        contractId: notification.data.contractData?.contractId || 'UNKNOWN',
        clientName: notification.data.clientData?.firstName + ' ' + notification.data.clientData?.lastName || 'Client inconnu',
        fraudProbability: notification.data.prediction.fraudProbability,
        riskLevel: notification.data.prediction.riskLevel,
        priority: this.calculatePriority(notification.data.prediction.fraudProbability),
        alertStatus: 'NEW',
        timestamp: notification.timestamp,
        detailedMessage: `Fraude détectée avec une probabilité de ${(notification.data.prediction.fraudProbability * 100).toFixed(1)}%`,
        suspiciousIndicators: []
      };

      this.alerts.unshift(alert);
      this.alertsSubject.next([...this.alerts]);

      console.log('🚨 Alerte créée à partir de la détection:', alert);
    }
  }

  /**
   * ✅ NOUVEAU : Calcule la priorité basée sur la probabilité de fraude
   */
  private calculatePriority(fraudProbability: number): string {
    if (fraudProbability >= 0.9) return 'CRITICAL';
    if (fraudProbability >= 0.75) return 'HIGH';
    if (fraudProbability >= 0.5) return 'MEDIUM';
    return 'LOW';
  }

  /**
   * ✅ NOUVEAU : Affiche une alerte critique spéciale
   */
  private showCriticalAlert(alert: FraudAlert): void {
    // Afficher une notification persistante pour les alertes critiques
    if ('Notification' in window && Notification.permission === 'granted') {
      const criticalNotification = new Notification('🚨 ALERTE FRAUDE CRITIQUE', {
        body: `Contrat ${alert.contractId} - Probabilité: ${(alert.fraudProbability * 100).toFixed(1)}%`,
        icon: '/assets/icons/critical-alert.png',
        requireInteraction: true,
        tag: 'critical-fraud-alert'
      });

      criticalNotification.onclick = () => {
        window.focus();
        // Naviguer vers la page des alertes
        window.location.href = '/books/fraud-alerts';
        criticalNotification.close();
      };
    }

    // Log spécial pour les alertes critiques
    console.error('🚨🚨🚨 ALERTE CRITIQUE:', alert);
  }

  /**
   * Traite une mise à jour de statut d'alerte
   */
  private handleAlertStatusUpdate(notification: NotificationMessage): void {
    if (notification.data) {
      const alertId = notification.data.alertId || notification.data.id;
      const newStatus = notification.data.newStatus || notification.data.alertStatus;

      // Mettre à jour l'alerte dans la liste
      const alertIndex = this.alerts.findIndex(a => a.id === alertId);
      if (alertIndex !== -1) {
        this.alerts[alertIndex].alertStatus = newStatus;
        this.alertsSubject.next([...this.alerts]);
      }

      console.log(`📝 Statut d'alerte mis à jour: ${alertId} -> ${newStatus}`);
    }
  }

  /**
   * Traite une mise à jour des statistiques
   */
  private handleStatisticsUpdate(notification: NotificationMessage): void {
    console.log('📊 Statistiques mises à jour:', notification.data);
    // Ici vous pouvez émettre vers un service de statistiques si nécessaire
  }

  /**
   * ✅ AMÉLIORATION : Notification visuelle améliorée
   */
  private showVisualNotification(notification: NotificationMessage): void {
    // Vérifier si les notifications du navigateur sont supportées et autorisées
    if ('Notification' in window && Notification.permission === 'granted') {
      const options = {
        body: notification.message,
        icon: this.getNotificationIcon(notification.priority),
        badge: '/assets/icons/fraud-alert-badge.png',
        tag: notification.type,
        requireInteraction: notification.priority === 'CRITICAL',
        data: notification.data
      };

      const browserNotification = new Notification(notification.title, options);

      // Gérer le clic sur la notification
      browserNotification.onclick = () => {
        window.focus();
        if (notification.actionUrl) {
          // Naviguer vers l'URL d'action si disponible
          window.location.href = notification.actionUrl;
        } else if (notification.type === 'FRAUD_ALERT') {
          // Naviguer vers les alertes pour les alertes de fraude
          window.location.href = '/books/fraud-alerts';
        }
        browserNotification.close();
      };

      // Fermer automatiquement après 5 secondes (sauf pour les critiques)
      if (notification.priority !== 'CRITICAL') {
        setTimeout(() => {
          browserNotification.close();
        }, 5000);
      }
    } else {
      // Fallback : afficher dans la console si les notifications ne sont pas disponibles
      console.log(`🔔 ${notification.title}: ${notification.message}`);
    }
  }

  /**
   * Retourne l'icône appropriée selon la priorité
   */
  private getNotificationIcon(priority: string): string {
    switch (priority) {
      case 'CRITICAL':
        return '/assets/icons/critical-alert.png';
      case 'HIGH':
        return '/assets/icons/high-alert.png';
      case 'MEDIUM':
        return '/assets/icons/medium-alert.png';
      default:
        return '/assets/icons/info-alert.png';
    }
  }

  /**
   * Envoie un message via WebSocket
   */
  private sendMessage(message: string): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(message);
      console.log('📤 Message envoyé:', message);
    } else {
      console.warn('⚠️ WebSocket non connecté, impossible d\'envoyer:', message);
    }
  }

  /**
   * Obtient l'ID de l'utilisateur actuel
   */
  private getCurrentUserId(): string {
    // Récupérer l'ID utilisateur depuis votre service d'authentification
    return localStorage.getItem('userId') ||
      sessionStorage.getItem('currentUser') ||
      'user_' + Date.now();
  }

  // ===== MÉTHODES PUBLIQUES =====

  /**
   * Observable pour recevoir les notifications
   */
  getNotifications(): Observable<NotificationMessage> {
    return this.notificationsSubject.asObservable();
  }

  /**
   * Observable pour le statut de connexion
   */
  getConnectionStatus(): Observable<boolean> {
    return this.connectionStatusSubject.asObservable();
  }

  /**
   * Observable pour les alertes de fraude
   */
  getAlerts(): Observable<FraudAlert[]> {
    return this.alertsSubject.asObservable();
  }

  /**
   * Obtient toutes les notifications
   */
  getAllNotifications(): NotificationMessage[] {
    return [...this.notifications];
  }

  /**
   * Obtient toutes les alertes
   */
  getAllAlerts(): FraudAlert[] {
    return [...this.alerts];
  }

  /**
   * ✅ NOUVEAU : Simule une alerte de test (utile pour le développement)
   */
  simulateTestAlert(): void {
    const testAlert: NotificationMessage = {
      type: 'FRAUD_ALERT',
      title: '🚨 Test - Alerte de Fraude',
      message: 'Ceci est une alerte de test pour vérifier le système de notification',
      priority: 'HIGH',
      timestamp: new Date().toISOString(),
      data: {
        id: Date.now(),
        contractId: 'TEST-' + Date.now(),
        clientName: 'Client Test',
        fraudProbability: 0.85,
        riskLevel: 'HIGH',
        priority: 'HIGH',
        alertStatus: 'NEW',
        timestamp: new Date().toISOString(),
        detailedMessage: 'Alerte de test générée pour vérifier le fonctionnement du système'
      }
    };

    this.handleNotification(testAlert);
  }

  /**
   * Marque une notification comme lue
   */
  markNotificationAsRead(notification: NotificationMessage): void {
    console.log('✅ Notification marquée comme lue:', notification);
    // Implémentation pour marquer comme lu
  }

  /**
   * Supprime une notification
   */
  removeNotification(notification: NotificationMessage): void {
    const index = this.notifications.indexOf(notification);
    if (index > -1) {
      this.notifications.splice(index, 1);
      console.log('🗑️ Notification supprimée');
    }
  }

  /**
   * Demande la permission pour les notifications du navigateur
   */
  async requestNotificationPermission(): Promise<NotificationPermission> {
    if ('Notification' in window) {
      const permission = await Notification.requestPermission();
      console.log('🔔 Permission notifications:', permission);
      return permission;
    }
    return Promise.resolve('denied');
  }

  /**
   * Reconnexion manuelle
   */
  reconnect(): void {
    console.log('🔄 Reconnexion manuelle...');
    if (this.socket) {
      this.socket.close();
    }
    this.reconnectAttempts = 0;
    this.connect();
  }

  /**
   * Ferme la connexion WebSocket
   */
  disconnect(): void {
    console.log('🔌 Déconnexion WebSocket...');
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.connectionStatusSubject.next(false);
  }

  /**
   * Obtient le nombre de notifications non lues
   */
  getUnreadNotificationsCount(): number {
    return this.notifications.filter(n =>
      n.priority === 'CRITICAL' || n.priority === 'HIGH'
    ).length;
  }

  /**
   * Obtient les alertes critiques non traitées
   */
  getCriticalAlerts(): FraudAlert[] {
    return this.alerts.filter(alert =>
      alert.priority === 'CRITICAL' &&
      (alert.alertStatus === 'NEW' || alert.alertStatus === 'IN_REVIEW')
    );
  }

  /**
   * ✅ NOUVEAU : Obtient les statistiques des alertes
   */
  getAlertStatistics() {
    const total = this.alerts.length;
    const critical = this.alerts.filter(a => a.priority === 'CRITICAL').length;
    const high = this.alerts.filter(a => a.priority === 'HIGH').length;
    const newAlerts = this.alerts.filter(a => a.alertStatus === 'NEW').length;

    return {
      total,
      critical,
      high,
      newAlerts,
      avgProbability: total > 0 ?
        this.alerts.reduce((sum, a) => sum + a.fraudProbability, 0) / total : 0
    };
  }
}

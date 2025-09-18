import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

import SockJS from 'sockjs-client';

import { Client, IMessage } from '@stomp/stompjs';

export interface NotificationMessage {
  type: string;
  title: string;
  message: string;
  data?: any;
  timestamp: string;
  priority: string;
  actionUrl?: string;
  read?: boolean;
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

@Injectable({ providedIn: 'root' })
export class NotificationService {
  // ===== Connexion =====
  private stomp?: Client;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly reconnectInterval = 3000; // ms

  // ===== State/streams =====
  private notificationsSubject = new Subject<NotificationMessage>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  private alertsSubject = new BehaviorSubject<FraudAlert[]>([]);
  private notifications: NotificationMessage[] = [];
  private alerts: FraudAlert[] = [];

  constructor() {
    this.connect();
    this.requestNotificationPermission();
  }

  /** Base backend (marche avec ou sans proxy Angular) */
  private backendBase(): string {
    return window.location.port === '4200'
      ? 'http://localhost:9099'
      : `${window.location.protocol}//${window.location.host}`;
  }

  /** Connexion STOMP/SockJS */
  private connect(): void {
    try {
      const url = `${this.backendBase()}/ws/notifications`;
      const sock = new SockJS(url);

      this.stomp = new Client({
        webSocketFactory: () => sock as any,
        reconnectDelay: 0, // on gère nous-mêmes
      });

      this.stomp.onConnect = () => {
        console.log('✅ STOMP connecté');
        this.connectionStatusSubject.next(true);
        this.reconnectAttempts = 0;

        // Abonnements aux topics du backend
        this.stomp!.subscribe('/topic/fraud-alerts', (msg: IMessage) => this.onStompMessage(msg));
        this.stomp!.subscribe('/topic/alert-updates', (msg: IMessage) => this.onStompMessage(msg));

        // (optionnel) informer le serveur
        this.sendMessage('subscribe');
      };

      this.stomp.onWebSocketClose = () => {
        console.log('❌ WS fermé');
        this.connectionStatusSubject.next(false);
        this.attemptReconnect();
      };

      this.stomp.onStompError = (frame) => {
        console.error('❌ STOMP error', frame);
      };

      this.stomp.activate();
    } catch (error) {
      console.error('❌ Erreur connexion STOMP', error);
      this.attemptReconnect();
    }
  }

  private onStompMessage(msg: IMessage) {
    try {
      const notification: NotificationMessage = JSON.parse(msg.body);
      this.handleNotification(notification);
    } catch (e) {
      console.error('❌ parse notif', e, msg.body);
    }
  }

  /** Reconnexion progressive */
  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ Nombre maximum de tentatives atteint');
      return;
    }
    this.reconnectAttempts++;
    console.log(`🔄 Reconnexion ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);
    setTimeout(() => this.connect(), this.reconnectInterval);
  }

  /** Envoi d’un petit message d’abonnement côté serveur (optionnel) */
  private sendMessage(message: string): void {
    if (this.stomp?.connected) {
      this.stomp.publish({ destination: '/app/subscribe', body: message });
      console.log('📤 STOMP publish /app/subscribe');
    } else {
      console.warn('⚠️ STOMP non connecté');
    }
  }

  // ============ Votre logique notifications/alertes (inchangée) ============
  private handleNotification(notification: NotificationMessage): void {
    this.notifications.unshift(notification);
    if (this.notifications.length > 100) this.notifications = this.notifications.slice(0, 100);

    switch (notification.type) {
      case 'FRAUD_ALERT': this.handleFraudAlert(notification); break;
      case 'ALERT_STATUS_UPDATE': this.handleAlertStatusUpdate(notification); break;
      case 'STATISTICS_UPDATE': this.handleStatisticsUpdate(notification); break;
      case 'WELCOME': console.log('👋 Message de bienvenue'); break;
      case 'FRAUD_DETECTION': this.handleFraudDetection(notification); break;
      default: console.log('📋 Notification:', notification.type);
    }

    this.notificationsSubject.next(notification);
    this.showVisualNotification(notification);
  }

  private handleFraudAlert(notification: NotificationMessage): void {
    if (!notification.data) return;
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
    this.alerts.unshift(alert);
    this.alertsSubject.next([...this.alerts]);
    if (alert.priority === 'CRITICAL') this.showCriticalAlert(alert);
  }

  private handleFraudDetection(notification: NotificationMessage): void {
    if (!(notification.data?.prediction?.isFraud)) return;
    const alert: FraudAlert = {
      id: Date.now(),
      contractId: notification.data.contractData?.contractId || 'UNKNOWN',
      clientName:
        (notification.data.clientData?.firstName || '') + ' ' +
        (notification.data.clientData?.lastName || ''),
      fraudProbability: notification.data.prediction.fraudProbability,
      riskLevel: notification.data.prediction.riskLevel,
      priority: this.calculatePriority(notification.data.prediction.fraudProbability),
      alertStatus: 'NEW',
      timestamp: notification.timestamp,
      detailedMessage:
        `Fraude détectée ${(notification.data.prediction.fraudProbability * 100).toFixed(1)}%`,
      suspiciousIndicators: []
    };
    this.alerts.unshift(alert);
    this.alertsSubject.next([...this.alerts]);
  }

  private calculatePriority(p: number): string {
    if (p >= 0.9) return 'CRITICAL';
    if (p >= 0.75) return 'HIGH';
    if (p >= 0.5) return 'MEDIUM';
    return 'LOW';
  }

  private showCriticalAlert(alert: FraudAlert): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      const n = new Notification('🚨 ALERTE FRAUDE CRITIQUE', {
        body: `Contrat ${alert.contractId} - ${(alert.fraudProbability * 100).toFixed(1)}%`,
        icon: '/assets/icons/critical-alert.png',
        requireInteraction: true,
        tag: 'critical-fraud-alert'
      });
      n.onclick = () => { window.focus(); window.location.href = '/books/fraud-alerts'; n.close(); };
    }
    console.error('🚨🚨🚨 ALERTE CRITIQUE:', alert);
  }

  private handleAlertStatusUpdate(notification: NotificationMessage): void {
    const id = notification.data?.alertId ?? notification.data?.id;
    const newStatus = notification.data?.newStatus ?? notification.data?.alertStatus;
    if (!id || !newStatus) return;
    const i = this.alerts.findIndex(a => a.id === id);
    if (i !== -1) {
      this.alerts[i].alertStatus = newStatus;
      this.alertsSubject.next([...this.alerts]);
    }
  }

  private handleStatisticsUpdate(notification: NotificationMessage): void {
    console.log('📊 Stats maj:', notification.data);
  }

  private showVisualNotification(notification: NotificationMessage): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      const n = new Notification(notification.title, {
        body: notification.message,
        icon: this.getNotificationIcon(notification.priority),
        badge: '/assets/icons/fraud-alert-badge.png',
        tag: notification.type,
        requireInteraction: notification.priority === 'CRITICAL',
        data: notification.data
      });
      n.onclick = () => {
        window.focus();
        if (notification.actionUrl) window.location.href = notification.actionUrl;
        else if (notification.type === 'FRAUD_ALERT') window.location.href = '/books/fraud-alerts';
        n.close();
      };
      if (notification.priority !== 'CRITICAL') setTimeout(() => n.close(), 5000);
    } else {
      console.log(`🔔 ${notification.title}: ${notification.message}`);
    }
  }

  private getNotificationIcon(priority: string): string {
    if (priority === 'CRITICAL') return '/assets/icons/critical-alert.png';
    if (priority === 'HIGH') return '/assets/icons/high-alert.png';
    if (priority === 'MEDIUM') return '/assets/icons/medium-alert.png';
    return '/assets/icons/info-alert.png';
  }

  // ===== API publique =====
  getNotifications(): Observable<NotificationMessage> { return this.notificationsSubject.asObservable(); }
  getConnectionStatus(): Observable<boolean> { return this.connectionStatusSubject.asObservable(); }
  getAlerts(): Observable<FraudAlert[]> { return this.alertsSubject.asObservable(); }
  getAllNotifications(): NotificationMessage[] { return [...this.notifications]; }
  getAllAlerts(): FraudAlert[] { return [...this.alerts]; }

  simulateTestAlert(): void {
    const testAlert: NotificationMessage = {
      type: 'FRAUD_ALERT',
      title: '🚨 Test - Alerte de Fraude',
      message: 'Alerte de test',
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
        detailedMessage: 'Alerte de test générée'
      }
    };
    this.handleNotification(testAlert);
  }

  async requestNotificationPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) return 'denied';
    const permission = await Notification.requestPermission();
    console.log('🔔 Permission notifications:', permission);
    return permission;
  }

  reconnect(): void {
    console.log('🔄 Reconnexion manuelle...');
    this.reconnectAttempts = 0;
    if (this.stomp) this.stomp.deactivate().finally(() => this.connect());
    else this.connect();
  }

  disconnect(): void {
    console.log('🔌 Déconnexion STOMP...');
    this.stomp?.deactivate();
    this.stomp = undefined;
    this.connectionStatusSubject.next(false);
  }

  getUnreadNotificationsCount(): number {
    return this.notifications.filter(n => n.priority === 'CRITICAL' || n.priority === 'HIGH').length;
  }

  getCriticalAlerts(): FraudAlert[] {
    return this.alerts.filter(a => a.priority === 'CRITICAL' && (a.alertStatus === 'NEW' || a.alertStatus === 'IN_REVIEW'));
  }

  /** Marque une notification comme lue (utilisé par NotificationPanel) */
  public markNotificationAsRead(notification: NotificationMessage): void {
    const i = this.notifications.indexOf(notification);
    if (i > -1) {
      this.notifications[i] = { ...this.notifications[i], read: true };
    }
    // Émet une maj pour que l’UI se rafraîchisse si elle écoute le flux
    this.notificationsSubject.next({ ...notification, read: true });
  }

  /** Supprime une notification (utilisé par NotificationPanel) */
  public removeNotification(notification: NotificationMessage): void {
    const i = this.notifications.indexOf(notification);
    if (i > -1) {
      this.notifications.splice(i, 1);
    }
    // Pas d’émission obligatoire, l’UI relira via getAllNotifications() si besoin
  }


}

import {Injectable} from '@angular/core';
import Keycloak from 'keycloak-js';
import {UserProfile} from './user-profile';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private _keycloak: Keycloak | undefined;

  get keycloak() {
    if (!this._keycloak) {
      this._keycloak = new Keycloak({
        url: 'http://localhost:9090',
        realm: 'assurance',
        clientId: 'angular-client'
      });
    }
    return this._keycloak;
  }

  private _profile: UserProfile | undefined;

  get profile(): UserProfile | undefined {
    return this._profile;
  }

  async init() {
    const authenticated = await this.keycloak.init({
      onLoad: 'login-required',
    });

    if (authenticated) {
      this._profile = (await this.keycloak.loadUserProfile()) as UserProfile;
      this._profile.token = this.keycloak.token || '';
    }
  }

  login() {
    return this.keycloak.login();
  }

  logout() {
    // this.keycloak.accountManagement();
    return this.keycloak.logout({redirectUri: 'http://localhost:4200'});
  }

  // ✅ NOUVELLE MÉTHODE AJOUTÉE - Compatible avec keycloak-angular
  async getToken(): Promise<string> {
    try {
      // Vérifier si le token est encore valide
      if (this.keycloak.token) {
        // Rafraîchir le token si nécessaire
        await this.keycloak.updateToken(30); // Rafraîchir si expire dans 30 secondes
        return this.keycloak.token;
      }
      throw new Error('Aucun token disponible');
    } catch (error) {
      console.error('Erreur lors de la récupération du token:', error);
      throw error;
    }
  }

  // ✅ MÉTHODE UTILITAIRE SUPPLÉMENTAIRE
  isLoggedIn(): boolean {
    return !!this.keycloak.authenticated;
  }

  // ✅ MÉTHODE POUR OBTENIR LES RÔLES UTILISATEUR
  getUserRoles(): string[] {
    return this.keycloak.realmAccess?.roles || [];
  }




}


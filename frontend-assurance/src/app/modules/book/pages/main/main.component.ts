import { Component } from '@angular/core';
import { KeycloakService } from '@app/services/keycloak/keycloak.service';
@Component({
  selector: 'app-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss']
})
export class MainComponent {
  constructor(public kc: KeycloakService) {}

  async logout(): Promise<void> {
    try {
      await this.kc.logout();
    } catch (e) {
      console.error('Erreur de déconnexion', e);
    }
  }
}

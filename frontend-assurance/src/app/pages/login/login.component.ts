import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
// Chemin corrigé depuis src/app/pages/login/ vers src/app/services/keycloak/
import {KeycloakService} from '../../services/keycloak/keycloak.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  constructor(
    private router: Router,
    // Injection de VOTRE KeycloakService manuel
    private keycloakService: KeycloakService
  ) {
  }

  ngOnInit(): void {
    // Attention : Appeler login() dans ngOnInit peut causer des boucles
    // si l'initialisation Keycloak n'est pas encore terminée ou échoue.
    // Il est préférable de déclencher le login sur une action utilisateur (bouton).
    // Si vous utilisez onLoad: 'login-required', cet appel est redondant.
    // this.triggerLogin(); // Commenté pour éviter les problèmes potentiels
  }

  // Méthode pour déclencher le login (par ex. sur un clic de bouton)
  async triggerLogin(): Promise<void> {
    // L'appel à login() devrait fonctionner si défini dans votre service manuel
    // et si le service est correctement injecté.
    try {
      await this.keycloakService.login();
    } catch (error) {
      console.error('Error during login:', error);
      // Gérer l'erreur, par exemple afficher un message à l'utilisateur
    }
  }

  // Méthodes pour les liens (si nécessaire)
  login() {
    // Appeler la méthode de déclenchement du login Keycloak
    this.triggerLogin();
  }

  register() {
    this.router.navigate(['register']);
  }
}


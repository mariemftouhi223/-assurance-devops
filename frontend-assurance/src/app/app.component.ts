import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'AntiFraude - Système de Gestion';

  constructor(private router: Router) {}

  logout(): void {
    // Logique de déconnexion
    console.log('Déconnexion...');

    // Supprimer les tokens/données de session
    localStorage.removeItem('authToken');
    sessionStorage.clear();

    // Rediriger vers la page de connexion
    this.router.navigate(['/login']);
  }
}

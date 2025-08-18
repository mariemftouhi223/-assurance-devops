import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
// Assurez-vous que ce chemin pointe vers VOTRE KeycloakService manuel
import { KeycloakService } from '../../services/keycloak/keycloak.service';

export const authGuard: CanActivateFn = () => {
  // Injection de VOTRE KeycloakService manuel
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  // Vérification avec la propriété 'keycloak' de votre service manuel
  if (keycloakService.keycloak.isTokenExpired()) {
    router.navigate(['login']);
    return false;
  }
  return true;
};


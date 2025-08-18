import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {MainComponent} from './pages/main/main.component';
import {BookListComponent} from './pages/book-list/book-list.component';
import {MyBooksComponent} from './pages/my-books/my-books.component';
import {ManageBookComponent} from './pages/manage-book/manage-book.component';
import {BorrowedBookListComponent} from './pages/borrowed-book-list/borrowed-book-list.component';
import {ReturnedBooksComponent} from './pages/returned-books/returned-books.component';
import {authGuard} from '../../services/guard/auth.guard';
import {BookDetailsComponent} from './pages/book-details/book-details.component';
import { HomeComponent } from './components/menu/home/home.component';
import { SinistresComponent } from './components/menu/sinistres/sinistres.component';
import { GestionAssuresComponent } from './components/menu/gestion-assures/gestion-assures.component';
import { FraudDashboardComponent } from './components/menu/fraud-dashboard/fraud-dashboard.component';
import {FraudAlertsComponent} from "@app/components/fraud-alerts/fraud-alerts.component";
import { FraudCasesComponent } from './components/menu/fraud-cases/fraud-cases.component';

import {FraudDetectionComponent} from "@app/components/fraud-detection/fraud-detection.component";

const routes: Routes = [
  {
    path: '',
    component: MainComponent,
    canActivate: [authGuard],
    children: [
      // ✅ Route par défaut - REDIRIGE vers dashboard au lieu de BookListComponent
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },



      { path: 'fraud-cases', component: FraudCasesComponent },

      // ✅ Routes principales de l'application d'assurance
      {
        path: 'dashboard',
        component: FraudDashboardComponent,
        canActivate: [authGuard]
      },
      {
        path: 'home',
        component: HomeComponent,
        canActivate: [authGuard]
      },

      // ✅ Route pour Gestion des assurés
      {
        path: 'insured',
        component: GestionAssuresComponent,
        canActivate: [authGuard]
      },


      // ✅ Route pour Sinistres et remboursement
      {
        path: 'claims',
        component: SinistresComponent,
        canActivate: [authGuard]
      },

      // ✅ Routes pour la détection de fraude (SANS DUPLICATION)
      {
        path: 'fraud-detection',
        component: FraudDetectionComponent,
        canActivate: [authGuard]
      },
      {
        path: 'fraud-alerts',
        component: FraudAlertsComponent,
        canActivate: [authGuard]
      },
      {
        path: 'fraud-alerts/:id',
        component: FraudAlertsComponent,
        canActivate: [authGuard]
      },
      {
        path: 'fraud-investigations',
        component: HomeComponent, // Temporaire - remplacer par le bon composant
        canActivate: [authGuard]
      },
      {
        path: 'statistics',
        component: HomeComponent, // Temporaire - remplacer par le bon composant
        canActivate: [authGuard]
      },

      // ✅ Routes existantes pour les livres (si nécessaires)
      {
        path: 'books',
        component: BookListComponent,
        canActivate: [authGuard]
      },
      {
        path: 'my-books',
        component: MyBooksComponent,
        canActivate: [authGuard]
      },
      {
        path: 'my-borrowed-books',
        component: BorrowedBookListComponent,
        canActivate: [authGuard]
      },
      {
        path: 'my-returned-books',
        component: ReturnedBooksComponent,
        canActivate: [authGuard]
      },
      {
        path: 'details/:bookId',
        component: BookDetailsComponent,
        canActivate: [authGuard]
      },
      {
        path: 'manage',
        component: ManageBookComponent,
        canActivate: [authGuard]
      },
      {
        path: 'manage/:bookId',
        component: ManageBookComponent,
        canActivate: [authGuard]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BookRoutingModule {
}

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';
import { BookRoutingModule } from './book-routing.module';
import { FraudDetectionComponent } from '../../components/fraud-detection/fraud-detection.component'; // Correct

import { MainComponent } from './pages/main/main.component';
import { MenuComponent } from './components/menu/menu.component';
import { BookListComponent } from './pages/book-list/book-list.component';
import { BookCardComponent } from './components/book-card/book-card.component';
import { MyBooksComponent } from './pages/my-books/my-books.component';
import { ManageBookComponent } from './pages/manage-book/manage-book.component';
import { FormsModule } from '@angular/forms';
import { BorrowedBookListComponent } from './pages/borrowed-book-list/borrowed-book-list.component';
import { RatingComponent } from './components/rating/rating.component';
import { ReturnedBooksComponent } from './pages/returned-books/returned-books.component';
import { BookDetailsComponent } from './pages/book-details/book-details.component';
import { HomeComponent } from './components/menu/home/home.component';
import { GestionAssuresComponent } from './components/menu/gestion-assures/gestion-assures.component';
import { SinistresComponent } from './components/menu/sinistres/sinistres.component';
import { HttpClientModule } from "@angular/common/http";
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { FraudCasesComponent } from './components/menu/fraud-cases/fraud-cases.component';

@NgModule({
  declarations: [
    MainComponent,
    MenuComponent,
    BookListComponent,
    BookCardComponent,
    MyBooksComponent,
    ManageBookComponent,
    BorrowedBookListComponent,
    RatingComponent,
    ReturnedBooksComponent,
    BookDetailsComponent,
    HomeComponent,
    GestionAssuresComponent,
    FraudDetectionComponent,
    FraudCasesComponent,
    SinistresComponent
  ],
  imports: [
    CommonModule,
    BookRoutingModule,
    HttpClientModule,
    FormsModule,
    RouterModule,
    ReactiveFormsModule,
    KeycloakAngularModule
  ],
  providers: [
    KeycloakService
  ],
  exports: [
    MenuComponent,
    BookCardComponent,
    GestionAssuresComponent,
    SinistresComponent,
    RatingComponent
  ]
} )
export class BookModule { }

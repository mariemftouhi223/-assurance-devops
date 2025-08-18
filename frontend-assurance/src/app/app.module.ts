import {APP_INITIALIZER, NgModule} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BookRoutingModule } from './modules/book/book-routing.module';


import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {HTTP_INTERCEPTORS, HttpClient, HttpClientModule} from '@angular/common/http';
import {HttpTokenInterceptor} from './services/interceptor/http-token.interceptor';
import { ActivateAccountComponent } from './pages/activate-account/activate-account.component';
import {CodeInputModule} from 'angular-code-input';
import {KeycloakService} from './services/keycloak/keycloak.service';
import {BookModule} from "@app/modules/book/book.module";
import { FraudDashboardComponent } from '@app/modules/book/components/menu/fraud-dashboard/fraud-dashboard.component';
import { NotificationService } from './services/notification.service';
import { NotificationPanelComponent } from './components/notification-panel/notification-panel.component';
import {FraudAlertsComponent} from "@app/components/fraud-alerts/fraud-alerts.component";

export function kcFactory(kcService: KeycloakService) {
  return () => kcService.init();
}

@NgModule({
  declarations: [
    AppComponent,
    NotificationPanelComponent,
    LoginComponent,
    RegisterComponent,
    ActivateAccountComponent,
    FraudAlertsComponent,
    FraudDashboardComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    CodeInputModule,
    BookModule,
    BookRoutingModule,
    ReactiveFormsModule

  ],
  providers: [
    NotificationService,
    HttpClient,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpTokenInterceptor,
      multi: true
    },
    {
      provide: APP_INITIALIZER,
      deps: [KeycloakService],
      useFactory: kcFactory,
      multi: true
    }

  ],
  bootstrap: [AppComponent]
})
export class AppModule { }

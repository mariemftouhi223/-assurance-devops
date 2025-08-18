import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-fraud-dashboard',
  templateUrl: './fraud-dashboard.component.html',
  styleUrls: ['./fraud-dashboard.component.scss']
})
export class FraudDashboardComponent implements OnInit {
  dashboardUrl!: SafeResourceUrl;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit() {
    // Utiliser DomSanitizer pour éviter les problèmes de sécurité avec les URLs
    this.dashboardUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      '/metabase/public/dashboard/8bc75fe0-1f23-4e2c-8b83-a10170fbfe54'
    );

  }
}

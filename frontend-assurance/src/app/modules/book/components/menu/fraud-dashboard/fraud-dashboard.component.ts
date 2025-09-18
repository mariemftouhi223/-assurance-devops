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
    const publicUrl =
      'http://localhost:3000/public/dashboard/8bc75fe0-1f23-4e2c-8b83-a10170fbfe54?bordered=true&titled=false';

    this.dashboardUrl = this.sanitizer.bypassSecurityTrustResourceUrl(publicUrl);
  }
}

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionAssuresComponent } from './gestion-assures.component';

describe('GestionAssuresComponent', () => {
  let component: GestionAssuresComponent;
  let fixture: ComponentFixture<GestionAssuresComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [GestionAssuresComponent]
    });
    fixture = TestBed.createComponent(GestionAssuresComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

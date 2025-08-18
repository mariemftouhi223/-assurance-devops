import { TestBed } from '@angular/core/testing';

import { AssureService } from './assure.service';

describe('AssureService', () => {
  let service: AssureService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AssureService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

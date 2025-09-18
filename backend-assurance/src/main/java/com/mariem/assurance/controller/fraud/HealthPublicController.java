package com.mariem.assurance.controller.fraud;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class HealthPublicController {

    @GetMapping(value = "/assures/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> assuresHealthz() {
        return ResponseEntity.ok(Map.of(
                "service", "assures",
                "status", "UP"
        ));
    }
}

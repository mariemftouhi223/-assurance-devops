package com.mariem.assurance.controller.fraud;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class TestController {

    @GetMapping("/api/v1/admin")
    @PreAuthorize("hasRole('ADMIN')") // <-- MODIFICATION ICI
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Tu es admin !");
    }

}
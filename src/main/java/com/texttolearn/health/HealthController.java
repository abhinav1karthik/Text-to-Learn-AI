package com.texttolearn.health;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    HealthResponse check() {
        return new HealthResponse("UP", "text-to-learn", Instant.now());
    }

    record HealthResponse(String status, String service, Instant checkedAt) {
    }
}

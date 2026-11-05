package com.vcraft.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Kiểm tra trạng thái hoạt động của hệ thống backend")
public class HealthController {

    @GetMapping
    @Operation(summary = "Kiểm tra trạng thái server (Health Check)")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "vCraft Backend - Làng gốm Bát Tràng",
                "timestamp", Instant.now().toString()
        ));
    }
}

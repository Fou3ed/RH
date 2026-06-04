package com.maram.payroll.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Lightweight system/info endpoints used to verify the API is alive.
 * Business endpoints live in their respective domain modules.
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    @Value("${spring.application.name:maram-payroll}")
    private String applicationName;

    @Value("${maram.version:0.1.0-SNAPSHOT}")
    private String version;

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "application", applicationName,
                "version", version,
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        );
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("message", "pong");
    }
}

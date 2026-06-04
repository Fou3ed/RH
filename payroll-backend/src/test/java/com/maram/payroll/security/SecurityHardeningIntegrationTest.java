package com.maram.payroll.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.maram.payroll.auth.dto.AuthResponse;
import com.maram.payroll.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security hardening checks: brute-force lockout, SQL-injection-safe search,
 * and the presence of security response headers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecurityHardeningIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    private HttpHeaders adminJson() {
        String token = rest.postForEntity("/auth/login",
                new LoginRequest("admin", "Admin@123!"), AuthResponse.class).getBody().token();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void accountLocksAfterFiveFailedAttempts() {
        // Create a dedicated user so we don't lock the shared admin.
        HttpHeaders admin = adminJson();
        rest.exchange("/users", HttpMethod.POST, new HttpEntity<>("""
                {"username":"lockme","email":"lockme@maram.local","password":"Password123!","roles":["EMPLOYEE"]}
                """, admin), JsonNode.class);

        // 5 failed attempts → account locked.
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> bad = rest.postForEntity("/auth/login",
                    new LoginRequest("lockme", "wrong-password"), String.class);
            assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // Correct password now also fails — the account is locked.
        ResponseEntity<String> afterLock = rest.postForEntity("/auth/login",
                new LoginRequest("lockme", "Password123!"), String.class);
        assertThat(afterLock.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void searchParameterIsNotSqlInjectable() {
        HttpHeaders admin = adminJson();
        // A classic injection payload must be treated as literal text → 200, zero matches, no error.
        String payload = "%27%3B%20DROP%20TABLE%20employees%3B%20--"; // '; DROP TABLE employees; --
        ResponseEntity<JsonNode> response = rest.exchange(
                "/employees?search=" + payload, HttpMethod.GET, new HttpEntity<>(admin), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("total").asInt()).isZero();

        // The table still exists (a follow-up query works).
        ResponseEntity<JsonNode> sanity = rest.exchange(
                "/employees", HttpMethod.GET, new HttpEntity<>(admin), JsonNode.class);
        assertThat(sanity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void securityHeadersArePresent() {
        ResponseEntity<String> response = rest.getForEntity("/system/ping", String.class);
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Content-Security-Policy")).contains("frame-ancestors 'none'");
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }
}

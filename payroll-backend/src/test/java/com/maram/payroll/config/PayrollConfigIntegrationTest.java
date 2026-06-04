package com.maram.payroll.config;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack payroll-configuration test: salary-scale year-over-year validation,
 * IRPP bracket overlap rejection, and the payroll-period lifecycle (duplicate
 * rejection + valid/invalid status transitions).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PayrollConfigIntegrationTest {

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

    private HttpHeaders auth() {
        String token = rest.postForEntity("/auth/login",
                new LoginRequest("admin", "Admin@123!"), AuthResponse.class).getBody().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void salaryScaleYearOverYearValidation() {
        HttpHeaders headers = auth();

        // 2025 baseline (category 1 = CAT1, échelon 1, multiplier 4.0).
        post(headers, "/config/salary-scales",
                """
                {"categoryId":1,"echelon":1,"year":2025,"salaryMultiplier":4.000}
                """, HttpStatus.CREATED);

        // 2026 lower multiplier → rejected (must not decrease).
        post(headers, "/config/salary-scales",
                """
                {"categoryId":1,"echelon":1,"year":2026,"salaryMultiplier":3.900}
                """, HttpStatus.BAD_REQUEST);

        // 2026 higher multiplier → accepted.
        post(headers, "/config/salary-scales",
                """
                {"categoryId":1,"echelon":1,"year":2026,"salaryMultiplier":4.100}
                """, HttpStatus.CREATED);
    }

    @Test
    void irppBracketOverlapIsRejected() {
        HttpHeaders headers = auth();

        post(headers, "/config/tax",
                """
                {"taxYear":2030,"taxType":"IRPP","minTaxableIncome":0,"maxTaxableIncome":2000,"taxRate":0}
                """, HttpStatus.CREATED);

        // Overlaps [0,2000) → rejected.
        post(headers, "/config/tax",
                """
                {"taxYear":2030,"taxType":"IRPP","minTaxableIncome":1500,"maxTaxableIncome":5000,"taxRate":10}
                """, HttpStatus.BAD_REQUEST);

        // Adjacent (no overlap) → accepted.
        post(headers, "/config/tax",
                """
                {"taxYear":2030,"taxType":"IRPP","minTaxableIncome":2000,"maxTaxableIncome":5000,"taxRate":10}
                """, HttpStatus.CREATED);
    }

    @Test
    void payrollPeriodLifecycle() {
        HttpHeaders headers = auth();

        // Create a period.
        ResponseEntity<JsonNode> created = rest.exchange(
                "/payroll-periods", HttpMethod.POST,
                new HttpEntity<>("""
                        {"periodMonth":7,"periodYear":2027}
                        """, headers), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = created.getBody().get("id").asLong();
        assertThat(created.getBody().get("status").asText()).isEqualTo("DRAFT");
        assertThat(created.getBody().get("periodCode").asText()).isEqualTo("2027-07");

        // Duplicate month/year → 409.
        ResponseEntity<Map> dup = rest.exchange(
                "/payroll-periods", HttpMethod.POST,
                new HttpEntity<>("""
                        {"periodMonth":7,"periodYear":2027}
                        """, headers), Map.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Invalid transition DRAFT → PAID → 400.
        ResponseEntity<Map> bad = rest.exchange(
                "/payroll-periods/" + id + "/transition", HttpMethod.POST,
                new HttpEntity<>("""
                        {"status":"PAID"}
                        """, headers), Map.class);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Valid transition DRAFT → LOCKED → 200.
        ResponseEntity<JsonNode> locked = rest.exchange(
                "/payroll-periods/" + id + "/transition", HttpMethod.POST,
                new HttpEntity<>("""
                        {"status":"LOCKED"}
                        """, headers), JsonNode.class);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(locked.getBody().get("status").asText()).isEqualTo("LOCKED");
    }

    @Test
    void allowanceConfigIsSeeded() {
        HttpHeaders headers = auth();
        ResponseEntity<JsonNode> list = rest.exchange(
                "/config/allowances", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        // V11 seeds 5 default allowances.
        assertThat(list.getBody().size()).isGreaterThanOrEqualTo(5);
    }

    private void post(HttpHeaders headers, String path, String body, HttpStatus expected) {
        ResponseEntity<String> response = rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode())
                .as("POST %s expected %s but body was %s", path, expected, response.getBody())
                .isEqualTo(expected);
    }
}

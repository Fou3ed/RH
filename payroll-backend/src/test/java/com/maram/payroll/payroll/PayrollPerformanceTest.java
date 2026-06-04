package com.maram.payroll.payroll;

import com.fasterxml.jackson.databind.JsonNode;
import com.maram.payroll.auth.dto.AuthResponse;
import com.maram.payroll.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scale check for the payroll engine: seed many employees, calculate the whole
 * period, and assert it completes well within a generous time budget — guards
 * against accidental O(n²) / N+1 regressions in the orchestration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PayrollPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PayrollPerformanceTest.class);
    private static final int EMPLOYEE_COUNT = 150;

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

    @Test
    void calculatesHundredsOfEmployeesQuickly() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(rest.postForEntity("/auth/login",
                new LoginRequest("admin", "Admin@123!"), AuthResponse.class).getBody().token());

        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        rest.exchange("/config/salary-scales", HttpMethod.POST, new HttpEntity<>("""
                {"categoryId":1,"echelon":3,"year":2026,"salaryMultiplier":4.000}
                """, h), JsonNode.class);

        for (int i = 1; i <= EMPLOYEE_COUNT; i++) {
            rest.exchange("/employees", HttpMethod.POST, new HttpEntity<>("""
                    {"employeeId":"PERF-%d","firstName":"Perf","lastName":"%d","hireDate":"2020-01-01",
                     "departmentId":%d,"categoryId":1,"echelon":3,"baseSalary":560}
                    """.formatted(i, i, deptId), h), JsonNode.class);
        }

        Long periodId = rest.exchange("/payroll-periods", HttpMethod.POST, new HttpEntity<>("""
                {"periodMonth":12,"periodYear":2026}
                """, h), JsonNode.class).getBody().get("id").asLong();

        long start = System.nanoTime();
        JsonNode summary = rest.exchange("/payroll/calculate?periodId=" + periodId, HttpMethod.POST,
                new HttpEntity<>(h), JsonNode.class).getBody();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Calculated {} payrolls in {} ms", EMPLOYEE_COUNT, elapsedMs);

        assertThat(summary.get("calculated").asInt()).isEqualTo(EMPLOYEE_COUNT);
        // Very generous budget — really we want to catch pathological regressions.
        assertThat(elapsedMs).isLessThan(30_000L);
    }
}

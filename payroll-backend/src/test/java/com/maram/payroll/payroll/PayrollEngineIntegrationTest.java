package com.maram.payroll.payroll;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end payroll engine test. Seeds an employee + salary scale + period, runs
 * the calculation, and asserts every computed component against a hand-worked
 * expected value (full attendance, seeded 2026 tax + allowance config).
 *
 * <pre>
 * base       = 560 × 4.000                       = 2240.00
 * adjusted   = 2240 × (26/26)                     = 2240.00
 * allowances = 9.04 + 87.17 + 16.01 + 22.50 + 10.00 (2 children) = 144.72
 * gross      = 2240.00 + 144.72                   = 2384.72
 * cnss       = 2384.72 × 5.95%                     = 141.89
 * irpp       = (2242.83 − 2000) × 10%             = 24.28   (taxable = gross − cnss)
 * deductions = 24.28 + 141.89                      = 166.17
 * net        = 2384.72 − 166.17                    = 2218.55
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PayrollEngineIntegrationTest {

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

    private JsonNode post(HttpHeaders h, String path, String body) {
        ResponseEntity<JsonNode> r = rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), JsonNode.class);
        assertThat(r.getStatusCode().is2xxSuccessful())
                .as("POST %s -> %s : %s", path, r.getStatusCode(), r.getBody())
                .isTrue();
        return r.getBody();
    }

    @Test
    void calculatesFullPayrollMatchingExpectedComponents() {
        HttpHeaders h = auth();
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        // Employee: configured base 560, échelon 3, category CAT1 (id 1), 2 children.
        Long employeeId = post(h, "/employees", """
                {"employeeId":"PAY-1","firstName":"Pay","lastName":"Roll","hireDate":"2020-01-01",
                 "departmentId":%d,"categoryId":1,"echelon":3,"baseSalary":560,"numberOfChildren":2}
                """.formatted(deptId)).get("id").asLong();

        // Salary scale CAT1 / échelon 3 / 2026 = 4.000.
        post(h, "/config/salary-scales", """
                {"categoryId":1,"echelon":3,"year":2026,"salaryMultiplier":4.000}
                """);

        // Payroll period June 2026.
        Long periodId = post(h, "/payroll-periods", """
                {"periodMonth":6,"periodYear":2026}
                """).get("id").asLong();

        // Run the engine.
        JsonNode summary = post(h, "/payroll/calculate?periodId=" + periodId, "");
        // Exactly one employee in this DB has a scale for échelon 3 → one row, net 2218.55.
        // (skippedCount is not asserted: other test fixtures may add scale-less employees.)
        assertThat(summary.get("calculated").asInt()).isEqualTo(1);
        assertThat(summary.get("totalNet").asDouble()).isEqualTo(2218.55);

        // Inspect the calculated row.
        JsonNode rows = rest.exchange("/payroll?periodId=" + periodId, HttpMethod.GET,
                new HttpEntity<>(h), JsonNode.class).getBody();
        assertThat(rows).hasSize(1);
        JsonNode row = rows.get(0);
        assertThat(row.get("employeeId").asLong()).isEqualTo(employeeId);
        assertThat(row.get("baseSalary").asDouble()).isEqualTo(2240.00);
        assertThat(row.get("totalAllowances").asDouble()).isEqualTo(144.72);
        assertThat(row.get("grossSalary").asDouble()).isEqualTo(2384.72);
        assertThat(row.get("cnssContribution").asDouble()).isEqualTo(141.89);
        assertThat(row.get("incomeTaxIrpp").asDouble()).isEqualTo(24.28);
        assertThat(row.get("totalDeductions").asDouble()).isEqualTo(166.17);
        assertThat(row.get("netSalary").asDouble()).isEqualTo(2218.55);
        assertThat(row.get("paymentStatus").asText()).isEqualTo("DRAFT");

        // Approve the row.
        long payrollId = row.get("id").asLong();
        JsonNode approved = post(h, "/payroll/" + payrollId + "/approve", "");
        assertThat(approved.get("paymentStatus").asText()).isEqualTo("APPROVED");
    }

    @Test
    void employeeWithoutSalaryScaleIsSkipped() {
        HttpHeaders h = auth();
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        post(h, "/employees", """
                {"employeeId":"PAY-2","firstName":"No","lastName":"Scale","hireDate":"2020-01-01",
                 "departmentId":%d,"categoryId":1,"echelon":9,"baseSalary":600}
                """.formatted(deptId));

        Long periodId = post(h, "/payroll-periods", """
                {"periodMonth":8,"periodYear":2026}
                """).get("id").asLong();

        // No scale for échelon 9 → that employee is skipped, not a hard failure.
        JsonNode summary = post(h, "/payroll/calculate?periodId=" + periodId, "");
        assertThat(summary.get("skippedCount").asInt()).isGreaterThanOrEqualTo(1);
    }
}

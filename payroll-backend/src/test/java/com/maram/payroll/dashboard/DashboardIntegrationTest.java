package com.maram.payroll.dashboard;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the HR and payroll dashboard aggregates against seeded data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DashboardIntegrationTest {

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
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void hrDashboardReflectsHeadcountAndDepartments() {
        HttpHeaders h = auth();
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        // Create two employees.
        for (int i = 1; i <= 2; i++) {
            rest.exchange("/employees", HttpMethod.POST, new HttpEntity<>("""
                    {"employeeId":"HR-%d","firstName":"Emp","lastName":"%d","hireDate":"%s",
                     "departmentId":%d,"categoryId":1}
                    """.formatted(i, i, java.time.LocalDate.now().toString(), deptId), h), JsonNode.class);
        }

        JsonNode hr = rest.exchange("/dashboard/hr", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class).getBody();
        assertThat(hr.get("totalEmployees").asLong()).isEqualTo(2);
        assertThat(hr.get("activeEmployees").asLong()).isEqualTo(2);
        assertThat(hr.get("newHiresThisMonth").asLong()).isEqualTo(2);
        assertThat(hr.get("byDepartment")).isNotEmpty();
        assertThat(hr.get("byDepartment").get(0).get("count").asLong()).isEqualTo(2);
        assertThat(hr.get("recentHires")).hasSize(2);
    }

    @Test
    void payrollDashboardReflectsLatestPeriod() {
        HttpHeaders h = auth();
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        rest.exchange("/employees", HttpMethod.POST, new HttpEntity<>("""
                {"employeeId":"PD-1","firstName":"Pay","lastName":"Dash","hireDate":"2020-01-01",
                 "departmentId":%d,"categoryId":1,"echelon":3,"baseSalary":560}
                """.formatted(deptId), h), JsonNode.class);
        rest.exchange("/config/salary-scales", HttpMethod.POST, new HttpEntity<>("""
                {"categoryId":1,"echelon":3,"year":2026,"salaryMultiplier":4.000}
                """, h), JsonNode.class);
        Long periodId = rest.exchange("/payroll-periods", HttpMethod.POST, new HttpEntity<>("""
                {"periodMonth":11,"periodYear":2026}
                """, h), JsonNode.class).getBody().get("id").asLong();
        rest.exchange("/payroll/calculate?periodId=" + periodId, HttpMethod.POST, new HttpEntity<>(h), JsonNode.class);

        JsonNode pd = rest.exchange("/dashboard/payroll", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class).getBody();
        assertThat(pd.get("periodCode").asText()).isEqualTo("2026-11");
        assertThat(pd.get("calculated").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(pd.get("totalNet").asDouble()).isGreaterThan(0.0);
    }

    @Test
    void dashboardRequiresAuthentication() {
        var response = rest.getForEntity("/dashboard/hr", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

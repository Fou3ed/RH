package com.maram.payroll.attendance;

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
 * Full-stack attendance test: record → duplicate-rejection → future-date
 * rejection → monthly summary, against a real PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AttendanceIntegrationTest {

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

    private long createEmployee(HttpHeaders headers, String empId) {
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
                .getBody().get(0).get("id").asLong();
        String body = """
                {"employeeId":"%s","firstName":"Att","lastName":"Test",
                 "hireDate":"2021-01-01","departmentId":%d,"categoryId":1}
                """.formatted(empId, deptId);
        return rest.exchange("/employees", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class)
                .getBody().get("id").asLong();
    }

    @Test
    void recordThenSummary() {
        HttpHeaders headers = auth();
        long employeeId = createEmployee(headers, "ATT-1");

        // Record a present day and a half day in the same month.
        String present = """
                {"employeeId":%d,"attendanceDate":"2026-03-02","status":"PRESENT","hoursWorked":8}
                """.formatted(employeeId);
        ResponseEntity<JsonNode> created = rest.exchange(
                "/attendance", HttpMethod.POST, new HttpEntity<>(present, headers), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("attendanceStatus").asText()).isEqualTo("PRESENT");

        String half = """
                {"employeeId":%d,"attendanceDate":"2026-03-03","status":"HALF_DAY","hoursWorked":4}
                """.formatted(employeeId);
        rest.exchange("/attendance", HttpMethod.POST, new HttpEntity<>(half, headers), JsonNode.class);

        // Summary: 1 present + 1 half-day → daysWorked 1.5
        ResponseEntity<JsonNode> summary = rest.exchange(
                "/attendance/employee/" + employeeId + "/summary?year=2026&month=3",
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summary.getBody().get("presentDays").asInt()).isEqualTo(1);
        assertThat(summary.getBody().get("halfDays").asInt()).isEqualTo(1);
        assertThat(summary.getBody().get("daysWorked").asDouble()).isEqualTo(1.5);
    }

    @Test
    void duplicateSameDayIsRejected() {
        HttpHeaders headers = auth();
        long employeeId = createEmployee(headers, "ATT-2");
        String body = """
                {"employeeId":%d,"attendanceDate":"2026-02-10","status":"PRESENT","hoursWorked":8}
                """.formatted(employeeId);

        rest.exchange("/attendance", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        ResponseEntity<Map> dup = rest.exchange(
                "/attendance", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void futureDateIsRejected() {
        HttpHeaders headers = auth();
        long employeeId = createEmployee(headers, "ATT-3");
        String body = """
                {"employeeId":%d,"attendanceDate":"2999-01-01","status":"PRESENT","hoursWorked":8}
                """.formatted(employeeId);
        ResponseEntity<Map> response = rest.exchange(
                "/attendance", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void hoursOutOfRangeIsRejected() {
        HttpHeaders headers = auth();
        long employeeId = createEmployee(headers, "ATT-4");
        String body = """
                {"employeeId":%d,"attendanceDate":"2026-01-05","status":"PRESENT","hoursWorked":20}
                """.formatted(employeeId);
        ResponseEntity<Map> response = rest.exchange(
                "/attendance", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

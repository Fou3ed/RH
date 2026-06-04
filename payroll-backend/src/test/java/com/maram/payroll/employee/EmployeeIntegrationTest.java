package com.maram.payroll.employee;

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
 * Full-stack employee-management test. Boots the app against real PostgreSQL and
 * drives the create → list → get → update → delete lifecycle as the admin user,
 * then verifies validation and RBAC behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EmployeeIntegrationTest {

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

    private HttpHeaders adminHeaders() {
        AuthResponse auth = rest.postForEntity(
                "/auth/login", new LoginRequest("admin", "Admin@123!"), AuthResponse.class).getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(auth.token());
        return headers;
    }

    @Test
    void employeeCrudLifecycle() {
        HttpHeaders headers = adminHeaders();

        // A department + category exist from seed data (GEN, CAT1). Look them up.
        Long deptId = firstId(rest.exchange("/departments", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getBody());
        Long categoryId = 1L; // CAT1 from V6 seed

        // CREATE
        String body = """
                {
                  "employeeId": "E-1001",
                  "firstName": "Sonia",
                  "lastName": "Chafai",
                  "hireDate": "2020-06-15",
                  "departmentId": %d,
                  "categoryId": %d,
                  "echelon": 3,
                  "email": "sonia@maram.tn",
                  "gender": "H"
                }
                """.formatted(deptId, categoryId);

        ResponseEntity<JsonNode> created = rest.exchange(
                "/employees", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long employeePk = created.getBody().get("id").asLong();
        assertThat(created.getBody().get("fullName").asText()).isEqualTo("Sonia Chafai");

        // LIST (paginated envelope)
        ResponseEntity<JsonNode> list = rest.exchange(
                "/employees?page=0&limit=25", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().get("total").asLong()).isEqualTo(1);
        assertThat(list.getBody().get("data")).hasSize(1);

        // GET
        ResponseEntity<JsonNode> fetched = rest.exchange(
                "/employees/" + employeePk, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(fetched.getBody().get("employeeId").asText()).isEqualTo("E-1001");

        // UPDATE
        String updateBody = """
                {
                  "firstName": "Sonia",
                  "lastName": "Ben Salah",
                  "hireDate": "2020-06-15",
                  "departmentId": %d,
                  "categoryId": %d,
                  "echelon": 4
                }
                """.formatted(deptId, categoryId);
        ResponseEntity<JsonNode> updated = rest.exchange(
                "/employees/" + employeePk, HttpMethod.PUT, new HttpEntity<>(updateBody, headers), JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("fullName").asText()).isEqualTo("Sonia Ben Salah");
        assertThat(updated.getBody().get("echelon").asInt()).isEqualTo(4);

        // DELETE
        ResponseEntity<Void> deleted = rest.exchange(
                "/employees/" + employeePk, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void createWithFutureHireDateIsRejected() {
        HttpHeaders headers = adminHeaders();
        Long deptId = firstId(rest.exchange("/departments", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getBody());

        String body = """
                {
                  "employeeId": "E-9999",
                  "firstName": "Future",
                  "lastName": "Hire",
                  "hireDate": "2999-01-01",
                  "departmentId": %d,
                  "categoryId": 1
                }
                """.formatted(deptId);

        ResponseEntity<Map> response = rest.exchange(
                "/employees", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void employeesEndpointRequiresAuthentication() {
        ResponseEntity<String> response = rest.getForEntity("/employees", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Long firstId(JsonNode array) {
        assertThat(array.isArray()).isTrue();
        assertThat(array).isNotEmpty();
        return array.get(0).get("id").asLong();
    }
}

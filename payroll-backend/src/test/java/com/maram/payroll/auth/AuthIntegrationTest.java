package com.maram.payroll.auth;

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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack auth test. Boots the whole application against a real PostgreSQL
 * (so Flyway V1–V7 run and Hibernate {@code ddl-auto: validate} verifies every
 * entity maps to the migrated schema), then exercises the login/RBAC flows.
 *
 * <p>Note: {@link TestRestTemplate} automatically prepends the configured
 * {@code server.servlet.context-path} (/api), so test paths omit it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void adminCanLoginThenAccessProtectedEndpoint() {
        ResponseEntity<AuthResponse> login = rest.postForEntity(
                "/auth/login", new LoginRequest("admin", "Admin@123!"), AuthResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        assertThat(login.getBody().token()).isNotBlank();
        assertThat(login.getBody().refreshToken()).isNotBlank();
        assertThat(login.getBody().user().roles()).contains("ADMIN");
        assertThat(login.getBody().user().permissions()).contains("user.manage");

        // The access token unlocks an admin-only endpoint.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getBody().token());
        ResponseEntity<String> users = rest.exchange(
                "/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() {
        ResponseEntity<String> response = rest.getForEntity("/users", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidCredentialsReturn401() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/auth/login", new LoginRequest("admin", "wrong-password"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

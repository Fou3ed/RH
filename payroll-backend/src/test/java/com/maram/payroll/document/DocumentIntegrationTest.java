package com.maram.payroll.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.maram.payroll.auth.dto.AuthResponse;
import com.maram.payroll.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end document storage test: boots PostgreSQL + a MinIO container, then
 * uploads → lists → downloads → deletes a document for an employee.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DocumentIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin123")
            .withCommand("server /data")
            .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("maram.minio.endpoint",
                () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("maram.minio.access-key", () -> "minioadmin");
        registry.add("maram.minio.secret-key", () -> "minioadmin123");
        registry.add("maram.minio.bucket", () -> "payroll-test");
    }

    @Autowired
    private TestRestTemplate rest;

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void uploadListDownloadDelete() {
        String token = rest.postForEntity("/auth/login",
                new LoginRequest("admin", "Admin@123!"), AuthResponse.class).getBody().token();
        HttpHeaders auth = bearer(token);

        // Resolve a department, then create an employee to attach a document to.
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(auth), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        HttpHeaders jsonAuth = bearer(token);
        jsonAuth.setContentType(MediaType.APPLICATION_JSON);
        String empBody = """
                {"employeeId":"DOC-1","firstName":"Doc","lastName":"Owner",
                 "hireDate":"2021-05-01","departmentId":%d,"categoryId":1}
                """.formatted(deptId);
        Long employeeId = rest.exchange("/employees", HttpMethod.POST,
                new HttpEntity<>(empBody, jsonAuth), JsonNode.class).getBody().get("id").asLong();

        // Upload a small PDF.
        byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes();
        ByteArrayResource resource = new ByteArrayResource(pdf) {
            @Override
            public String getFilename() {
                return "contract.pdf";
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("type", "CONTRACT");
        // The file part carries its own PDF content type so DocumentService accepts it.
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.APPLICATION_PDF);
        form.add("file", new HttpEntity<>(resource, partHeaders));
        HttpHeaders multipart = bearer(token);
        multipart.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<JsonNode> upload = rest.exchange(
                "/employees/" + employeeId + "/documents", HttpMethod.POST,
                new HttpEntity<>(form, multipart), JsonNode.class);
        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long documentId = upload.getBody().get("id").asLong();
        assertThat(upload.getBody().get("fileName").asText()).isEqualTo("contract.pdf");

        // List shows it.
        ResponseEntity<JsonNode> list = rest.exchange(
                "/employees/" + employeeId + "/documents", HttpMethod.GET,
                new HttpEntity<>(auth), JsonNode.class);
        assertThat(list.getBody()).hasSize(1);

        // Download returns the bytes.
        ResponseEntity<byte[]> download = rest.exchange(
                "/documents/" + documentId + "/download", HttpMethod.GET,
                new HttpEntity<>(auth), byte[].class);
        assertThat(download.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download.getBody()).isEqualTo(pdf);

        // Delete removes it.
        ResponseEntity<Void> delete = rest.exchange(
                "/documents/" + documentId, HttpMethod.DELETE,
                new HttpEntity<>(auth), Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<JsonNode> after = rest.exchange(
                "/employees/" + employeeId + "/documents", HttpMethod.GET,
                new HttpEntity<>(auth), JsonNode.class);
        assertThat(after.getBody()).isEmpty();
    }
}

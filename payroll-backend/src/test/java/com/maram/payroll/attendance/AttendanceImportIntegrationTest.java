package com.maram.payroll.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.maram.payroll.auth.dto.AuthResponse;
import com.maram.payroll.auth.dto.LoginRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the monthly attendance grid import: a clean grid commits and the
 * resulting records show up in the employee's summary.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AttendanceImportIntegrationTest {

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
        headers.setBearerAuth(token);
        return headers;
    }

    private long createEmployee(HttpHeaders jsonHeaders, String empId) {
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(jsonHeaders), JsonNode.class)
                .getBody().get(0).get("id").asLong();
        String body = """
                {"employeeId":"%s","firstName":"Grid","lastName":"Import",
                 "hireDate":"2021-01-01","departmentId":%d,"categoryId":1}
                """.formatted(empId, deptId);
        return rest.exchange("/employees", HttpMethod.POST, new HttpEntity<>(body, jsonHeaders), JsonNode.class)
                .getBody().get("id").asLong();
    }

    /** Header: employeeId | 1 | 2 | 3 ; one data row with codes 8,4,A. */
    private byte[] grid(String employeeId) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("attendance");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("employeeId");
            header.createCell(1).setCellValue("1");
            header.createCell(2).setCellValue("2");
            header.createCell(3).setCellValue("3");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(employeeId);
            row.createCell(1).setCellValue("8");
            row.createCell(2).setCellValue("4");
            row.createCell(3).setCellValue("A");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> multipart(byte[] xlsx, String token) {
        ByteArrayResource resource = new ByteArrayResource(xlsx) {
            @Override
            public String getFilename() {
                return "attendance.xlsx";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void gridImportCommitsAndShowsInSummary() {
        HttpHeaders jsonAuth = auth();
        jsonAuth.setContentType(MediaType.APPLICATION_JSON);
        long employeeId = createEmployee(jsonAuth, "GRID-1");
        String token = jsonAuth.getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length());

        byte[] xlsx = grid("GRID-1");

        // Preview: 1 valid row, 3 recognized day records.
        ResponseEntity<JsonNode> preview = rest.postForEntity(
                "/attendance/import/preview?year=2026&month=4", multipart(xlsx, token), JsonNode.class);
        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().get("validRows").asInt()).isEqualTo(1);
        assertThat(preview.getBody().get("totalRecords").asInt()).isEqualTo(3);

        // Commit.
        ResponseEntity<JsonNode> commit = rest.postForEntity(
                "/attendance/import?year=2026&month=4", multipart(xlsx, token), JsonNode.class);
        assertThat(commit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(commit.getBody().get("imported").asInt()).isEqualTo(3);

        // Summary reflects the imported records: 1 present, 1 half, 1 absent.
        HttpHeaders bearer = new HttpHeaders();
        bearer.setBearerAuth(token);
        ResponseEntity<JsonNode> summary = rest.exchange(
                "/attendance/employee/" + employeeId + "/summary?year=2026&month=4",
                HttpMethod.GET, new HttpEntity<>(bearer), JsonNode.class);
        assertThat(summary.getBody().get("presentDays").asInt()).isEqualTo(1);
        assertThat(summary.getBody().get("halfDays").asInt()).isEqualTo(1);
        assertThat(summary.getBody().get("absentDays").asInt()).isEqualTo(1);
        assertThat(summary.getBody().get("daysWorked").asDouble()).isEqualTo(1.5);
    }
}

package com.maram.payroll.employee;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the .xlsx import: a clean file commits atomically, while a file with a
 * bad row is rejected with per-row errors and writes nothing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EmployeeImportIntegrationTest {

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

    private static final String[] HEADER = {
            "employeeId", "firstName", "lastName", "hireDate", "departmentCode", "categoryCode", "echelon", "email"
    };

    private String token() {
        return rest.postForEntity("/auth/login", new LoginRequest("admin", "Admin@123!"), AuthResponse.class)
                .getBody().token();
    }

    private byte[] workbook(List<String[]> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("employees");
            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADER.length; c++) {
                header.createCell(c).setCellValue(HEADER[c]);
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = rows.get(r);
                for (int c = 0; c < values.length; c++) {
                    if (values[c] != null) {
                        row.createCell(c).setCellValue(values[c]);
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> multipart(byte[] xlsx, String authToken) {
        ByteArrayResource resource = new ByteArrayResource(xlsx) {
            @Override
            public String getFilename() {
                return "employees.xlsx";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(authToken);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void cleanFileImportsAtomically() {
        String authToken = token();
        byte[] xlsx = workbook(List.of(
                new String[]{"IMP-1", "Amine", "Trabelsi", "2021-01-10", "GEN", "CAT1", "2", "amine@maram.tn"},
                new String[]{"IMP-2", "Leila", "Gharbi", "2019-09-01", "GEN", "CAT1", "3", "leila@maram.tn"}
        ));

        // Preview reports two valid rows.
        ResponseEntity<JsonNode> preview = rest.postForEntity(
                "/employees/import/preview", multipart(xlsx, authToken), JsonNode.class);
        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().get("validRows").asInt()).isEqualTo(2);
        assertThat(preview.getBody().get("invalidRows").asInt()).isZero();

        // Commit imports them.
        ResponseEntity<JsonNode> commit = rest.postForEntity(
                "/employees/import", multipart(xlsx, authToken), JsonNode.class);
        assertThat(commit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(commit.getBody().get("imported").asInt()).isEqualTo(2);

        // They are now listable.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        ResponseEntity<JsonNode> list = rest.exchange(
                "/employees?search=IMP", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(list.getBody().get("total").asInt()).isEqualTo(2);
    }

    @Test
    void fileWithInvalidRowIsRejectedWithErrors() {
        String authToken = token();
        byte[] xlsx = workbook(List.of(
                new String[]{"IMP-9", "Valid", "Person", "2020-02-02", "GEN", "CAT1", "1", "ok@maram.tn"},
                // Unknown department + future hire date → invalid
                new String[]{"IMP-10", "Bad", "Row", "2999-01-01", "NOPE", "CAT1", "1", "bad@maram.tn"}
        ));

        ResponseEntity<JsonNode> commit = rest.postForEntity(
                "/employees/import", multipart(xlsx, authToken), JsonNode.class);

        assertThat(commit.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(commit.getBody().get("invalidRows").asInt()).isEqualTo(1);

        // Nothing was written (atomic) — the valid row is absent too.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        ResponseEntity<JsonNode> list = rest.exchange(
                "/employees?search=IMP-9", org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(list.getBody().get("total").asInt()).isZero();
    }
}

package com.maram.payroll.reporting;

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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reporting end-to-end: seed + calculate a payroll, then download the payslip PDF,
 * the payroll Excel, and the IRPP/CNSS declaration CSVs — verifying each artifact's
 * format and that it contains the expected data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReportingIntegrationTest {

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

    private JsonNode post(HttpHeaders h, String path, String body) {
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, h), JsonNode.class).getBody();
    }

    @Test
    void generatesPayslipExcelAndTaxDeclarations() {
        HttpHeaders h = auth();
        Long deptId = rest.exchange("/departments", HttpMethod.GET, new HttpEntity<>(h), JsonNode.class)
                .getBody().get(0).get("id").asLong();

        post(h, "/employees", """
                {"employeeId":"RPT-1","firstName":"Report","lastName":"Target","hireDate":"2020-01-01",
                 "departmentId":%d,"categoryId":1,"echelon":3,"baseSalary":560,"cnssNumber":"CN-RPT-1"}
                """.formatted(deptId));
        post(h, "/config/salary-scales", """
                {"categoryId":1,"echelon":3,"year":2026,"salaryMultiplier":4.000}
                """);
        Long periodId = post(h, "/payroll-periods", """
                {"periodMonth":9,"periodYear":2026}
                """).get("id").asLong();
        post(h, "/payroll/calculate?periodId=" + periodId, "");

        long payrollId = rest.exchange("/payroll?periodId=" + periodId, HttpMethod.GET,
                new HttpEntity<>(h), JsonNode.class).getBody().get(0).get("id").asLong();

        HttpHeaders bearer = new HttpHeaders();
        bearer.setBearerAuth(h.getFirst(HttpHeaders.AUTHORIZATION).substring("Bearer ".length()));

        // Payslip PDF
        ResponseEntity<byte[]> pdf = rest.exchange("/reports/payslip/" + payrollId, HttpMethod.GET,
                new HttpEntity<>(bearer), byte[].class);
        assertThat(pdf.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(new String(pdf.getBody(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        // Payroll Excel (.xlsx is a ZIP → starts with "PK")
        ResponseEntity<byte[]> xlsx = rest.exchange("/reports/payroll/export?periodId=" + periodId,
                HttpMethod.GET, new HttpEntity<>(bearer), byte[].class);
        assertThat(xlsx.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(xlsx.getBody()[0]).isEqualTo((byte) 'P');
        assertThat(xlsx.getBody()[1]).isEqualTo((byte) 'K');

        // IRPP CSV
        ResponseEntity<String> irpp = rest.exchange("/reports/tax/irpp?periodId=" + periodId,
                HttpMethod.GET, new HttpEntity<>(bearer), String.class);
        assertThat(irpp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(irpp.getBody()).contains("employee_id,full_name,cnss_number,gross,taxable,irpp");
        assertThat(irpp.getBody()).contains("RPT-1");

        // CNSS CSV
        ResponseEntity<String> cnss = rest.exchange("/reports/tax/cnss?periodId=" + periodId,
                HttpMethod.GET, new HttpEntity<>(bearer), String.class);
        assertThat(cnss.getBody()).contains("employee_id,full_name,cnss_number,gross,cnss");
        assertThat(cnss.getBody()).contains("CN-RPT-1");
    }
}

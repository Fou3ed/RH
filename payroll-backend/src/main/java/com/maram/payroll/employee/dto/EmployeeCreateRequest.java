package com.maram.payroll.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank @Size(max = 20) String employeeId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        LocalDate dateOfBirth,
        @Pattern(regexp = "[MH]", message = "gender must be M or H") String gender,
        @NotNull @PastOrPresent(message = "hire date cannot be in the future") LocalDate hireDate,
        @NotNull Long departmentId,
        Long positionId,
        @NotNull Long categoryId,
        @Min(1) @Max(14) Integer echelon,
        BigDecimal baseSalary,
        String familyStatus,
        @Min(0) Integer numberOfChildren,
        String nationalId,
        @Size(max = 20) String cnssNumber,
        String phoneNumber,
        @Email String email,
        String address,
        String city,
        String zipCode,
        String paymentMethod,
        String bankAccountNumber,
        String bankCode) {
}

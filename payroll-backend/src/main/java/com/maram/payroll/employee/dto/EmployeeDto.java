package com.maram.payroll.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Full read projection of an employee, including resolved org names.
 */
public record EmployeeDto(
        Long id,
        String employeeId,
        String firstName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        LocalDate hireDate,
        Long departmentId,
        String departmentName,
        Long positionId,
        String positionName,
        Long categoryId,
        String categoryCode,
        Integer echelon,
        BigDecimal baseSalary,
        String familyStatus,
        Integer numberOfChildren,
        String nationalId,
        String cnssNumber,
        String phoneNumber,
        String email,
        String address,
        String city,
        String zipCode,
        String paymentMethod,
        String bankAccountNumber,
        String bankCode,
        String employmentStatus,
        LocalDate terminationDate,
        String terminationReason) {
}

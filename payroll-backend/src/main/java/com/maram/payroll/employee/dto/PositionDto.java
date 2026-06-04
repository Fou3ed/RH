package com.maram.payroll.employee.dto;

public record PositionDto(
        Long id,
        String code,
        String name,
        String description,
        Long departmentId,
        String departmentName,
        Integer salaryGrade) {
}

package com.maram.payroll.employee.dto;

public record DepartmentDto(
        Long id,
        String code,
        String name,
        String description,
        Long parentId,
        String parentName) {
}

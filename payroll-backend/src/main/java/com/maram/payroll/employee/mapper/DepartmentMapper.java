package com.maram.payroll.employee.mapper;

import com.maram.payroll.employee.dto.DepartmentDto;
import com.maram.payroll.employee.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    DepartmentDto toDto(Department department);
}

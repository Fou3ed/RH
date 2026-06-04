package com.maram.payroll.employee.mapper;

import com.maram.payroll.employee.dto.EmployeeDto;
import com.maram.payroll.employee.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "positionId", source = "position.id")
    @Mapping(target = "positionName", source = "position.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryCode", source = "category.code")
    EmployeeDto toDto(Employee employee);
}

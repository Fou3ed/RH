package com.maram.payroll.employee.mapper;

import com.maram.payroll.employee.dto.PositionDto;
import com.maram.payroll.employee.entity.Position;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PositionMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    PositionDto toDto(Position position);
}

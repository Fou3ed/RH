package com.maram.payroll.employee.service;

import com.maram.payroll.common.audit.AuditService;
import com.maram.payroll.common.dto.PageResponse;
import com.maram.payroll.common.exception.ConflictException;
import com.maram.payroll.common.exception.ResourceNotFoundException;
import com.maram.payroll.employee.dto.EmployeeCreateRequest;
import com.maram.payroll.employee.dto.EmployeeDto;
import com.maram.payroll.employee.dto.EmployeeUpdateRequest;
import com.maram.payroll.employee.entity.Department;
import com.maram.payroll.employee.entity.Employee;
import com.maram.payroll.employee.entity.Position;
import com.maram.payroll.employee.entity.SalaryCategory;
import com.maram.payroll.employee.mapper.EmployeeMapper;
import com.maram.payroll.employee.repository.DepartmentRepository;
import com.maram.payroll.employee.repository.EmployeeRepository;
import com.maram.payroll.employee.repository.PositionRepository;
import com.maram.payroll.employee.repository.SalaryCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final SalaryCategoryRepository categoryRepository;
    private final EmployeeMapper mapper;
    private final AuditService auditService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository,
                           SalaryCategoryRepository categoryRepository,
                           EmployeeMapper mapper,
                           AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> search(Long departmentId, String status, String search,
                                            int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("fullName").ascending());
        Page<Employee> result = employeeRepository.search(
                departmentId, emptyToNull(status), emptyToNull(search), pageable);
        return PageResponse.of(result, mapper::toDto);
    }

    @Transactional(readOnly = true)
    public EmployeeDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public EmployeeDto create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmployeeId(request.employeeId())) {
            throw new ConflictException("Employee ID already exists: " + request.employeeId());
        }
        validateUniqueIdentifiers(request.cnssNumber(), request.nationalId());

        Employee employee = new Employee();
        employee.setEmployeeId(request.employeeId());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setFullName(fullName(request.firstName(), request.lastName()));
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setGender(request.gender());
        employee.setHireDate(request.hireDate());
        employee.setDepartment(resolveDepartment(request.departmentId()));
        employee.setPosition(resolvePosition(request.positionId()));
        employee.setCategory(resolveCategory(request.categoryId()));
        employee.setEchelon(request.echelon());
        employee.setBaseSalary(request.baseSalary());
        employee.setFamilyStatus(request.familyStatus());
        employee.setNumberOfChildren(request.numberOfChildren());
        employee.setNationalId(request.nationalId());
        employee.setCnssNumber(request.cnssNumber());
        employee.setPhoneNumber(request.phoneNumber());
        employee.setEmail(request.email());
        employee.setAddress(request.address());
        employee.setCity(request.city());
        employee.setZipCode(request.zipCode());
        employee.setPaymentMethod(request.paymentMethod());
        employee.setBankAccountNumber(request.bankAccountNumber());
        employee.setBankCode(request.bankCode());
        employee.setEmploymentStatus("ACTIVE");

        Employee saved = employeeRepository.save(employee);
        auditService.log("EMPLOYEE", saved.getId(), "CREATE", "Created employee " + saved.getEmployeeId());
        return mapper.toDto(saved);
    }

    @Transactional
    public EmployeeDto update(Long id, EmployeeUpdateRequest request) {
        Employee employee = find(id);

        // Uniqueness checks exclude the employee's own current values.
        if (changed(employee.getCnssNumber(), request.cnssNumber())
                && request.cnssNumber() != null
                && employeeRepository.existsByCnssNumber(request.cnssNumber())) {
            throw new ConflictException("CNSS number already exists: " + request.cnssNumber());
        }
        if (changed(employee.getNationalId(), request.nationalId())
                && request.nationalId() != null
                && employeeRepository.existsByNationalId(request.nationalId())) {
            throw new ConflictException("National ID already exists: " + request.nationalId());
        }

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setFullName(fullName(request.firstName(), request.lastName()));
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setGender(request.gender());
        employee.setHireDate(request.hireDate());
        employee.setDepartment(resolveDepartment(request.departmentId()));
        employee.setPosition(resolvePosition(request.positionId()));
        employee.setCategory(resolveCategory(request.categoryId()));
        employee.setEchelon(request.echelon());
        employee.setBaseSalary(request.baseSalary());
        employee.setFamilyStatus(request.familyStatus());
        employee.setNumberOfChildren(request.numberOfChildren());
        employee.setNationalId(request.nationalId());
        employee.setCnssNumber(request.cnssNumber());
        employee.setPhoneNumber(request.phoneNumber());
        employee.setEmail(request.email());
        employee.setAddress(request.address());
        employee.setCity(request.city());
        employee.setZipCode(request.zipCode());
        employee.setPaymentMethod(request.paymentMethod());
        employee.setBankAccountNumber(request.bankAccountNumber());
        employee.setBankCode(request.bankCode());
        if (StringUtils.hasText(request.employmentStatus())) {
            employee.setEmploymentStatus(request.employmentStatus());
        }
        employee.setTerminationDate(request.terminationDate());
        employee.setTerminationReason(request.terminationReason());

        auditService.log("EMPLOYEE", employee.getId(), "UPDATE", "Updated employee " + employee.getEmployeeId());
        return mapper.toDto(employee);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = find(id);
        employeeRepository.delete(employee);
        auditService.log("EMPLOYEE", id, "DELETE", "Deleted employee " + employee.getEmployeeId());
    }

    // ---- helpers ----

    private void validateUniqueIdentifiers(String cnss, String nationalId) {
        if (cnss != null && employeeRepository.existsByCnssNumber(cnss)) {
            throw new ConflictException("CNSS number already exists: " + cnss);
        }
        if (nationalId != null && employeeRepository.existsByNationalId(nationalId)) {
            throw new ConflictException("National ID already exists: " + nationalId);
        }
    }

    private Department resolveDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
    }

    private Position resolvePosition(Long positionId) {
        if (positionId == null) {
            return null;
        }
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position", positionId));
    }

    private SalaryCategory resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary category", categoryId));
    }

    private Employee find(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private static String fullName(String first, String last) {
        return (first.trim() + " " + last.trim()).trim();
    }

    private static boolean changed(String current, String incoming) {
        return current == null ? incoming != null : !current.equals(incoming);
    }

    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}

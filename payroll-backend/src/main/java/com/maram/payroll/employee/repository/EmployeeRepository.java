package com.maram.payroll.employee.repository;

import com.maram.payroll.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeId(String employeeId);

    boolean existsByCnssNumber(String cnssNumber);

    boolean existsByNationalId(String nationalId);

    List<Employee> findByEmploymentStatus(String employmentStatus);

    long countByEmploymentStatus(String employmentStatus);

    long countByHireDateBetween(java.time.LocalDate start, java.time.LocalDate end);

    List<Employee> findTop5ByOrderByHireDateDesc();

    /** Headcount grouped by department name, busiest first. Returns [name, count] rows. */
    @Query("""
            SELECT e.department.name, COUNT(e)
            FROM Employee e
            GROUP BY e.department.name
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countByDepartment();

    /**
     * Paginated employee search with optional filters. A null filter is ignored,
     * so the same query backs "list all" and any combination of filters.
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE (:departmentId IS NULL OR e.department.id = :departmentId)
              AND (CAST(:status AS string) IS NULL OR e.employmentStatus = :status)
              AND (CAST(:search AS string) IS NULL
                   OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(e.employeeId) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Employee> search(@Param("departmentId") Long departmentId,
                          @Param("status") String status,
                          @Param("search") String search,
                          Pageable pageable);
}

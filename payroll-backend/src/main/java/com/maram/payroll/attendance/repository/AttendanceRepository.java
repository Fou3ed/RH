package com.maram.payroll.attendance.repository;

import com.maram.payroll.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDate(
            Long employeeId, LocalDate start, LocalDate end);

    /** All attendance in a date window, optionally restricted to a department. */
    @Query("""
            SELECT a FROM Attendance a
            WHERE a.attendanceDate BETWEEN :start AND :end
              AND (:departmentId IS NULL OR a.employee.department.id = :departmentId)
            """)
    List<Attendance> findInRange(@Param("start") LocalDate start,
                                 @Param("end") LocalDate end,
                                 @Param("departmentId") Long departmentId);
}

package com.maram.payroll.config.period;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PayrollPeriodDtos {

    private PayrollPeriodDtos() {
    }

    public record Response(
            Long id,
            String periodCode,
            Integer periodMonth,
            Integer periodYear,
            LocalDate startDate,
            LocalDate endDate,
            Integer workingDays,
            Integer holidaysInPeriod,
            String status,
            LocalDateTime createdAt) {

        public static Response from(PayrollPeriod p) {
            return new Response(p.getId(), p.getPeriodCode(), p.getPeriodMonth(), p.getPeriodYear(),
                    p.getStartDate(), p.getEndDate(), p.getWorkingDays(), p.getHolidaysInPeriod(),
                    p.getStatus().name(), p.getCreatedAt());
        }
    }

    public record CreateRequest(
            @NotNull @Min(1) @Max(12) Integer periodMonth,
            @NotNull Integer periodYear,
            LocalDate startDate,
            LocalDate endDate,
            Integer workingDays,
            Integer holidaysInPeriod) {
    }

    public record TransitionRequest(
            @NotNull String status) {
    }
}

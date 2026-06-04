package com.maram.payroll.payroll.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the payroll calculators (no Spring / DB). Each asserts the
 * formulas and the specific test cases documented in the Sprint 6 roadmap
 * (US-023…028), to 2-decimal money precision.
 */
class CalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // ---- US-023: base salary + attendance adjustment ----

    @Test
    void baseSalaryIsConfiguredBaseTimesMultiplier() {
        SalaryCalculator calc = new SalaryCalculator();
        // 560 × 4.042 = 2263.52
        assertThat(calc.calculateBaseSalary(bd("560"), bd("4.042"))).isEqualByComparingTo("2263.52");
    }

    @Test
    void attendanceAdjustmentProratesByDaysWorked() {
        SalaryCalculator calc = new SalaryCalculator();
        // full month = unchanged
        assertThat(calc.adjustForAttendance(bd("2600"), bd("26"))).isEqualByComparingTo("2600.00");
        // half month
        assertThat(calc.adjustForAttendance(bd("2600"), bd("13"))).isEqualByComparingTo("1300.00");
    }

    // ---- US-024: allowances ----

    @Test
    void presenceAllowanceIsAttendanceAdjusted() {
        AllowanceCalculator calc = new AllowanceCalculator();
        // 9.036 × (22.5 / 26) = 7.8196 → 7.82
        assertThat(calc.component(bd("9.036"), true, bd("22.5"))).isEqualByComparingTo("7.82");
    }

    @Test
    void transportAllowanceIsFixed() {
        AllowanceCalculator calc = new AllowanceCalculator();
        assertThat(calc.component(bd("87.165"), false, bd("13"))).isEqualByComparingTo("87.17");
    }

    @Test
    void childAllowanceIsPerChild() {
        AllowanceCalculator calc = new AllowanceCalculator();
        assertThat(calc.child(bd("5.000"), 3)).isEqualByComparingTo("15.00");
        assertThat(calc.child(bd("5.000"), 0)).isEqualByComparingTo("0.00");
        assertThat(calc.child(bd("5.000"), null)).isEqualByComparingTo("0.00");
    }

    // ---- US-025: performance bonus ----

    @Test
    void performanceBonusByRating() {
        PerformanceBonusCalculator calc = new PerformanceBonusCalculator();
        assertThat(calc.calculate(bd("2000"), 8)).isEqualByComparingTo("100.00"); // 5%
        assertThat(calc.calculate(bd("2000"), 7)).isEqualByComparingTo("50.00");  // 2.5%
        assertThat(calc.calculate(bd("2000"), 0)).isEqualByComparingTo("0.00");   // absent
        assertThat(calc.calculate(bd("2000"), null)).isEqualByComparingTo("0.00"); // missing
    }

    // ---- US-026: IRPP progressive tax ----

    @Test
    void irppProgressiveBrackets() {
        IRPPTaxCalculator calc = new IRPPTaxCalculator();
        List<IRPPTaxCalculator.Bracket> brackets = List.of(
                new IRPPTaxCalculator.Bracket(bd("0"), bd("2000"), bd("0")),
                new IRPPTaxCalculator.Bracket(bd("2000"), bd("5000"), bd("10")),
                new IRPPTaxCalculator.Bracket(bd("5000"), bd("10000"), bd("20")),
                new IRPPTaxCalculator.Bracket(bd("10000"), null, bd("30")));

        assertThat(calc.calculate(bd("1000"), brackets, BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        assertThat(calc.calculate(bd("3000"), brackets, BigDecimal.ZERO)).isEqualByComparingTo("100.00");
        assertThat(calc.calculate(bd("6000"), brackets, BigDecimal.ZERO)).isEqualByComparingTo("500.00");
        // 12000 = 300 + 1000 + 600 = 1900
        assertThat(calc.calculate(bd("12000"), brackets, BigDecimal.ZERO)).isEqualByComparingTo("1900.00");
    }

    @Test
    void irppTaxCreditIsSubtractedAndFlooredAtZero() {
        IRPPTaxCalculator calc = new IRPPTaxCalculator();
        List<IRPPTaxCalculator.Bracket> brackets = List.of(
                new IRPPTaxCalculator.Bracket(bd("0"), bd("2000"), bd("0")),
                new IRPPTaxCalculator.Bracket(bd("2000"), bd("5000"), bd("10")));
        // tax on 3000 = 100, credit 30 → 70
        assertThat(calc.calculate(bd("3000"), brackets, bd("30"))).isEqualByComparingTo("70.00");
        // credit larger than tax → floored at 0
        assertThat(calc.calculate(bd("3000"), brackets, bd("500"))).isEqualByComparingTo("0.00");
    }

    // ---- US-027: CNSS ----

    @Test
    void cnssIsRateOfGross() {
        CNSSCalculator calc = new CNSSCalculator();
        assertThat(calc.calculate(bd("1000"), bd("5.95"))).isEqualByComparingTo("59.50");
        assertThat(calc.calculate(bd("2000"), bd("5.95"))).isEqualByComparingTo("119.00");
    }

    // ---- US-028: absence penalty ----

    @Test
    void absencePenaltyIsDailyRateTimesDaysAbsent() {
        AbsencePenaltyCalculator calc = new AbsencePenaltyCalculator();
        // 560 / 26 = 21.538… → 1 day = 21.54
        assertThat(calc.calculate(bd("560"), bd("1"))).isEqualByComparingTo("21.54");
        // 3.5 days = 75.39
        assertThat(calc.calculate(bd("560"), bd("3.5"))).isEqualByComparingTo("75.39");
    }
}

export interface PayrollRow {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  daysWorked: number;
  baseSalary: number;
  adjustedSalary: number;
  presenceAllowance: number;
  transportAllowance: number;
  diligenceAllowance: number;
  mealAllowance: number;
  childAllowance: number;
  performanceBonus: number;
  totalAllowances: number;
  grossSalary: number;
  incomeTaxIrpp: number;
  cnssContribution: number;
  healthInsurance: number;
  totalDeductions: number;
  netSalary: number;
  paymentStatus: string;
}

export interface PayrollRunSummary {
  periodId: number;
  periodCode: string;
  calculated: number;
  skippedCount: number;
  totalGross: number;
  totalDeductions: number;
  totalNet: number;
  skipped: string[];
}

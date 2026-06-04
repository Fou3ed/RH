export interface SalaryScale {
  id: number;
  categoryId: number;
  categoryCode: string;
  echelon: number;
  year: number;
  salaryMultiplier: number;
  annualSalary: number | null;
  increaseAmount: number | null;
  increasePercent: number | null;
  validFrom: string | null;
  validTo: string | null;
}

export interface TaxConfig {
  id: number;
  taxYear: number;
  taxType: string;
  minTaxableIncome: number | null;
  maxTaxableIncome: number | null;
  taxRate: number | null;
  taxCreditAmount: number | null;
  familyStatusCode: string | null;
  numberOfChildren: number | null;
  effectiveDate: string | null;
  endDate: string | null;
}

export interface AllowanceConfig {
  id: number;
  allowanceType: string;
  amount: number;
  attendanceAdjusted: boolean;
  effectiveDate: string | null;
  endDate: string | null;
}

export interface PayrollPeriod {
  id: number;
  periodCode: string;
  periodMonth: number;
  periodYear: number;
  startDate: string;
  endDate: string;
  workingDays: number | null;
  holidaysInPeriod: number | null;
  status: PeriodStatus;
  createdAt: string;
}

export type PeriodStatus = 'DRAFT' | 'LOCKED' | 'PROCESSING' | 'FINALIZED' | 'PAID';

/** The single next status allowed from each lifecycle state (forward path). */
export const NEXT_STATUS: Record<PeriodStatus, PeriodStatus | null> = {
  DRAFT: 'LOCKED',
  LOCKED: 'PROCESSING',
  PROCESSING: 'FINALIZED',
  FINALIZED: 'PAID',
  PAID: null,
};

export const PERIOD_STATUS_COLOR: Record<PeriodStatus, 'default' | 'info' | 'warning' | 'success'> = {
  DRAFT: 'default',
  LOCKED: 'info',
  PROCESSING: 'warning',
  FINALIZED: 'success',
  PAID: 'success',
};

export const ALLOWANCE_TYPES = ['PRESENCE', 'TRANSPORT', 'DILIGENCE', 'MEAL', 'CHILD'] as const;
export const TAX_TYPES = ['IRPP', 'CNSS', 'HEALTH'] as const;

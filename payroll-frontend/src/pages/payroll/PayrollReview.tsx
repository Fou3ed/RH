import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useTranslation, Trans } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import CalculateIcon from '@mui/icons-material/Calculate';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DownloadIcon from '@mui/icons-material/Download';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { payrollPeriodService } from '@/services/config.service';
import { payrollService } from '@/services/payroll.service';
import { reportService } from '@/services/report.service';
import { useAuth } from '@/context/AuthContext';
import PageHeader from '@/components/common/PageHeader';
import StatusChip from '@/components/common/StatusChip';
import type { PayrollRunSummary } from '@/types/payroll';
import type { ApiError } from '@/types/api';

const money = (n: number) => n?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const num = { fontVariantNumeric: 'tabular-nums' } as const;

function TotalCard({ label, value, color = '#3b5bdb' }: { label: string; value: string; color?: string }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ py: 2.5 }}>
        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          {label}
        </Typography>
        <Typography variant="h4" sx={{ fontWeight: 800, mt: 0.5, color, ...num }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

export default function PayrollReview() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const canCalculate = hasPermission('payroll.calculate');
  const canApprove = hasPermission('payroll.approve');
  const canExportPayroll = hasPermission('payroll.export');
  const canExportReports = hasPermission('report.export');

  const [periodId, setPeriodId] = useState<number | ''>('');
  const [summary, setSummary] = useState<PayrollRunSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: periods } = useQuery({ queryKey: ['payroll-periods'], queryFn: payrollPeriodService.list });

  const { data: rows, isFetching } = useQuery({
    queryKey: ['payroll', periodId],
    queryFn: () => payrollService.list(Number(periodId)),
    enabled: periodId !== '',
  });

  const calculate = useMutation({
    mutationFn: () => payrollService.calculate(Number(periodId)),
    onSuccess: (s) => {
      setSummary(s);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['payroll', periodId] });
    },
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? t('payroll.calculationFailed')),
  });

  const approve = useMutation({
    mutationFn: (id: number) => payrollService.approve(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['payroll', periodId] }),
  });

  const totals = useMemo(() => {
    if (!rows) return null;
    return rows.reduce(
      (acc, r) => ({
        gross: acc.gross + r.grossSalary,
        deductions: acc.deductions + r.totalDeductions,
        net: acc.net + r.netSalary,
      }),
      { gross: 0, deductions: 0, net: 0 },
    );
  }, [rows]);

  return (
    <Stack spacing={3}>
      <PageHeader title={t('payroll.title')} subtitle={t('payroll.subtitle')} />

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <TextField
            select
            label={t('payroll.period')}
            value={periodId}
            onChange={(e) => {
              setPeriodId(e.target.value === '' ? '' : Number(e.target.value));
              setSummary(null);
            }}
            size="small"
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">{t('payroll.selectPeriod')}</MenuItem>
            {periods?.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                {p.periodCode} ({t(`status.${p.status}`, { defaultValue: p.status })})
              </MenuItem>
            ))}
          </TextField>
          <Box sx={{ flexGrow: 1 }} />
          {canExportPayroll && periodId !== '' && (rows?.length ?? 0) > 0 && (
            <Button variant="outlined" startIcon={<DownloadIcon />}
              onClick={() => reportService.payrollExcel(Number(periodId))}>
              {t('payroll.excel')}
            </Button>
          )}
          {canExportReports && periodId !== '' && (rows?.length ?? 0) > 0 && (
            <>
              <Button variant="outlined" onClick={() => reportService.irppCsv(Number(periodId))}>
                {t('payroll.irppCsv')}
              </Button>
              <Button variant="outlined" onClick={() => reportService.cnssCsv(Number(periodId))}>
                {t('payroll.cnssCsv')}
              </Button>
            </>
          )}
          {canCalculate && (
            <Button
              variant="contained"
              startIcon={<CalculateIcon />}
              disabled={periodId === '' || calculate.isPending}
              onClick={() => calculate.mutate()}
            >
              {calculate.isPending ? t('payroll.calculating') : t('payroll.calculate')}
            </Button>
          )}
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      {summary && (
        <Alert severity={summary.skippedCount ? 'warning' : 'success'}>
          <Trans
            i18nKey="payroll.calculatedAlert"
            values={{ count: summary.calculated, period: summary.periodCode, net: money(summary.totalNet) }}
            components={{ 1: <strong />, 3: <strong /> }}
          />
          {summary.skippedCount > 0 &&
            t('payroll.skipped', { count: summary.skippedCount, list: summary.skipped.join('; ') })}
        </Alert>
      )}

      {totals && rows && rows.length > 0 && (
        <Grid container spacing={2.5}>
          <Grid item xs={12} sm={4}>
            <TotalCard label={t('payroll.totalGross')} value={`${money(totals.gross)} TND`} color="#0f172a" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TotalCard label={t('payroll.totalDeductions')} value={`${money(totals.deductions)} TND`} color="#d97706" />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TotalCard label={t('payroll.totalNet')} value={`${money(totals.net)} TND`} color="#16a34a" />
          </Grid>
        </Grid>
      )}

      {periodId !== '' && (
        <Paper>
          <TableContainer>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>{t('payroll.col.id')}</TableCell>
                  <TableCell>{t('payroll.col.employee')}</TableCell>
                  <TableCell align="right">{t('payroll.col.days')}</TableCell>
                  <TableCell align="right">{t('payroll.col.gross')}</TableCell>
                  <TableCell align="right">{t('payroll.col.allowances')}</TableCell>
                  <TableCell align="right">{t('payroll.col.irpp')}</TableCell>
                  <TableCell align="right">{t('payroll.col.cnss')}</TableCell>
                  <TableCell align="right">{t('payroll.col.net')}</TableCell>
                  <TableCell>{t('payroll.col.status')}</TableCell>
                  <TableCell align="right">{t('payroll.col.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {isFetching && (
                  <TableRow>
                    <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                      <CircularProgress size={24} />
                    </TableCell>
                  </TableRow>
                )}
                {!isFetching && rows?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={10} sx={{ py: 8 }}>
                      <Stack alignItems="center" spacing={1.5}>
                        <ReceiptLongIcon sx={{ fontSize: 48, color: 'grey.300' }} />
                        <Typography fontWeight={600}>{t('payroll.noPayrollYet')}</Typography>
                        <Typography color="text.secondary" variant="body2">
                          {t('payroll.clickCalculate')}
                        </Typography>
                      </Stack>
                    </TableCell>
                  </TableRow>
                )}
                {rows?.map((r) => (
                  <TableRow key={r.id} hover>
                    <TableCell sx={{ fontWeight: 600, ...num }}>{r.employeeCode}</TableCell>
                    <TableCell sx={{ fontWeight: 500 }}>{r.employeeName}</TableCell>
                    <TableCell align="right" sx={num}>{r.daysWorked}</TableCell>
                    <TableCell align="right" sx={num}>{money(r.grossSalary)}</TableCell>
                    <TableCell align="right" sx={num}>{money(r.totalAllowances)}</TableCell>
                    <TableCell align="right" sx={num}>{money(r.incomeTaxIrpp)}</TableCell>
                    <TableCell align="right" sx={num}>{money(r.cnssContribution)}</TableCell>
                    <TableCell align="right" sx={{ ...num, fontWeight: 700 }}>
                      {money(r.netSalary)}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={r.paymentStatus} />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title={t('payroll.downloadPayslip')}>
                        <IconButton size="small" onClick={() => reportService.payslip(r.id)}>
                          <PictureAsPdfIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {canApprove && r.paymentStatus === 'DRAFT' && (
                        <Button
                          size="small"
                          startIcon={<CheckCircleIcon />}
                          onClick={() => approve.mutate(r.id)}
                          disabled={approve.isPending}
                        >
                          {t('payroll.approve')}
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </Stack>
  );
}

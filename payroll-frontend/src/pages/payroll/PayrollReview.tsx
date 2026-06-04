import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
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
import { payrollPeriodService } from '@/services/config.service';
import { payrollService } from '@/services/payroll.service';
import { reportService } from '@/services/report.service';
import { useAuth } from '@/context/AuthContext';
import type { PayrollRunSummary } from '@/types/payroll';
import type { ApiError } from '@/types/api';

const money = (n: number) => n?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function PayrollReview() {
  const { hasPermission } = useAuth();
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
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? 'Calculation failed.'),
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
      <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
        Payroll
      </Typography>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <TextField
            select
            label="Period"
            value={periodId}
            onChange={(e) => {
              setPeriodId(e.target.value === '' ? '' : Number(e.target.value));
              setSummary(null);
            }}
            size="small"
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">Select a period…</MenuItem>
            {periods?.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                {p.periodCode} ({p.status})
              </MenuItem>
            ))}
          </TextField>
          <Box sx={{ flexGrow: 1 }} />
          {canExportPayroll && periodId !== '' && (rows?.length ?? 0) > 0 && (
            <Button variant="outlined" startIcon={<DownloadIcon />}
              onClick={() => reportService.payrollExcel(Number(periodId))}>
              Excel
            </Button>
          )}
          {canExportReports && periodId !== '' && (rows?.length ?? 0) > 0 && (
            <>
              <Button variant="outlined" onClick={() => reportService.irppCsv(Number(periodId))}>
                IRPP CSV
              </Button>
              <Button variant="outlined" onClick={() => reportService.cnssCsv(Number(periodId))}>
                CNSS CSV
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
              {calculate.isPending ? 'Calculating…' : 'Calculate payroll'}
            </Button>
          )}
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      {summary && (
        <Alert severity={summary.skippedCount ? 'warning' : 'success'}>
          Calculated <strong>{summary.calculated}</strong> payroll(s) for {summary.periodCode}. Net total{' '}
          <strong>{money(summary.totalNet)} TND</strong>.
          {summary.skippedCount > 0 && (
            <> {summary.skippedCount} skipped: {summary.skipped.join('; ')}</>
          )}
        </Alert>
      )}

      {periodId !== '' && (
        <Paper>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Employee</TableCell>
                  <TableCell align="right">Days</TableCell>
                  <TableCell align="right">Gross</TableCell>
                  <TableCell align="right">Allowances</TableCell>
                  <TableCell align="right">IRPP</TableCell>
                  <TableCell align="right">CNSS</TableCell>
                  <TableCell align="right">Net</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
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
                    <TableCell colSpan={10} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                      No payroll yet — click “Calculate payroll”.
                    </TableCell>
                  </TableRow>
                )}
                {rows?.map((r) => (
                  <TableRow key={r.id} hover>
                    <TableCell>{r.employeeCode}</TableCell>
                    <TableCell>{r.employeeName}</TableCell>
                    <TableCell align="right">{r.daysWorked}</TableCell>
                    <TableCell align="right">{money(r.grossSalary)}</TableCell>
                    <TableCell align="right">{money(r.totalAllowances)}</TableCell>
                    <TableCell align="right">{money(r.incomeTaxIrpp)}</TableCell>
                    <TableCell align="right">{money(r.cnssContribution)}</TableCell>
                    <TableCell align="right">
                      <strong>{money(r.netSalary)}</strong>
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        color={r.paymentStatus === 'APPROVED' ? 'success' : 'default'}
                        label={r.paymentStatus}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Download payslip">
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
                          Approve
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {totals && rows && rows.length > 0 && (
            <Box sx={{ p: 2, display: 'flex', gap: 3, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
              <Typography>Gross: <strong>{money(totals.gross)}</strong></Typography>
              <Typography>Deductions: <strong>{money(totals.deductions)}</strong></Typography>
              <Typography>Net: <strong>{money(totals.net)} TND</strong></Typography>
            </Box>
          )}
        </Paper>
      )}
    </Stack>
  );
}

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { payrollPeriodService } from '@/services/config.service';
import { NEXT_STATUS, PERIOD_STATUS_COLOR } from '@/types/config';
import { useAuth } from '@/context/AuthContext';
import type { ApiError } from '@/types/api';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export default function PayrollPeriods() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const canManage = hasPermission('config.manage') || hasPermission('payroll.calculate');

  const now = new Date();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [year, setYear] = useState(now.getFullYear());

  const { data: periods } = useQuery({ queryKey: ['payroll-periods'], queryFn: payrollPeriodService.list });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['payroll-periods'] });

  const create = useMutation({
    mutationFn: () => payrollPeriodService.create({ periodMonth: month, periodYear: year }),
    onSuccess: () => {
      invalidate();
      setOpen(false);
    },
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? 'Create failed.'),
  });

  const transition = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      payrollPeriodService.transition(id, status),
    onSuccess: invalidate,
  });

  const remove = useMutation({ mutationFn: (id: number) => payrollPeriodService.remove(id), onSuccess: invalidate });

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
          Payroll periods
        </Typography>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setError(null); setOpen(true); }}>
            New period
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Period</TableCell>
                <TableCell>Dates</TableCell>
                <TableCell>Working days</TableCell>
                <TableCell>Status</TableCell>
                {canManage && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {periods?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No payroll periods yet.
                  </TableCell>
                </TableRow>
              )}
              {periods?.map((p) => {
                const next = NEXT_STATUS[p.status];
                return (
                  <TableRow key={p.id} hover>
                    <TableCell>{p.periodCode}</TableCell>
                    <TableCell>{p.startDate} → {p.endDate}</TableCell>
                    <TableCell>{p.workingDays}</TableCell>
                    <TableCell>
                      <Chip size="small" color={PERIOD_STATUS_COLOR[p.status]} label={p.status} />
                    </TableCell>
                    {canManage && (
                      <TableCell align="right">
                        {next && (
                          <Tooltip title={`Advance to ${next}`}>
                            <IconButton size="small" color="primary"
                              onClick={() => transition.mutate({ id: p.id, status: next })}>
                              <ArrowForwardIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {p.status === 'DRAFT' && (
                          <Tooltip title="Delete">
                            <IconButton size="small" color="error"
                              onClick={() => { if (window.confirm(`Delete ${p.periodCode}?`)) remove.mutate(p.id); }}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>New payroll period</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField select label="Month" value={month} onChange={(e) => setMonth(Number(e.target.value))} fullWidth>
              {MONTHS.map((m, i) => (
                <MenuItem key={m} value={i + 1}>{m}</MenuItem>
              ))}
            </TextField>
            <TextField label="Year" type="number" value={year} onChange={(e) => setYear(Number(e.target.value))} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => create.mutate()} disabled={create.isPending}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

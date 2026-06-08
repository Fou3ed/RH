import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Button,
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
import { NEXT_STATUS } from '@/types/config';
import { useAuth } from '@/context/AuthContext';
import { useConfirm } from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import type { ApiError } from '@/types/api';

export default function PayrollPeriods() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const confirm = useConfirm();
  const canManage = hasPermission('config.manage') || hasPermission('payroll.calculate');

  const now = new Date();
  const months = t('common.months', { returnObjects: true }) as string[];
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
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? t('periods.createFailed')),
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
          {t('periods.title')}
        </Typography>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { setError(null); setOpen(true); }}>
            {t('periods.newPeriod')}
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('periods.period')}</TableCell>
                <TableCell>{t('periods.dates')}</TableCell>
                <TableCell>{t('periods.workingDays')}</TableCell>
                <TableCell>{t('common.status')}</TableCell>
                {canManage && <TableCell align="right">{t('common.actions')}</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {periods?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    {t('periods.none')}
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
                      <StatusChip status={p.status} />
                    </TableCell>
                    {canManage && (
                      <TableCell align="right">
                        {next && (
                          <Tooltip title={t('periods.advanceTo', { status: t(`status.${next}`, { defaultValue: next }) })}>
                            <IconButton size="small" color="primary"
                              onClick={() => transition.mutate({ id: p.id, status: next })}>
                              <ArrowForwardIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {p.status === 'DRAFT' && (
                          <Tooltip title={t('common.delete')}>
                            <IconButton size="small" color="error"
                              onClick={async () => {
                                if (
                                  await confirm({
                                    title: t('periods.deleteTitle'),
                                    message: t('periods.deleteConfirm', { code: p.periodCode }),
                                    destructive: true,
                                  })
                                )
                                  remove.mutate(p.id);
                              }}>
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
        <DialogTitle>{t('periods.newPeriod')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField select label={t('periods.month')} value={month} onChange={(e) => setMonth(Number(e.target.value))} fullWidth>
              {months.map((m, i) => (
                <MenuItem key={m} value={i + 1}>{m}</MenuItem>
              ))}
            </TextField>
            <TextField label={t('periods.year')} type="number" value={year} onChange={(e) => setYear(Number(e.target.value))} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="contained" onClick={() => create.mutate()} disabled={create.isPending}>
            {t('common.create')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

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
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { taxConfigService } from '@/services/config.service';
import { TAX_TYPES } from '@/types/config';
import { useAuth } from '@/context/AuthContext';
import type { ApiError } from '@/types/api';

export default function TaxConfigTab() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const canManage = hasPermission('config.manage');
  const queryClient = useQueryClient();

  const [year, setYear] = useState(new Date().getFullYear());
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    taxYear: year, taxType: 'IRPP', minTaxableIncome: '', maxTaxableIncome: '', taxRate: '',
  });

  const { data: rows } = useQuery({ queryKey: ['tax-config', year], queryFn: () => taxConfigService.list(year) });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['tax-config'] });

  const save = useMutation({
    mutationFn: () =>
      taxConfigService.create({
        taxYear: Number(form.taxYear),
        taxType: form.taxType,
        minTaxableIncome: form.minTaxableIncome === '' ? null : Number(form.minTaxableIncome),
        maxTaxableIncome: form.maxTaxableIncome === '' ? null : Number(form.maxTaxableIncome),
        taxRate: form.taxRate === '' ? null : Number(form.taxRate),
      }),
    onSuccess: () => {
      invalidate();
      setOpen(false);
    },
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? t('taxConfig.saveFailed')),
  });

  const remove = useMutation({ mutationFn: (id: number) => taxConfigService.remove(id), onSuccess: invalidate });

  const openCreate = () => {
    setForm({ taxYear: year, taxType: 'IRPP', minTaxableIncome: '', maxTaxableIncome: '', taxRate: '' });
    setError(null);
    setOpen(true);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField label={t('common.year')} type="number" size="small" value={year}
          onChange={(e) => setYear(Number(e.target.value))} sx={{ width: 120 }} />
        <span style={{ flexGrow: 1 }} />
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            {t('taxConfig.addRule')}
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('taxConfig.type')}</TableCell>
                <TableCell>{t('taxConfig.minIncome')}</TableCell>
                <TableCell>{t('taxConfig.maxIncome')}</TableCell>
                <TableCell>{t('taxConfig.ratePct')}</TableCell>
                {canManage && <TableCell align="right">{t('common.actions')}</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    {t('taxConfig.none', { year })}
                  </TableCell>
                </TableRow>
              )}
              {rows?.map((row) => (
                <TableRow key={row.id} hover>
                  <TableCell>{row.taxType}</TableCell>
                  <TableCell>{row.minTaxableIncome ?? '—'}</TableCell>
                  <TableCell>{row.maxTaxableIncome ?? '—'}</TableCell>
                  <TableCell>{row.taxRate ?? '—'}</TableCell>
                  {canManage && (
                    <TableCell align="right">
                      <Tooltip title={t('common.delete')}>
                        <IconButton size="small" color="error" onClick={() => remove.mutate(row.id)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>{t('taxConfig.addTitle')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField select label={t('taxConfig.type')} value={form.taxType}
              onChange={(e) => setForm({ ...form, taxType: e.target.value })} fullWidth>
              {TAX_TYPES.map((type) => (
                <MenuItem key={type} value={type}>{type}</MenuItem>
              ))}
            </TextField>
            <TextField label={t('common.year')} type="number" value={form.taxYear}
              onChange={(e) => setForm({ ...form, taxYear: Number(e.target.value) })} fullWidth />
            <TextField label={t('taxConfig.minTaxableIncome')} type="number" value={form.minTaxableIncome}
              onChange={(e) => setForm({ ...form, minTaxableIncome: e.target.value })} fullWidth
              helperText={t('taxConfig.irppOnly')} />
            <TextField label={t('taxConfig.maxTaxableIncome')} type="number" value={form.maxTaxableIncome}
              onChange={(e) => setForm({ ...form, maxTaxableIncome: e.target.value })} fullWidth />
            <TextField label={t('taxConfig.ratePct')} type="number" value={form.taxRate}
              onChange={(e) => setForm({ ...form, taxRate: e.target.value })} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="contained" onClick={() => save.mutate()} disabled={save.isPending}>
            {t('common.save')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

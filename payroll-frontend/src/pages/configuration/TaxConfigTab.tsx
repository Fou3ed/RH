import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
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
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? 'Save failed.'),
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
        <TextField label="Year" type="number" size="small" value={year}
          onChange={(e) => setYear(Number(e.target.value))} sx={{ width: 120 }} />
        <span style={{ flexGrow: 1 }} />
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add rule
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Type</TableCell>
                <TableCell>Min income</TableCell>
                <TableCell>Max income</TableCell>
                <TableCell>Rate %</TableCell>
                {canManage && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    No tax configuration for {year}.
                  </TableCell>
                </TableRow>
              )}
              {rows?.map((t) => (
                <TableRow key={t.id} hover>
                  <TableCell>{t.taxType}</TableCell>
                  <TableCell>{t.minTaxableIncome ?? '—'}</TableCell>
                  <TableCell>{t.maxTaxableIncome ?? '—'}</TableCell>
                  <TableCell>{t.taxRate ?? '—'}</TableCell>
                  {canManage && (
                    <TableCell align="right">
                      <Tooltip title="Delete">
                        <IconButton size="small" color="error" onClick={() => remove.mutate(t.id)}>
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
        <DialogTitle>Add tax rule</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField select label="Type" value={form.taxType}
              onChange={(e) => setForm({ ...form, taxType: e.target.value })} fullWidth>
              {TAX_TYPES.map((t) => (
                <MenuItem key={t} value={t}>{t}</MenuItem>
              ))}
            </TextField>
            <TextField label="Year" type="number" value={form.taxYear}
              onChange={(e) => setForm({ ...form, taxYear: Number(e.target.value) })} fullWidth />
            <TextField label="Min taxable income" type="number" value={form.minTaxableIncome}
              onChange={(e) => setForm({ ...form, minTaxableIncome: e.target.value })} fullWidth
              helperText="IRPP brackets only" />
            <TextField label="Max taxable income" type="number" value={form.maxTaxableIncome}
              onChange={(e) => setForm({ ...form, maxTaxableIncome: e.target.value })} fullWidth />
            <TextField label="Rate %" type="number" value={form.taxRate}
              onChange={(e) => setForm({ ...form, taxRate: e.target.value })} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate()} disabled={save.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

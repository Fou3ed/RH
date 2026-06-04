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
import { salaryScaleService } from '@/services/config.service';
import { referenceService } from '@/services/reference.service';
import { useAuth } from '@/context/AuthContext';
import type { ApiError } from '@/types/api';

export default function SalaryScalesTab() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('config.manage');
  const queryClient = useQueryClient();

  const [year, setYear] = useState(new Date().getFullYear());
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ categoryId: '', echelon: '', year, salaryMultiplier: '' });

  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: referenceService.categories });
  const { data: scales } = useQuery({
    queryKey: ['salary-scales', year],
    queryFn: () => salaryScaleService.list(year),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['salary-scales'] });

  const save = useMutation({
    mutationFn: () =>
      salaryScaleService.create({
        categoryId: Number(form.categoryId),
        echelon: Number(form.echelon),
        year: Number(form.year),
        salaryMultiplier: Number(form.salaryMultiplier),
      }),
    onSuccess: () => {
      invalidate();
      setOpen(false);
    },
    onError: (err) =>
      setError((err as AxiosError<ApiError>).response?.data?.message ?? 'Save failed.'),
  });

  const remove = useMutation({
    mutationFn: (id: number) => salaryScaleService.remove(id),
    onSuccess: invalidate,
  });

  const openCreate = () => {
    setForm({ categoryId: '', echelon: '', year, salaryMultiplier: '' });
    setError(null);
    setOpen(true);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={2} alignItems="center">
        <TextField
          label="Year"
          type="number"
          size="small"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
          sx={{ width: 120 }}
        />
        <span style={{ flexGrow: 1 }} />
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add scale
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Category</TableCell>
                <TableCell>Échelon</TableCell>
                <TableCell>Multiplier</TableCell>
                <TableCell>Valid from</TableCell>
                {canManage && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {scales?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    No salary scales for {year}.
                  </TableCell>
                </TableRow>
              )}
              {scales?.map((s) => (
                <TableRow key={s.id} hover>
                  <TableCell>{s.categoryCode}</TableCell>
                  <TableCell>{s.echelon}</TableCell>
                  <TableCell>{s.salaryMultiplier}</TableCell>
                  <TableCell>{s.validFrom ?? '—'}</TableCell>
                  {canManage && (
                    <TableCell align="right">
                      <Tooltip title="Delete">
                        <IconButton size="small" color="error" onClick={() => remove.mutate(s.id)}>
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
        <DialogTitle>Add salary scale</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField select label="Category" value={form.categoryId}
              onChange={(e) => setForm({ ...form, categoryId: e.target.value })} fullWidth>
              {categories?.map((c) => (
                <MenuItem key={c.id} value={c.id}>{c.code}</MenuItem>
              ))}
            </TextField>
            <TextField label="Échelon" type="number" value={form.echelon}
              onChange={(e) => setForm({ ...form, echelon: e.target.value })}
              inputProps={{ min: 1, max: 14 }} fullWidth />
            <TextField label="Year" type="number" value={form.year}
              onChange={(e) => setForm({ ...form, year: Number(e.target.value) })} fullWidth />
            <TextField label="Multiplier" type="number" value={form.salaryMultiplier}
              onChange={(e) => setForm({ ...form, salaryMultiplier: e.target.value })}
              inputProps={{ step: 0.001 }} fullWidth />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate()}
            disabled={save.isPending || !form.categoryId || !form.echelon || !form.salaryMultiplier}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

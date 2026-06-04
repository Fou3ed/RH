import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import {
  Alert,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
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
import { allowanceConfigService } from '@/services/config.service';
import { ALLOWANCE_TYPES } from '@/types/config';
import { useAuth } from '@/context/AuthContext';
import type { ApiError } from '@/types/api';

export default function AllowancesTab() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('config.manage');
  const queryClient = useQueryClient();

  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({ allowanceType: 'PRESENCE', amount: '', attendanceAdjusted: false });

  const { data: rows } = useQuery({ queryKey: ['allowance-config'], queryFn: allowanceConfigService.list });
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['allowance-config'] });

  const save = useMutation({
    mutationFn: () =>
      allowanceConfigService.create({
        allowanceType: form.allowanceType,
        amount: Number(form.amount),
        attendanceAdjusted: form.attendanceAdjusted,
      }),
    onSuccess: () => {
      invalidate();
      setOpen(false);
    },
    onError: (err) => setError((err as AxiosError<ApiError>).response?.data?.message ?? 'Save failed.'),
  });

  const remove = useMutation({ mutationFn: (id: number) => allowanceConfigService.remove(id), onSuccess: invalidate });

  const openCreate = () => {
    setForm({ allowanceType: 'PRESENCE', amount: '', attendanceAdjusted: false });
    setError(null);
    setOpen(true);
  };

  return (
    <Stack spacing={2}>
      <Stack direction="row" alignItems="center">
        <span style={{ flexGrow: 1 }} />
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Add allowance
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Type</TableCell>
                <TableCell>Amount (TND)</TableCell>
                <TableCell>Attendance-adjusted</TableCell>
                <TableCell>Effective</TableCell>
                {canManage && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows?.map((a) => (
                <TableRow key={a.id} hover>
                  <TableCell>{a.allowanceType}</TableCell>
                  <TableCell>{a.amount}</TableCell>
                  <TableCell>{a.attendanceAdjusted ? 'Yes' : 'No'}</TableCell>
                  <TableCell>{a.effectiveDate ?? '—'}</TableCell>
                  {canManage && (
                    <TableCell align="right">
                      <Tooltip title="Delete">
                        <IconButton size="small" color="error" onClick={() => remove.mutate(a.id)}>
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
        <DialogTitle>Add allowance default</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField select label="Type" value={form.allowanceType}
              onChange={(e) => setForm({ ...form, allowanceType: e.target.value })} fullWidth>
              {ALLOWANCE_TYPES.map((t) => (
                <MenuItem key={t} value={t}>{t}</MenuItem>
              ))}
            </TextField>
            <TextField label="Amount (TND)" type="number" value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
              inputProps={{ step: 0.001 }} fullWidth />
            <FormControlLabel
              control={
                <Checkbox checked={form.attendanceAdjusted}
                  onChange={(e) => setForm({ ...form, attendanceAdjusted: e.target.checked })} />
              }
              label="Scale by days worked (÷26)"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate()} disabled={save.isPending || !form.amount}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

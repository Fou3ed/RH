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
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { departmentService } from '@/services/employee.service';
import { useAuth } from '@/context/AuthContext';
import type { Department, DepartmentRequest } from '@/types/employee';
import type { ApiError } from '@/types/api';

const EMPTY: DepartmentRequest = { code: '', name: '', description: '' };

export default function Departments() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const canManage = hasPermission('config.manage');

  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<DepartmentRequest>(EMPTY);
  const [error, setError] = useState<string | null>(null);

  const { data: departments, isLoading } = useQuery({
    queryKey: ['departments'],
    queryFn: departmentService.list,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['departments'] });

  const save = useMutation({
    mutationFn: (values: DepartmentRequest) =>
      editingId ? departmentService.update(editingId, values) : departmentService.create(values),
    onSuccess: () => {
      invalidate();
      setOpen(false);
    },
    onError: (err) => {
      const apiError = err as AxiosError<ApiError>;
      setError(apiError.response?.data?.message ?? 'Save failed.');
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => departmentService.remove(id),
    onSuccess: invalidate,
  });

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY);
    setError(null);
    setOpen(true);
  };

  const openEdit = (dept: Department) => {
    setEditingId(dept.id);
    setForm({ code: dept.code, name: dept.name, description: dept.description ?? '' });
    setError(null);
    setOpen(true);
  };

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
          Departments
        </Typography>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            New department
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Parent</TableCell>
                {canManage && <TableCell align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && departments?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No departments yet.</Typography>
                  </TableCell>
                </TableRow>
              )}
              {departments?.map((dept) => (
                <TableRow key={dept.id} hover>
                  <TableCell>{dept.code}</TableCell>
                  <TableCell>{dept.name}</TableCell>
                  <TableCell>{dept.description ?? '—'}</TableCell>
                  <TableCell>{dept.parentName ?? '—'}</TableCell>
                  {canManage && (
                    <TableCell align="right">
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => openEdit(dept)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => {
                            if (window.confirm(`Delete ${dept.name}?`)) remove.mutate(dept.id);
                          }}
                        >
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

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? 'Edit department' : 'New department'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Code"
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Description"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              multiline
              minRows={2}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => save.mutate(form)}
            disabled={save.isPending || !form.code || !form.name}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

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
import { useConfirm } from '@/components/common/ConfirmDialog';
import type { Department, DepartmentRequest } from '@/types/employee';
import type { ApiError } from '@/types/api';

const EMPTY: DepartmentRequest = { code: '', name: '', description: '' };

export default function Departments() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const confirm = useConfirm();
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
      setError(apiError.response?.data?.message ?? t('departments.saveFailed'));
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
          {t('departments.title')}
        </Typography>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            {t('departments.newDepartment')}
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('departments.code')}</TableCell>
                <TableCell>{t('departments.name')}</TableCell>
                <TableCell>{t('departments.description')}</TableCell>
                <TableCell>{t('departments.parent')}</TableCell>
                {canManage && <TableCell align="right">{t('common.actions')}</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && departments?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">{t('departments.none')}</Typography>
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
                      <Tooltip title={t('common.edit')}>
                        <IconButton size="small" onClick={() => openEdit(dept)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title={t('common.delete')}>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={async () => {
                            if (
                              await confirm({
                                title: t('departments.deleteTitle'),
                                message: t('departments.deleteConfirm', { name: dept.name }),
                                destructive: true,
                              })
                            )
                              remove.mutate(dept.id);
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
        <DialogTitle>{editingId ? t('departments.editDepartment') : t('departments.newDepartment')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label={t('departments.code')}
              value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label={t('departments.name')}
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label={t('departments.description')}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              multiline
              minRows={2}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>{t('common.cancel')}</Button>
          <Button
            variant="contained"
            onClick={() => save.mutate(form)}
            disabled={save.isPending || !form.code || !form.name}
          >
            {t('common.save')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

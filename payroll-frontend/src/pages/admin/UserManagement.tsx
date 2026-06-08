import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormGroup,
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
import BadgeIcon from '@mui/icons-material/Badge';
import BlockIcon from '@mui/icons-material/Block';
import { userService } from '@/services/user.service';
import { useAuth } from '@/context/AuthContext';
import { useConfirm } from '@/components/common/ConfirmDialog';
import StatusChip from '@/components/common/StatusChip';
import { ROLE_CODES } from '@/types/user';
import type { CreateUserRequest, UserSummary } from '@/types/user';
import type { ApiError } from '@/types/api';

const EMPTY_CREATE: CreateUserRequest = {
  username: '',
  email: '',
  password: '',
  roles: [],
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function UserManagement() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const confirm = useConfirm();
  const canManage = hasPermission('user.manage');

  // Create dialog state
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<CreateUserRequest>(EMPTY_CREATE);
  const [createError, setCreateError] = useState<string | null>(null);

  // Edit-roles dialog state
  const [rolesOpen, setRolesOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserSummary | null>(null);
  const [editRoles, setEditRoles] = useState<string[]>([]);
  const [rolesError, setRolesError] = useState<string | null>(null);

  const { data: users, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: userService.list,
    enabled: canManage,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['users'] });

  const create = useMutation({
    mutationFn: (values: CreateUserRequest) => userService.create(values),
    onSuccess: () => {
      invalidate();
      setCreateOpen(false);
    },
    onError: (err) => {
      const apiError = err as AxiosError<ApiError>;
      setCreateError(apiError.response?.data?.message ?? t('users.saveFailed'));
    },
  });

  const saveRoles = useMutation({
    mutationFn: ({ id, roles }: { id: number; roles: string[] }) =>
      userService.updateRoles(id, { roles }),
    onSuccess: () => {
      invalidate();
      setRolesOpen(false);
    },
    onError: (err) => {
      const apiError = err as AxiosError<ApiError>;
      setRolesError(apiError.response?.data?.message ?? t('users.saveFailed'));
    },
  });

  const deactivate = useMutation({
    mutationFn: (id: number) => userService.deactivate(id),
    onSuccess: invalidate,
  });

  const openCreate = () => {
    setForm(EMPTY_CREATE);
    setCreateError(null);
    setCreateOpen(true);
  };

  const openRoles = (user: UserSummary) => {
    setEditingUser(user);
    setEditRoles(user.roles);
    setRolesError(null);
    setRolesOpen(true);
  };

  const toggleRole = (roles: string[], code: string) =>
    roles.includes(code) ? roles.filter((r) => r !== code) : [...roles, code];

  const emailInvalid = form.email.length > 0 && !EMAIL_RE.test(form.email);
  const createDisabled =
    create.isPending ||
    form.username.trim().length < 3 ||
    !EMAIL_RE.test(form.email) ||
    form.password.length < 8 ||
    form.roles.length === 0;

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Box>
          <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
            {t('users.title')}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>
            {t('users.subtitle')}
          </Typography>
        </Box>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            {t('users.newUser')}
          </Button>
        )}
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('users.username')}</TableCell>
                <TableCell>{t('users.email')}</TableCell>
                <TableCell>{t('users.roles')}</TableCell>
                <TableCell>{t('common.status')}</TableCell>
                {canManage && <TableCell align="right">{t('common.actions')}</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {!isLoading && users?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">{t('users.none')}</Typography>
                  </TableCell>
                </TableRow>
              )}
              {users?.map((user) => (
                <TableRow key={user.id} hover>
                  <TableCell sx={{ fontWeight: 600 }}>{user.username}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                      {user.roles.length === 0 && <Typography color="text.secondary">—</Typography>}
                      {user.roles.map((role) => (
                        <Chip key={role} label={role} size="small" variant="outlined" />
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <StatusChip status={user.status} />
                  </TableCell>
                  {canManage && (
                    <TableCell align="right">
                      <Tooltip title={t('users.editRoles')}>
                        <IconButton size="small" onClick={() => openRoles(user)}>
                          <BadgeIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title={t('users.deactivate')}>
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            disabled={user.status !== 'ACTIVE'}
                            onClick={async () => {
                              if (
                                await confirm({
                                  title: t('users.deactivateTitle'),
                                  message: t('users.deactivateConfirm', { name: user.username }),
                                  destructive: true,
                                })
                              )
                                deactivate.mutate(user.id);
                            }}
                          >
                            <BlockIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Create user dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{t('users.newUser')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {createError && <Alert severity="error">{createError}</Alert>}
            <TextField
              label={t('users.username')}
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
              fullWidth
              helperText={t('users.usernameHelper')}
            />
            <TextField
              label={t('users.email')}
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
              fullWidth
              error={emailInvalid}
              helperText={emailInvalid ? t('users.emailInvalid') : undefined}
            />
            <TextField
              label={t('users.password')}
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
              fullWidth
              error={form.password.length > 0 && form.password.length < 8}
              helperText={t('users.passwordHelper')}
            />
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                {t('users.roles')}
              </Typography>
              <FormGroup>
                {ROLE_CODES.map((code) => (
                  <FormControlLabel
                    key={code}
                    control={
                      <Checkbox
                        checked={form.roles.includes(code)}
                        onChange={() => setForm({ ...form, roles: toggleRole(form.roles, code) })}
                      />
                    }
                    label={code}
                  />
                ))}
              </FormGroup>
              {form.roles.length === 0 && (
                <Typography variant="caption" color="error">
                  {t('users.rolesRequired')}
                </Typography>
              )}
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>{t('common.cancel')}</Button>
          <Button variant="contained" onClick={() => create.mutate(form)} disabled={createDisabled}>
            {t('common.create')}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit roles dialog */}
      <Dialog open={rolesOpen} onClose={() => setRolesOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{t('users.editRolesFor', { name: editingUser?.username })}</DialogTitle>
        <DialogContent>
          <Stack spacing={1} sx={{ mt: 1 }}>
            {rolesError && <Alert severity="error">{rolesError}</Alert>}
            <FormGroup>
              {ROLE_CODES.map((code) => (
                <FormControlLabel
                  key={code}
                  control={
                    <Checkbox
                      checked={editRoles.includes(code)}
                      onChange={() => setEditRoles(toggleRole(editRoles, code))}
                    />
                  }
                  label={code}
                />
              ))}
            </FormGroup>
            {editRoles.length === 0 && (
              <Typography variant="caption" color="error">
                {t('users.rolesRequired')}
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRolesOpen(false)}>{t('common.cancel')}</Button>
          <Button
            variant="contained"
            onClick={() => editingUser && saveRoles.mutate({ id: editingUser.id, roles: editRoles })}
            disabled={saveRoles.isPending || editRoles.length === 0}
          >
            {t('common.save')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

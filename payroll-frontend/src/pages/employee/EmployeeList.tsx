import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Avatar,
  Button,
  CircularProgress,
  IconButton,
  InputAdornment,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import SearchIcon from '@mui/icons-material/Search';
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline';
import { departmentService, employeeService } from '@/services/employee.service';
import { EMPLOYMENT_STATUSES } from '@/types/employee';
import { useAuth } from '@/context/AuthContext';
import PageHeader from '@/components/common/PageHeader';
import StatusChip from '@/components/common/StatusChip';
import { useConfirm } from '@/components/common/ConfirmDialog';

/** Deterministic avatar tint from a name, so each employee keeps a stable colour. */
const AVATAR_COLORS = ['#3b5bdb', '#0d9488', '#7c3aed', '#d97706', '#db2777', '#0891b2'];
const avatarColor = (s: string) =>
  AVATAR_COLORS[[...s].reduce((a, c) => a + c.charCodeAt(0), 0) % AVATAR_COLORS.length];
const initials = (s: string) =>
  s.split(' ').slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('');

export default function EmployeeList() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const confirm = useConfirm();

  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(25);
  const [search, setSearch] = useState('');
  const [departmentId, setDepartmentId] = useState<number | ''>('');
  const [status, setStatus] = useState<string>('');

  const { data: departments } = useQuery({
    queryKey: ['departments'],
    queryFn: departmentService.list,
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['employees', page, limit, search, departmentId, status],
    queryFn: () =>
      employeeService.list({
        page,
        limit,
        search: search || null,
        departmentId: departmentId === '' ? null : departmentId,
        status: status || null,
      }),
  });

  const remove = useMutation({
    mutationFn: (id: number) => employeeService.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['employees'] }),
  });

  const canCreate = hasPermission('employee.create');
  const canEdit = hasPermission('employee.edit');
  const canDelete = hasPermission('employee.delete');
  const canImport = hasPermission('employee.import');

  return (
    <Stack spacing={3}>
      <PageHeader
        title={t('employees.title')}
        subtitle={data ? t('employees.count', { count: data.total }) : t('employees.manageWorkforce')}
        actions={
          <>
            {canImport && (
              <Button
                component={RouterLink}
                to="/employees/import"
                variant="outlined"
                startIcon={<UploadFileIcon />}
              >
                {t('employees.import')}
              </Button>
            )}
            {canCreate && (
              <Button component={RouterLink} to="/employees/new" variant="contained" startIcon={<AddIcon />}>
                {t('employees.newEmployee')}
              </Button>
            )}
          </>
        }
      />

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            placeholder={t('employees.searchPlaceholder')}
            value={search}
            onChange={(e) => {
              setPage(0);
              setSearch(e.target.value);
            }}
            size="small"
            sx={{ flex: 1 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" color="disabled" />
                </InputAdornment>
              ),
            }}
          />
          <TextField
            select
            label={t('employees.department')}
            value={departmentId}
            onChange={(e) => {
              setPage(0);
              setDepartmentId(e.target.value === '' ? '' : Number(e.target.value));
            }}
            size="small"
            sx={{ minWidth: 180 }}
          >
            <MenuItem value="">{t('common.all')}</MenuItem>
            {departments?.map((d) => (
              <MenuItem key={d.id} value={d.id}>
                {d.name}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label={t('employees.status')}
            value={status}
            onChange={(e) => {
              setPage(0);
              setStatus(e.target.value);
            }}
            size="small"
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="">{t('common.all')}</MenuItem>
            {EMPLOYMENT_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {t(`status.${s}`)}
              </MenuItem>
            ))}
          </TextField>
        </Stack>
      </Paper>

      {isError && <Alert severity="error">Failed to load employees.</Alert>}

      <Paper>
        <TableContainer>
          <Table stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell>{t('employees.id')}</TableCell>
                <TableCell>{t('employees.name')}</TableCell>
                <TableCell>{t('employees.department')}</TableCell>
                <TableCell>{t('employees.position')}</TableCell>
                <TableCell>{t('employees.category')}</TableCell>
                <TableCell>{t('employees.status')}</TableCell>
                <TableCell align="right">{t('common.actions')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              )}

              {!isLoading && data?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} sx={{ py: 8 }}>
                    <Stack alignItems="center" spacing={1.5}>
                      <PeopleOutlineIcon sx={{ fontSize: 48, color: 'grey.300' }} />
                      <Typography fontWeight={600}>{t('employees.noneFound')}</Typography>
                      <Typography color="text.secondary" variant="body2">
                        {t('employees.tryAdjusting')}
                      </Typography>
                    </Stack>
                  </TableCell>
                </TableRow>
              )}

              {data?.data.map((emp) => (
                <TableRow
                  key={emp.id}
                  hover
                  onClick={() => navigate(`/employees/${emp.id}`)}
                  sx={{ cursor: 'pointer' }}
                >
                  <TableCell sx={{ fontWeight: 600, fontVariantNumeric: 'tabular-nums' }}>
                    {emp.employeeId}
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={1.5} alignItems="center">
                      <Avatar
                        sx={{
                          width: 32,
                          height: 32,
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          bgcolor: avatarColor(emp.fullName),
                        }}
                      >
                        {initials(emp.fullName)}
                      </Avatar>
                      <Typography variant="body2" fontWeight={600}>
                        {emp.fullName}
                      </Typography>
                    </Stack>
                  </TableCell>
                  <TableCell>{emp.departmentName}</TableCell>
                  <TableCell>{emp.positionName ?? '—'}</TableCell>
                  <TableCell>{emp.categoryCode}</TableCell>
                  <TableCell>
                    <StatusChip status={emp.employmentStatus} />
                  </TableCell>
                  <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                    <Tooltip title={t('common.view')}>
                      <IconButton size="small" onClick={() => navigate(`/employees/${emp.id}`)}>
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {canEdit && (
                      <Tooltip title={t('common.edit')}>
                        <IconButton size="small" onClick={() => navigate(`/employees/${emp.id}/edit`)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    {canDelete && (
                      <Tooltip title={t('common.delete')}>
                        <IconButton
                          size="small"
                          color="error"
                          onClick={async () => {
                            if (
                              await confirm({
                                title: t('employees.deleteTitle'),
                                message: t('employees.deleteConfirm', { name: emp.fullName }),
                                destructive: true,
                              })
                            )
                              remove.mutate(emp.id);
                          }}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={data?.total ?? 0}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={limit}
          onRowsPerPageChange={(e) => {
            setLimit(Number(e.target.value));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 25, 50, 100]}
        />
      </Paper>
    </Stack>
  );
}

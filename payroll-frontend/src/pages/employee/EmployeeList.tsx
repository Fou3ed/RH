import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  IconButton,
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
import { departmentService, employeeService } from '@/services/employee.service';
import { EMPLOYMENT_STATUSES } from '@/types/employee';
import { useAuth } from '@/context/AuthContext';

const statusColor: Record<string, 'success' | 'default' | 'warning' | 'error'> = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  LEAVE: 'warning',
  TERMINATED: 'error',
};

export default function EmployeeList() {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

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
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
          Employees
        </Typography>
        <Stack direction="row" spacing={1}>
          {canImport && (
            <Button
              component={RouterLink}
              to="/employees/import"
              variant="outlined"
              startIcon={<UploadFileIcon />}
            >
              Import
            </Button>
          )}
          {canCreate && (
            <Button component={RouterLink} to="/employees/new" variant="contained" startIcon={<AddIcon />}>
              New employee
            </Button>
          )}
        </Stack>
      </Stack>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            label="Search (name or ID)"
            value={search}
            onChange={(e) => {
              setPage(0);
              setSearch(e.target.value);
            }}
            size="small"
            sx={{ flex: 1 }}
          />
          <TextField
            select
            label="Department"
            value={departmentId}
            onChange={(e) => {
              setPage(0);
              setDepartmentId(e.target.value === '' ? '' : Number(e.target.value));
            }}
            size="small"
            sx={{ minWidth: 180 }}
          >
            <MenuItem value="">All</MenuItem>
            {departments?.map((d) => (
              <MenuItem key={d.id} value={d.id}>
                {d.name}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Status"
            value={status}
            onChange={(e) => {
              setPage(0);
              setStatus(e.target.value);
            }}
            size="small"
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="">All</MenuItem>
            {EMPLOYMENT_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>
        </Stack>
      </Paper>

      {isError && <Alert severity="error">Failed to load employees.</Alert>}

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Department</TableCell>
                <TableCell>Position</TableCell>
                <TableCell>Category</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                    <CircularProgress size={28} />
                  </TableCell>
                </TableRow>
              )}

              {!isLoading && data?.data.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No employees found.</Typography>
                  </TableCell>
                </TableRow>
              )}

              {data?.data.map((emp) => (
                <TableRow key={emp.id} hover>
                  <TableCell>{emp.employeeId}</TableCell>
                  <TableCell>{emp.fullName}</TableCell>
                  <TableCell>{emp.departmentName}</TableCell>
                  <TableCell>{emp.positionName ?? '—'}</TableCell>
                  <TableCell>{emp.categoryCode}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={emp.employmentStatus}
                      color={statusColor[emp.employmentStatus] ?? 'default'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="View">
                      <IconButton size="small" onClick={() => navigate(`/employees/${emp.id}`)}>
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    {canEdit && (
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => navigate(`/employees/${emp.id}/edit`)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    {canDelete && (
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={() => {
                            if (window.confirm(`Delete ${emp.fullName}?`)) remove.mutate(emp.id);
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

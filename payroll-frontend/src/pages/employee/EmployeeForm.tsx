import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import {
  Alert,
  Box,
  Button,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { departmentService, employeeService } from '@/services/employee.service';
import { referenceService } from '@/services/reference.service';
import { EMPLOYMENT_STATUSES, type EmployeeFormValues } from '@/types/employee';
import type { ApiError } from '@/types/api';

const EMPTY: EmployeeFormValues = {
  employeeId: '',
  firstName: '',
  lastName: '',
  hireDate: '',
  departmentId: '',
  positionId: '',
  categoryId: '',
  echelon: '',
  gender: '',
  email: '',
  cnssNumber: '',
  familyStatus: '',
  numberOfChildren: '',
  phoneNumber: '',
  employmentStatus: 'ACTIVE',
};

export default function EmployeeForm() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [form, setForm] = useState<EmployeeFormValues>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const { data: departments } = useQuery({ queryKey: ['departments'], queryFn: departmentService.list });
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: referenceService.categories });
  const { data: existing } = useQuery({
    queryKey: ['employee', id],
    queryFn: () => employeeService.get(Number(id)),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing) {
      setForm({
        firstName: existing.firstName,
        lastName: existing.lastName,
        hireDate: existing.hireDate,
        departmentId: existing.departmentId,
        positionId: existing.positionId ?? '',
        categoryId: existing.categoryId,
        echelon: existing.echelon ?? '',
        gender: existing.gender ?? '',
        email: existing.email ?? '',
        cnssNumber: existing.cnssNumber ?? '',
        familyStatus: existing.familyStatus ?? '',
        numberOfChildren: existing.numberOfChildren ?? '',
        phoneNumber: existing.phoneNumber ?? '',
        employmentStatus: existing.employmentStatus,
      });
    }
  }, [existing]);

  const set = (key: keyof EmployeeFormValues, value: unknown) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const mutation = useMutation({
    mutationFn: (values: EmployeeFormValues) =>
      isEdit ? employeeService.update(Number(id), values) : employeeService.create(values),
    onSuccess: (emp) => {
      queryClient.invalidateQueries({ queryKey: ['employees'] });
      navigate(`/employees/${emp.id}`);
    },
    onError: (err) => {
      const apiError = err as AxiosError<ApiError>;
      setError(apiError.response?.data?.message ?? 'Save failed.');
      setFieldErrors(apiError.response?.data?.fieldErrors ?? {});
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    // Drop empty strings so optional fields are sent as omitted, not "".
    const payload = Object.fromEntries(
      Object.entries(form).filter(([, v]) => v !== '' && v !== undefined),
    ) as unknown as EmployeeFormValues;
    mutation.mutate(payload);
  };

  return (
    <Stack spacing={3}>
      <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
        {isEdit ? 'Edit employee' : 'New employee'}
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}

      <Paper sx={{ p: 3 }} component="form" onSubmit={handleSubmit}>
        <Grid container spacing={2}>
          {!isEdit && (
            <Grid item xs={12} sm={4}>
              <TextField
                label="Employee ID"
                value={form.employeeId}
                onChange={(e) => set('employeeId', e.target.value)}
                error={Boolean(fieldErrors.employeeId)}
                helperText={fieldErrors.employeeId}
                fullWidth
                required
              />
            </Grid>
          )}
          <Grid item xs={12} sm={4}>
            <TextField
              label="First name"
              value={form.firstName}
              onChange={(e) => set('firstName', e.target.value)}
              error={Boolean(fieldErrors.firstName)}
              helperText={fieldErrors.firstName}
              fullWidth
              required
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Last name"
              value={form.lastName}
              onChange={(e) => set('lastName', e.target.value)}
              error={Boolean(fieldErrors.lastName)}
              helperText={fieldErrors.lastName}
              fullWidth
              required
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Hire date"
              type="date"
              value={form.hireDate}
              onChange={(e) => set('hireDate', e.target.value)}
              error={Boolean(fieldErrors.hireDate)}
              helperText={fieldErrors.hireDate}
              InputLabelProps={{ shrink: true }}
              fullWidth
              required
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              select
              label="Department"
              value={form.departmentId}
              onChange={(e) => set('departmentId', Number(e.target.value))}
              error={Boolean(fieldErrors.departmentId)}
              helperText={fieldErrors.departmentId}
              fullWidth
              required
            >
              {departments?.map((d) => (
                <MenuItem key={d.id} value={d.id}>
                  {d.name}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              select
              label="Salary category"
              value={form.categoryId}
              onChange={(e) => set('categoryId', Number(e.target.value))}
              error={Boolean(fieldErrors.categoryId)}
              helperText={fieldErrors.categoryId}
              fullWidth
              required
            >
              {categories?.map((c) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.code}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Échelon"
              type="number"
              value={form.echelon}
              onChange={(e) => set('echelon', e.target.value === '' ? '' : Number(e.target.value))}
              error={Boolean(fieldErrors.echelon)}
              helperText={fieldErrors.echelon}
              inputProps={{ min: 1, max: 14 }}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              select
              label="Gender"
              value={form.gender}
              onChange={(e) => set('gender', e.target.value)}
              error={Boolean(fieldErrors.gender)}
              helperText={fieldErrors.gender}
              fullWidth
            >
              <MenuItem value="">—</MenuItem>
              <MenuItem value="H">H</MenuItem>
              <MenuItem value="M">M</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Email"
              value={form.email}
              onChange={(e) => set('email', e.target.value)}
              error={Boolean(fieldErrors.email)}
              helperText={fieldErrors.email}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="CNSS number"
              value={form.cnssNumber}
              onChange={(e) => set('cnssNumber', e.target.value)}
              error={Boolean(fieldErrors.cnssNumber)}
              helperText={fieldErrors.cnssNumber}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label="Phone"
              value={form.phoneNumber}
              onChange={(e) => set('phoneNumber', e.target.value)}
              fullWidth
            />
          </Grid>
          {isEdit && (
            <Grid item xs={12} sm={4}>
              <TextField
                select
                label="Status"
                value={form.employmentStatus}
                onChange={(e) => set('employmentStatus', e.target.value)}
                fullWidth
              >
                {EMPLOYMENT_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          )}
        </Grid>

        <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
          <Button type="submit" variant="contained" disabled={mutation.isPending}>
            {mutation.isPending ? 'Saving…' : 'Save'}
          </Button>
          <Button variant="outlined" onClick={() => navigate(-1)}>
            Cancel
          </Button>
        </Box>
      </Paper>
    </Stack>
  );
}

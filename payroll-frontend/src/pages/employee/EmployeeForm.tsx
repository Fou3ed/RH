import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
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
} from '@mui/material';
import { departmentService, employeeService } from '@/services/employee.service';
import { referenceService } from '@/services/reference.service';
import { EMPLOYMENT_STATUSES, FAMILY_STATUSES, type EmployeeFormValues } from '@/types/employee';
import type { ApiError } from '@/types/api';
import PageHeader from '@/components/common/PageHeader';

const EMPTY: EmployeeFormValues = {
  employeeId: '',
  firstName: '',
  lastName: '',
  hireDate: '',
  departmentId: '',
  positionId: '',
  categoryId: '',
  echelon: '',
  baseSalary: '',
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
  const { t } = useTranslation();
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
        baseSalary: existing.baseSalary ?? '',
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
      setError(apiError.response?.data?.message ?? t('employees.form.saveFailed'));
      setFieldErrors(apiError.response?.data?.fieldErrors ?? {});
    },
  });

  /** Client-side validation — returns a field→message map (empty means valid). */
  const validate = (values: EmployeeFormValues): Record<string, string> => {
    const e: Record<string, string> = {};
    const required = (v: unknown) => v === '' || v === undefined || v === null;

    if (!isEdit && required(values.employeeId)) e.employeeId = t('employees.form.err.idRequired');
    if (required(values.firstName)) e.firstName = t('employees.form.err.firstNameRequired');
    if (required(values.lastName)) e.lastName = t('employees.form.err.lastNameRequired');
    if (required(values.hireDate)) e.hireDate = t('employees.form.err.hireDateRequired');
    else if (values.hireDate > new Date().toISOString().slice(0, 10))
      e.hireDate = t('employees.form.err.hireDateFuture');
    if (required(values.departmentId)) e.departmentId = t('employees.form.err.departmentRequired');
    if (required(values.categoryId)) e.categoryId = t('employees.form.err.categoryRequired');
    if (required(values.baseSalary)) e.baseSalary = t('employees.form.err.baseSalaryRequired');
    else if (Number(values.baseSalary) <= 0) e.baseSalary = t('employees.form.err.baseSalaryPositive');
    if (values.echelon !== '' && values.echelon !== undefined) {
      const n = Number(values.echelon);
      if (n < 1 || n > 14) e.echelon = t('employees.form.err.echelonRange');
    }
    if (values.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email))
      e.email = t('employees.form.err.emailInvalid');
    return e;
  };

  const handleSubmit = (ev: FormEvent) => {
    ev.preventDefault();
    setError(null);
    const errors = validate(form);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      setError(t('employees.form.fixHighlighted'));
      return;
    }
    // Drop empty strings so optional fields are sent as omitted, not "".
    const payload = Object.fromEntries(
      Object.entries(form).filter(([, v]) => v !== '' && v !== undefined),
    ) as unknown as EmployeeFormValues;
    mutation.mutate(payload);
  };

  return (
    <Stack spacing={3}>
      <PageHeader
        backTo={isEdit ? `/employees/${id}` : '/employees'}
        title={isEdit ? t('employees.form.editTitle') : t('employees.form.newTitle')}
        subtitle={isEdit ? t('employees.form.editSubtitle') : t('employees.form.newSubtitle')}
      />

      {error && <Alert severity="error">{error}</Alert>}

      <Paper sx={{ p: 3 }} component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          {!isEdit && (
            <Grid item xs={12} sm={4}>
              <TextField
                label={t('employees.form.employeeId')}
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
              label={t('employees.form.firstName')}
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
              label={t('employees.form.lastName')}
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
              label={t('employees.form.hireDate')}
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
              label={t('employees.form.department')}
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
              label={t('employees.form.salaryCategory')}
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
              label={t('employees.form.echelon')}
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
              label={t('employees.form.baseSalary')}
              type="number"
              value={form.baseSalary}
              onChange={(e) => set('baseSalary', e.target.value === '' ? '' : Number(e.target.value))}
              error={Boolean(fieldErrors.baseSalary)}
              helperText={fieldErrors.baseSalary ?? t('employees.form.baseSalaryHelper')}
              inputProps={{ min: 0, step: '0.001' }}
              fullWidth
              required
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              select
              label={t('employees.form.gender')}
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
              label={t('employees.form.email')}
              value={form.email}
              onChange={(e) => set('email', e.target.value)}
              error={Boolean(fieldErrors.email)}
              helperText={fieldErrors.email}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label={t('employees.form.cnssNumber')}
              value={form.cnssNumber}
              onChange={(e) => set('cnssNumber', e.target.value)}
              error={Boolean(fieldErrors.cnssNumber)}
              helperText={fieldErrors.cnssNumber}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label={t('employees.form.phone')}
              value={form.phoneNumber}
              onChange={(e) => set('phoneNumber', e.target.value)}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              select
              label={t('employees.form.familyStatus')}
              value={form.familyStatus}
              onChange={(e) => set('familyStatus', e.target.value)}
              error={Boolean(fieldErrors.familyStatus)}
              helperText={fieldErrors.familyStatus ?? t('employees.form.familyStatusHelper')}
              fullWidth
            >
              <MenuItem value="">—</MenuItem>
              {FAMILY_STATUSES.map((s) => (
                <MenuItem key={s.code} value={s.code}>
                  {t(`employees.familyStatus.${s.code}`)}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              label={t('employees.form.numberOfChildren')}
              type="number"
              value={form.numberOfChildren}
              onChange={(e) =>
                set('numberOfChildren', e.target.value === '' ? '' : Number(e.target.value))
              }
              error={Boolean(fieldErrors.numberOfChildren)}
              helperText={fieldErrors.numberOfChildren}
              inputProps={{ min: 0, max: 20 }}
              fullWidth
            />
          </Grid>
          {isEdit && (
            <Grid item xs={12} sm={4}>
              <TextField
                select
                label={t('employees.form.statusLabel')}
                value={form.employmentStatus}
                onChange={(e) => set('employmentStatus', e.target.value)}
                fullWidth
              >
                {EMPLOYMENT_STATUSES.map((s) => (
                  <MenuItem key={s} value={s}>
                    {t(`status.${s}`)}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          )}
        </Grid>

        <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
          <Button type="submit" variant="contained" disabled={mutation.isPending}>
            {mutation.isPending ? t('common.saving') : t('common.save')}
          </Button>
          <Button variant="outlined" onClick={() => navigate(-1)}>
            {t('common.cancel')}
          </Button>
        </Box>
      </Paper>
    </Stack>
  );
}

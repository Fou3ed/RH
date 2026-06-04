import { Link as RouterLink, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { employeeService } from '@/services/employee.service';
import { useAuth } from '@/context/AuthContext';
import type { Employee } from '@/types/employee';

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Grid item xs={12} sm={6} md={4}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography>{value ?? '—'}</Typography>
    </Grid>
  );
}

export default function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['employee', id],
    queryFn: () => employeeService.get(Number(id)),
  });

  if (isLoading) return <CircularProgress />;
  if (isError || !data) return <Alert severity="error">Employee not found.</Alert>;

  const emp: Employee = data;

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Stack direction="row" spacing={2} alignItems="center">
          <Button component={RouterLink} to="/employees" startIcon={<ArrowBackIcon />} size="small">
            Back
          </Button>
          <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
            {emp.fullName}
          </Typography>
          <Chip size="small" label={emp.employmentStatus} />
        </Stack>
        {hasPermission('employee.edit') && (
          <Button
            component={RouterLink}
            to={`/employees/${emp.id}/edit`}
            variant="contained"
            startIcon={<EditIcon />}
          >
            Edit
          </Button>
        )}
      </Stack>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Identity
        </Typography>
        <Grid container spacing={2}>
          <Field label="Employee ID" value={emp.employeeId} />
          <Field label="Gender" value={emp.gender} />
          <Field label="Date of birth" value={emp.dateOfBirth} />
          <Field label="National ID" value={emp.nationalId} />
          <Field label="CNSS number" value={emp.cnssNumber} />
          <Field label="Family status" value={emp.familyStatus} />
        </Grid>

        <Divider sx={{ my: 3 }} />
        <Typography variant="h6" gutterBottom>
          Employment
        </Typography>
        <Grid container spacing={2}>
          <Field label="Department" value={emp.departmentName} />
          <Field label="Position" value={emp.positionName} />
          <Field label="Category" value={emp.categoryCode} />
          <Field label="Échelon" value={emp.echelon} />
          <Field label="Hire date" value={emp.hireDate} />
          <Field label="Base salary" value={emp.baseSalary} />
        </Grid>

        <Divider sx={{ my: 3 }} />
        <Typography variant="h6" gutterBottom>
          Contact &amp; payment
        </Typography>
        <Grid container spacing={2}>
          <Field label="Email" value={emp.email} />
          <Field label="Phone" value={emp.phoneNumber} />
          <Field label="City" value={emp.city} />
          <Field label="Payment method" value={emp.paymentMethod} />
          <Field label="Bank account" value={emp.bankAccountNumber} />
          <Field label="Bank code" value={emp.bankCode} />
        </Grid>
      </Paper>
    </Stack>
  );
}

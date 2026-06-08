import { type ReactNode } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Grid,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import WorkOutlineIcon from '@mui/icons-material/WorkOutline';
import ContactMailOutlinedIcon from '@mui/icons-material/ContactMailOutlined';
import { employeeService } from '@/services/employee.service';
import { useAuth } from '@/context/AuthContext';
import EmployeeDocuments from '@/components/employee/EmployeeDocuments';
import PageHeader from '@/components/common/PageHeader';
import StatusChip from '@/components/common/StatusChip';
import type { Employee } from '@/types/employee';

const AVATAR_COLORS = ['#3b5bdb', '#0d9488', '#7c3aed', '#d97706', '#db2777', '#0891b2'];
const avatarColor = (s: string) =>
  AVATAR_COLORS[[...s].reduce((a, c) => a + c.charCodeAt(0), 0) % AVATAR_COLORS.length];
const initials = (s: string) =>
  s.split(' ').slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('');

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <Grid item xs={12} sm={6} md={4}>
      <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, letterSpacing: '0.02em' }}>
        {label}
      </Typography>
      <Typography sx={{ fontWeight: 500 }}>{value ?? '—'}</Typography>
    </Grid>
  );
}

function SectionCard({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
  return (
    <Paper sx={{ p: 3 }}>
      <Stack direction="row" spacing={1.5} alignItems="center" mb={2.5}>
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: 2,
            display: 'grid',
            placeItems: 'center',
            color: 'primary.main',
            bgcolor: (t) => `${t.palette.primary.main}1f`,
          }}
        >
          {icon}
        </Box>
        <Typography variant="h5">{title}</Typography>
      </Stack>
      <Grid container spacing={2.5}>
        {children}
      </Grid>
    </Paper>
  );
}

export default function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const { t } = useTranslation();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['employee', id],
    queryFn: () => employeeService.get(Number(id)),
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }
  if (isError || !data) return <Alert severity="error">{t('employees.detail.notFound')}</Alert>;

  const emp: Employee = data;

  return (
    <Stack spacing={3}>
      <PageHeader
        backTo="/employees"
        adornment={
          <Avatar
            sx={{
              width: 52,
              height: 52,
              fontWeight: 700,
              fontSize: '1.1rem',
              bgcolor: avatarColor(emp.fullName),
            }}
          >
            {initials(emp.fullName)}
          </Avatar>
        }
        title={
          <Stack direction="row" spacing={1.5} alignItems="center">
            <span>{emp.fullName}</span>
            <StatusChip status={emp.employmentStatus} />
          </Stack>
        }
        subtitle={`${emp.employeeId} · ${emp.departmentName}`}
        actions={
          hasPermission('employee.edit') && (
            <Button
              component={RouterLink}
              to={`/employees/${emp.id}/edit`}
              variant="contained"
              startIcon={<EditIcon />}
            >
              {t('common.edit')}
            </Button>
          )
        }
      />

      <SectionCard icon={<BadgeOutlinedIcon />} title={t('employees.detail.identity')}>
        <Field label={t('employees.detail.employeeId')} value={emp.employeeId} />
        <Field label={t('employees.detail.gender')} value={emp.gender} />
        <Field label={t('employees.detail.dateOfBirth')} value={emp.dateOfBirth} />
        <Field label={t('employees.detail.nationalId')} value={emp.nationalId} />
        <Field label={t('employees.detail.cnssNumber')} value={emp.cnssNumber} />
        <Field label={t('employees.detail.familyStatus')} value={emp.familyStatus} />
      </SectionCard>

      <SectionCard icon={<WorkOutlineIcon />} title={t('employees.detail.employment')}>
        <Field label={t('employees.detail.department')} value={emp.departmentName} />
        <Field label={t('employees.detail.position')} value={emp.positionName} />
        <Field label={t('employees.detail.category')} value={emp.categoryCode} />
        <Field label={t('employees.detail.echelon')} value={emp.echelon} />
        <Field label={t('employees.detail.hireDate')} value={emp.hireDate} />
        <Field
          label={t('employees.detail.baseSalary')}
          value={emp.baseSalary != null ? `${emp.baseSalary.toLocaleString()} TND` : '—'}
        />
      </SectionCard>

      <SectionCard icon={<ContactMailOutlinedIcon />} title={t('employees.detail.contactPayment')}>
        <Field label={t('employees.detail.email')} value={emp.email} />
        <Field label={t('employees.detail.phone')} value={emp.phoneNumber} />
        <Field label={t('employees.detail.city')} value={emp.city} />
        <Field label={t('employees.detail.paymentMethod')} value={emp.paymentMethod} />
        <Field label={t('employees.detail.bankAccount')} value={emp.bankAccountNumber} />
        <Field label={t('employees.detail.bankCode')} value={emp.bankCode} />
      </SectionCard>

      <EmployeeDocuments employeeId={emp.id} />
    </Stack>
  );
}

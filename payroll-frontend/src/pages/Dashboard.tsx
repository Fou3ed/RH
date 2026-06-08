import { type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  LinearProgress,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
  alpha,
} from '@mui/material';
import GroupsIcon from '@mui/icons-material/Groups';
import HowToRegIcon from '@mui/icons-material/HowToReg';
import PersonOffIcon from '@mui/icons-material/PersonOff';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import PaymentsIcon from '@mui/icons-material/Payments';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import { dashboardService } from '@/services/dashboard.service';
import { useAuth } from '@/context/AuthContext';

function KpiCard({
  label,
  value,
  icon,
  color = '#3b5bdb',
  hint,
}: {
  label: string;
  value: string | number;
  icon: ReactNode;
  color?: string;
  hint?: string;
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, letterSpacing: '0.04em', textTransform: 'uppercase' }}>
              {label}
            </Typography>
            <Typography variant="h3" sx={{ fontWeight: 800, mt: 0.5, color: 'text.primary' }}>
              {value}
            </Typography>
            {hint && (
              <Typography variant="caption" color="text.secondary">
                {hint}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.5,
              display: 'grid',
              placeItems: 'center',
              color,
              bgcolor: alpha(color, 0.12),
              flexShrink: 0,
            }}
          >
            {icon}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

function SectionTitle({ children }: { children: ReactNode }) {
  return (
    <Typography variant="h4" sx={{ mb: 0.5 }}>
      {children}
    </Typography>
  );
}

export default function Dashboard() {
  const { user, hasPermission } = useAuth();
  const { t } = useTranslation();
  const canHr = hasPermission('employee.view');
  const canPayroll = hasPermission('payroll.view');

  const hr = useQuery({ queryKey: ['dashboard-hr'], queryFn: dashboardService.hr, enabled: canHr });
  const payroll = useQuery({ queryKey: ['dashboard-payroll'], queryFn: dashboardService.payroll, enabled: canPayroll });

  const maxDept = Math.max(1, ...(hr.data?.byDepartment.map((d) => d.count) ?? [1]));

  return (
    <Stack spacing={4}>
      <Box>
        <Typography variant="h1">
          {user ? t('dashboard.welcome', { name: user.username }) : t('dashboard.welcomeNoName')}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
          {t('dashboard.subtitle')}
        </Typography>
      </Box>

      {canHr && hr.isLoading && (
        <Box sx={{ display: 'grid', placeItems: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      )}

      {canHr && hr.data && (
        <Stack spacing={2}>
          <SectionTitle>{t('dashboard.people')}</SectionTitle>
          <Grid container spacing={2.5}>
            <Grid item xs={6} md={3}>
              <KpiCard label={t('dashboard.headcount')} value={hr.data.totalEmployees} icon={<GroupsIcon />} />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard label={t('dashboard.active')} value={hr.data.activeEmployees} icon={<HowToRegIcon />} color="#16a34a" />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard label={t('dashboard.inactive')} value={hr.data.inactiveEmployees} icon={<PersonOffIcon />} color="#64748b" />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard label={t('dashboard.newHires')} value={hr.data.newHiresThisMonth} icon={<PersonAddIcon />} color="#0d9488" hint={t('dashboard.thisMonth')} />
            </Grid>
          </Grid>

          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3, height: '100%' }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2}>
                  <Typography variant="h5">{t('dashboard.headcountByDept')}</Typography>
                  <Chip
                    size="small"
                    icon={<EventAvailableIcon sx={{ fontSize: 16 }} />}
                    label={t('dashboard.attendance', { value: (hr.data.attendanceRateThisMonth * 100).toFixed(1) })}
                    sx={{ bgcolor: alpha('#3b5bdb', 0.1), color: '#3b5bdb' }}
                  />
                </Stack>
                {hr.data.byDepartment.length === 0 && (
                  <Typography color="text.secondary">{t('dashboard.noEmployees')}</Typography>
                )}
                <Stack spacing={2}>
                  {hr.data.byDepartment.map((d) => (
                    <Box key={d.department}>
                      <Stack direction="row" justifyContent="space-between" mb={0.5}>
                        <Typography variant="body2" fontWeight={500}>
                          {d.department}
                        </Typography>
                        <Typography variant="body2" fontWeight={700}>
                          {d.count}
                        </Typography>
                      </Stack>
                      <LinearProgress
                        variant="determinate"
                        value={(d.count / maxDept) * 100}
                        sx={{ height: 8 }}
                      />
                    </Box>
                  ))}
                </Stack>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3, height: '100%' }}>
                <Typography variant="h5" gutterBottom>
                  {t('dashboard.recentHires')}
                </Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>{t('dashboard.id')}</TableCell>
                      <TableCell>{t('dashboard.name')}</TableCell>
                      <TableCell>{t('dashboard.department')}</TableCell>
                      <TableCell>{t('dashboard.hireDate')}</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {hr.data.recentHires.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary' }}>
                          {t('dashboard.noHires')}
                        </TableCell>
                      </TableRow>
                    )}
                    {hr.data.recentHires.map((e) => (
                      <TableRow key={e.employeeId} hover>
                        <TableCell sx={{ fontWeight: 600 }}>{e.employeeId}</TableCell>
                        <TableCell>{e.fullName}</TableCell>
                        <TableCell>{e.department ?? '—'}</TableCell>
                        <TableCell>{e.hireDate}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            </Grid>
          </Grid>
        </Stack>
      )}

      {canPayroll && payroll.data && (
        <Stack spacing={2}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <SectionTitle>{t('dashboard.latestPayrollPeriod')}</SectionTitle>
            {payroll.data.periodCode && (
              <Chip label={`${payroll.data.periodCode} · ${payroll.data.status}`} color="primary" variant="outlined" />
            )}
          </Stack>
          {!payroll.data.periodCode ? (
            <Paper sx={{ p: 3 }}>
              <Typography color="text.secondary">{t('dashboard.noPeriodCreated')}</Typography>
            </Paper>
          ) : (
            <>
              <Grid container spacing={2.5}>
                <Grid item xs={6} md={3}>
                  <KpiCard
                    label={t('dashboard.calculated')}
                    value={`${payroll.data.calculated}/${payroll.data.activeEmployees}`}
                    icon={<PaymentsIcon />}
                  />
                </Grid>
                <Grid item xs={6} md={3}>
                  <KpiCard label={t('dashboard.approved')} value={payroll.data.approved} icon={<FactCheckIcon />} color="#16a34a" />
                </Grid>
                <Grid item xs={6} md={3}>
                  <KpiCard label={t('dashboard.pending')} value={payroll.data.pendingApproval} icon={<PendingActionsIcon />} color="#d97706" hint={t('dashboard.awaitingApproval')} />
                </Grid>
                <Grid item xs={6} md={3}>
                  <KpiCard label={t('dashboard.netTotal')} value={`${payroll.data.totalNet.toLocaleString()} TND`} icon={<PaymentsIcon />} color="#3b5bdb" />
                </Grid>
              </Grid>
              <Paper sx={{ p: 3 }}>
                <Stack direction="row" justifyContent="space-between" mb={1}>
                  <Typography variant="body2" fontWeight={600}>
                    {t('dashboard.periodCompletion')}
                  </Typography>
                  <Typography variant="body2" fontWeight={700} color="primary.main">
                    {payroll.data.percentComplete}%
                  </Typography>
                </Stack>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(100, payroll.data.percentComplete)}
                  sx={{ height: 10 }}
                />
              </Paper>
            </>
          )}
        </Stack>
      )}
    </Stack>
  );
}

import { useQuery } from '@tanstack/react-query';
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
} from '@mui/material';
import { dashboardService } from '@/services/dashboard.service';
import { useAuth } from '@/context/AuthContext';

function KpiCard({ label, value, color }: { label: string; value: string | number; color?: string }) {
  return (
    <Card>
      <CardContent>
        <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase' }}>
          {label}
        </Typography>
        <Typography variant="h4" sx={{ fontWeight: 700, color }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

export default function Dashboard() {
  const { user, hasPermission } = useAuth();
  const canHr = hasPermission('employee.view');
  const canPayroll = hasPermission('payroll.view');

  const hr = useQuery({ queryKey: ['dashboard-hr'], queryFn: dashboardService.hr, enabled: canHr });
  const payroll = useQuery({ queryKey: ['dashboard-payroll'], queryFn: dashboardService.payroll, enabled: canPayroll });

  const maxDept = Math.max(1, ...(hr.data?.byDepartment.map((d) => d.count) ?? [1]));

  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
          Welcome{user ? `, ${user.username}` : ''}
        </Typography>
        <Typography color="text.secondary">MARAM Confection Payroll Platform</Typography>
      </div>

      {canHr && hr.isLoading && <CircularProgress />}

      {canHr && hr.data && (
        <>
          <Grid container spacing={2}>
            <Grid item xs={6} md={3}>
              <KpiCard label="Headcount" value={hr.data.totalEmployees} />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard label="Active" value={hr.data.activeEmployees} color="#2e7d32" />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard label="Inactive" value={hr.data.inactiveEmployees} color="#9e9e9e" />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard label="New hires (month)" value={hr.data.newHiresThisMonth} />
            </Grid>
            <Grid item xs={6} md={3}>
              <KpiCard
                label="Attendance (month)"
                value={`${(hr.data.attendanceRateThisMonth * 100).toFixed(1)}%`}
                color="#1565c0"
              />
            </Grid>
          </Grid>

          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Headcount by department
                </Typography>
                {hr.data.byDepartment.length === 0 && (
                  <Typography color="text.secondary">No employees yet.</Typography>
                )}
                <Stack spacing={1.5}>
                  {hr.data.byDepartment.map((d) => (
                    <Box key={d.department}>
                      <Stack direction="row" justifyContent="space-between">
                        <Typography variant="body2">{d.department}</Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {d.count}
                        </Typography>
                      </Stack>
                      <LinearProgress
                        variant="determinate"
                        value={(d.count / maxDept) * 100}
                        sx={{ height: 8, borderRadius: 4 }}
                      />
                    </Box>
                  ))}
                </Stack>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Recent hires
                </Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>ID</TableCell>
                      <TableCell>Name</TableCell>
                      <TableCell>Department</TableCell>
                      <TableCell>Hire date</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {hr.data.recentHires.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary' }}>
                          No hires yet.
                        </TableCell>
                      </TableRow>
                    )}
                    {hr.data.recentHires.map((e) => (
                      <TableRow key={e.employeeId}>
                        <TableCell>{e.employeeId}</TableCell>
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
        </>
      )}

      {canPayroll && payroll.data && (
        <Paper sx={{ p: 3 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Latest payroll period</Typography>
            {payroll.data.periodCode && (
              <Chip label={`${payroll.data.periodCode} · ${payroll.data.status}`} color="info" />
            )}
          </Stack>
          {!payroll.data.periodCode ? (
            <Typography color="text.secondary">No payroll period created yet.</Typography>
          ) : (
            <Grid container spacing={2}>
              <Grid item xs={6} md={3}>
                <KpiCard label="Calculated" value={`${payroll.data.calculated}/${payroll.data.activeEmployees}`} />
              </Grid>
              <Grid item xs={6} md={3}>
                <KpiCard label="Approved" value={payroll.data.approved} color="#2e7d32" />
              </Grid>
              <Grid item xs={6} md={3}>
                <KpiCard label="Pending approval" value={payroll.data.pendingApproval} color="#ed6c02" />
              </Grid>
              <Grid item xs={6} md={3}>
                <KpiCard label="Net total (TND)" value={payroll.data.totalNet.toLocaleString()} color="#1565c0" />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="caption" color="text.secondary">
                  Completion: {payroll.data.percentComplete}%
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(100, payroll.data.percentComplete)}
                  sx={{ height: 10, borderRadius: 5, mt: 0.5 }}
                />
              </Grid>
            </Grid>
          )}
        </Paper>
      )}
    </Stack>
  );
}

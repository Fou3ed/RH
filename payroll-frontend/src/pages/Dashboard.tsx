import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Stack,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import { systemService } from '@/services/system.service';
import { useAuth } from '@/context/AuthContext';

/**
 * Landing page for the foundation milestone. Verifies end-to-end connectivity
 * between the SPA and the Spring Boot API.
 */
export default function Dashboard() {
  const { user } = useAuth();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['system-info'],
    queryFn: systemService.getInfo,
  });

  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h1" gutterBottom>
          Welcome{user ? `, ${user.username}` : ''}
        </Typography>
        <Typography color="text.secondary">
          MARAM Confection Payroll Platform — development environment.
        </Typography>
      </div>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h2" gutterBottom>
                Backend connectivity
              </Typography>

              {isLoading && (
                <Stack direction="row" spacing={1} alignItems="center">
                  <CircularProgress size={20} />
                  <Typography>Contacting API…</Typography>
                </Stack>
              )}

              {isError && (
                <Alert severity="error" icon={<ErrorIcon />}>
                  Could not reach the backend. Is it running on port 8080?
                </Alert>
              )}

              {data && (
                <Stack spacing={1.5}>
                  <Chip
                    color="success"
                    icon={<CheckCircleIcon />}
                    label={`API ${data.status}`}
                    sx={{ width: 'fit-content' }}
                  />
                  <Typography>
                    <strong>Application:</strong> {data.application}
                  </Typography>
                  <Typography>
                    <strong>Version:</strong> {data.version}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Checked at {new Date(data.timestamp).toLocaleString()}
                  </Typography>
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h2" gutterBottom>
                Next steps
              </Typography>
              <Typography component="ul" sx={{ pl: 2, m: 0 }} color="text.secondary">
                <li>✅ Sprint 2 — Authentication &amp; RBAC</li>
                <li>✅ Sprint 3 — Employee management</li>
                <li>✅ Sprint 4 — Attendance tracking</li>
                <li>✅ Sprint 5 — Payroll configuration</li>
                <li>Sprint 6 — Payroll calculation engine</li>
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Stack>
  );
}

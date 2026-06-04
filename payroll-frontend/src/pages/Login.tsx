import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { AxiosError } from 'axios';
import { useAuth } from '@/context/AuthContext';
import type { ApiError } from '@/types/api';

interface LocationState {
  from?: { pathname: string };
}

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = (location.state as LocationState)?.from?.pathname ?? '/';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ username, password });
      navigate(redirectTo, { replace: true });
    } catch (err) {
      const apiError = err as AxiosError<ApiError>;
      setError(apiError.response?.data?.message ?? 'Login failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const disabled = submitting || username.trim() === '' || password.trim() === '';

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 400 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={1} alignItems="center" mb={3}>
            <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
              MARAM Payroll
            </Typography>
            <Typography color="text.secondary">Sign in to continue</Typography>
          </Stack>

          <form onSubmit={handleSubmit}>
            <Stack spacing={2}>
              {error && <Alert severity="error">{error}</Alert>}
              <TextField
                label="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                autoFocus
                fullWidth
              />
              <TextField
                label="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                fullWidth
              />
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={disabled}
                startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : null}
              >
                {submitting ? 'Signing in…' : 'Sign in'}
              </Button>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}

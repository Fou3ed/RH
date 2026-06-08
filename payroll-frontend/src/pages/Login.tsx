import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import BoltOutlinedIcon from '@mui/icons-material/BoltOutlined';
import InsightsOutlinedIcon from '@mui/icons-material/InsightsOutlined';
import { AxiosError } from 'axios';
import { useAuth } from '@/context/AuthContext';
import LanguageSwitcher from '@/components/common/LanguageSwitcher';
import type { ApiError } from '@/types/api';

interface LocationState {
  from?: { pathname: string };
}

const HIGHLIGHT_ICONS = [<ShieldOutlinedIcon />, <BoltOutlinedIcon />, <InsightsOutlinedIcon />];

export default function Login() {
  const { login } = useAuth();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = (location.state as LocationState)?.from?.pathname ?? '/';

  const highlights = [1, 2, 3].map((n, i) => ({
    icon: HIGHLIGHT_ICONS[i],
    title: t(`auth.highlight${n}Title`),
    text: t(`auth.highlight${n}Text`),
  }));

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
      setError(apiError.response?.data?.message ?? t('auth.loginFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  const disabled = submitting || username.trim() === '' || password.trim() === '';

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', bgcolor: 'background.default' }}>
      {/* Brand / marketing panel */}
      <Box
        sx={{
          flex: 1.1,
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: 6,
          color: '#fff',
          position: 'relative',
          overflow: 'hidden',
          background: 'linear-gradient(150deg, #2f49b0 0%, #3b5bdb 45%, #5c7cfa 100%)',
        }}
      >
        <Box
          sx={{
            position: 'absolute',
            width: 480,
            height: 480,
            borderRadius: '50%',
            top: -160,
            right: -160,
            background: 'rgba(255,255,255,0.08)',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            width: 320,
            height: 320,
            borderRadius: '50%',
            bottom: -120,
            left: -80,
            background: 'rgba(255,255,255,0.06)',
          }}
        />

        <Stack direction="row" alignItems="center" spacing={1.5} sx={{ position: 'relative' }}>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.5,
              display: 'grid',
              placeItems: 'center',
              fontWeight: 800,
              fontSize: '1.25rem',
              bgcolor: 'rgba(255,255,255,0.15)',
              backdropFilter: 'blur(4px)',
            }}
          >
            M
          </Box>
          <Typography sx={{ fontWeight: 800, fontSize: '1.25rem', letterSpacing: '-0.02em' }}>
            MARAM Payroll
          </Typography>
        </Stack>

        <Box sx={{ position: 'relative' }}>
          <Typography sx={{ fontSize: '2.25rem', fontWeight: 800, letterSpacing: '-0.03em', lineHeight: 1.15, mb: 4 }}>
            {t('auth.heroTitle')}
          </Typography>
          <Stack spacing={2.5}>
            {highlights.map((h) => (
              <Stack key={h.title} direction="row" spacing={2} alignItems="flex-start">
                <Box
                  sx={{
                    width: 40,
                    height: 40,
                    borderRadius: 2,
                    display: 'grid',
                    placeItems: 'center',
                    bgcolor: 'rgba(255,255,255,0.15)',
                    flexShrink: 0,
                  }}
                >
                  {h.icon}
                </Box>
                <Box>
                  <Typography sx={{ fontWeight: 700 }}>{h.title}</Typography>
                  <Typography sx={{ opacity: 0.85, fontSize: '0.875rem' }}>{h.text}</Typography>
                </Box>
              </Stack>
            ))}
          </Stack>
        </Box>

        <Typography sx={{ position: 'relative', opacity: 0.7, fontSize: '0.8rem' }}>
          {t('auth.footer')}
        </Typography>
      </Box>

      {/* Form panel */}
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: { xs: 3, sm: 6 } }}>
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <LanguageSwitcher color="inherit" />
        </Box>
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Paper sx={{ width: '100%', maxWidth: 400, p: { xs: 3, sm: 5 }, border: '1px solid', borderColor: 'divider' }}>
            <Stack spacing={1} mb={4}>
              <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
                {t('auth.signIn')}
              </Typography>
              <Typography color="text.secondary">{t('auth.welcomeBack')}</Typography>
            </Stack>

            <form onSubmit={handleSubmit}>
              <Stack spacing={2.5}>
                {error && <Alert severity="error">{error}</Alert>}
                <TextField
                  label={t('auth.username')}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                  autoFocus
                  fullWidth
                />
                <TextField
                  label={t('auth.password')}
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
                  sx={{ mt: 0.5 }}
                >
                  {submitting ? t('auth.signingIn') : t('auth.signIn')}
                </Button>
              </Stack>
            </form>
          </Paper>
        </Box>
      </Box>
    </Box>
  );
}

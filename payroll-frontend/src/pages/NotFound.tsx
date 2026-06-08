import { Button, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function NotFound() {
  const { t } = useTranslation();
  return (
    <Stack spacing={2} alignItems="flex-start">
      <Typography variant="h1">404</Typography>
      <Typography color="text.secondary">{t('errors.notFoundDesc')}</Typography>
      <Button component={Link} to="/" variant="contained">
        {t('common.backToDashboard')}
      </Button>
    </Stack>
  );
}

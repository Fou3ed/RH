import { type ReactNode } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';

interface PageHeaderProps {
  title: ReactNode;
  subtitle?: ReactNode;
  /** Renders a "Back" button linking to this route, left of the title. */
  backTo?: string;
  /** Right-aligned action buttons / chips. */
  actions?: ReactNode;
  /** Optional adornment shown left of the title (e.g. an avatar). */
  adornment?: ReactNode;
}

/** Consistent page title block: optional back button, title + subtitle, right-aligned actions. */
export default function PageHeader({ title, subtitle, backTo, actions, adornment }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      justifyContent="space-between"
      alignItems={{ xs: 'flex-start', sm: 'center' }}
      spacing={2}
    >
      <Stack direction="row" spacing={2} alignItems="center">
        {backTo && (
          <Button component={RouterLink} to={backTo} startIcon={<ArrowBackIcon />} size="small" color="inherit">
            Back
          </Button>
        )}
        {adornment}
        <Box>
          <Typography variant="h1">{title}</Typography>
          {subtitle && (
            <Typography color="text.secondary" sx={{ mt: 0.25 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
      </Stack>
      {actions && (
        <Stack direction="row" spacing={1.5} flexShrink={0}>
          {actions}
        </Stack>
      )}
    </Stack>
  );
}

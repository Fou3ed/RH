import { Button, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <Stack spacing={2} alignItems="flex-start">
      <Typography variant="h1">404</Typography>
      <Typography color="text.secondary">This page does not exist.</Typography>
      <Button component={Link} to="/" variant="contained">
        Back to dashboard
      </Button>
    </Stack>
  );
}

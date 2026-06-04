import { Button, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export default function Unauthorized() {
  return (
    <Stack spacing={2} alignItems="flex-start">
      <Typography variant="h1">403</Typography>
      <Typography color="text.secondary">
        You don&apos;t have permission to view this page.
      </Typography>
      <Button component={Link} to="/" variant="contained">
        Back to dashboard
      </Button>
    </Stack>
  );
}

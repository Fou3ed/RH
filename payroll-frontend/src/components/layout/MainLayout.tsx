import { AppBar, Box, Toolbar, Typography, Container } from '@mui/material';
import type { ReactNode } from 'react';

interface MainLayoutProps {
  children: ReactNode;
}

/** Minimal app shell — header + content area. Sidebar/nav arrive in later sprints. */
export default function MainLayout({ children }: MainLayoutProps) {
  return (
    <Box sx={{ minHeight: '100vh' }}>
      <AppBar position="static" elevation={1}>
        <Toolbar>
          <Typography variant="h6" sx={{ fontWeight: 700, flexGrow: 1 }}>
            MARAM&nbsp;·&nbsp;Payroll
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.8 }}>
            Sprint 1 — Foundation
          </Typography>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        {children}
      </Container>
    </Box>
  );
}

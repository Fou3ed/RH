import { useState, type MouseEvent, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Container,
  IconButton,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import { useAuth } from '@/context/AuthContext';

interface MainLayoutProps {
  children: ReactNode;
}

/** App shell — header with the signed-in user menu + content area. */
export default function MainLayout({ children }: MainLayoutProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  const openMenu = (e: MouseEvent<HTMLElement>) => setAnchor(e.currentTarget);
  const closeMenu = () => setAnchor(null);

  const handleLogout = async () => {
    closeMenu();
    await logout();
    navigate('/login', { replace: true });
  };

  const initials = user?.username?.slice(0, 2).toUpperCase() ?? '?';

  return (
    <Box sx={{ minHeight: '100vh' }}>
      <AppBar position="static" elevation={1}>
        <Toolbar>
          <Typography variant="h6" sx={{ fontWeight: 700, flexGrow: 1 }}>
            MARAM&nbsp;·&nbsp;Payroll
          </Typography>

          {user && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box sx={{ textAlign: 'right', display: { xs: 'none', sm: 'block' } }}>
                <Typography variant="body2" sx={{ lineHeight: 1.2 }}>
                  {user.username}
                </Typography>
                <Typography variant="caption" sx={{ opacity: 0.8 }}>
                  {user.roles.join(', ')}
                </Typography>
              </Box>
              <IconButton onClick={openMenu} size="small" sx={{ p: 0 }}>
                <Avatar sx={{ width: 36, height: 36, bgcolor: 'secondary.main' }}>{initials}</Avatar>
              </IconButton>
              <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={closeMenu}>
                <MenuItem onClick={handleLogout}>Sign out</MenuItem>
              </Menu>
            </Box>
          )}
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        {children}
      </Container>
    </Box>
  );
}

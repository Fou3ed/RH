import { useState, type MouseEvent, type ReactNode } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import ApartmentIcon from '@mui/icons-material/Apartment';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import PaymentsIcon from '@mui/icons-material/Payments';
import SettingsIcon from '@mui/icons-material/Settings';
import { useAuth } from '@/context/AuthContext';

interface MainLayoutProps {
  children: ReactNode;
}

const DRAWER_WIDTH = 220;

interface NavItem {
  label: string;
  to: string;
  icon: ReactNode;
  permission?: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: <DashboardIcon /> },
  { label: 'Employees', to: '/employees', icon: <PeopleIcon />, permission: 'employee.view' },
  { label: 'Attendance', to: '/attendance', icon: <EventAvailableIcon />, permission: 'attendance.view' },
  { label: 'Payroll', to: '/payroll', icon: <PaymentsIcon />, permission: 'payroll.view' },
  { label: 'Payroll Periods', to: '/payroll-periods', icon: <CalendarMonthIcon />, permission: 'payroll.view' },
  { label: 'Departments', to: '/departments', icon: <ApartmentIcon />, permission: 'employee.view' },
  { label: 'Configuration', to: '/configuration', icon: <SettingsIcon />, permission: 'payroll.view' },
];

/** App shell — top bar with user menu + left navigation drawer. */
export default function MainLayout({ children }: MainLayoutProps) {
  const { user, logout, hasPermission } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  const handleLogout = async () => {
    setAnchor(null);
    await logout();
    navigate('/login', { replace: true });
  };

  const initials = user?.username?.slice(0, 2).toUpperCase() ?? '?';
  const visibleNav = NAV_ITEMS.filter((item) => !item.permission || hasPermission(item.permission));

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" elevation={1} sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
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
              <IconButton onClick={(e: MouseEvent<HTMLElement>) => setAnchor(e.currentTarget)} sx={{ p: 0 }}>
                <Avatar sx={{ width: 36, height: 36, bgcolor: 'secondary.main' }}>{initials}</Avatar>
              </IconButton>
              <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={() => setAnchor(null)}>
                <MenuItem onClick={handleLogout}>Sign out</MenuItem>
              </Menu>
            </Box>
          )}
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto' }}>
          <List>
            {visibleNav.map((item) => {
              const selected =
                item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
              return (
                <ListItemButton key={item.to} component={RouterLink} to={item.to} selected={selected}>
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              );
            })}
          </List>
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}

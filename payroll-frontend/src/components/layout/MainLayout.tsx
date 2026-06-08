import { useState, type MouseEvent, type ReactNode } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  AppBar,
  Avatar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Menu,
  MenuItem,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import ApartmentIcon from '@mui/icons-material/Apartment';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth';
import PaymentsIcon from '@mui/icons-material/Payments';
import SettingsIcon from '@mui/icons-material/Settings';
import GroupIcon from '@mui/icons-material/Group';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '@/context/AuthContext';
import LanguageSwitcher from '@/components/common/LanguageSwitcher';

interface MainLayoutProps {
  children: ReactNode;
}

const DRAWER_WIDTH = 248;

interface NavItem {
  labelKey: string;
  to: string;
  icon: ReactNode;
  permission?: string;
  sectionKey: string;
}

const NAV_ITEMS: NavItem[] = [
  { labelKey: 'nav.dashboard', to: '/', icon: <DashboardIcon />, sectionKey: 'nav.overview' },
  { labelKey: 'nav.employees', to: '/employees', icon: <PeopleIcon />, permission: 'employee.view', sectionKey: 'nav.people' },
  { labelKey: 'nav.attendance', to: '/attendance', icon: <EventAvailableIcon />, permission: 'attendance.view', sectionKey: 'nav.people' },
  { labelKey: 'nav.departments', to: '/departments', icon: <ApartmentIcon />, permission: 'employee.view', sectionKey: 'nav.people' },
  { labelKey: 'nav.payrollItem', to: '/payroll', icon: <PaymentsIcon />, permission: 'payroll.view', sectionKey: 'nav.payroll' },
  { labelKey: 'nav.payrollPeriods', to: '/payroll-periods', icon: <CalendarMonthIcon />, permission: 'payroll.view', sectionKey: 'nav.payroll' },
  { labelKey: 'nav.configuration', to: '/configuration', icon: <SettingsIcon />, permission: 'payroll.view', sectionKey: 'nav.payroll' },
  { labelKey: 'nav.users', to: '/users', icon: <GroupIcon />, permission: 'user.manage', sectionKey: 'nav.administration' },
];

/** Square gradient logo mark + wordmark. */
function BrandMark() {
  const { t } = useTranslation();
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
      <Box
        sx={{
          width: 36,
          height: 36,
          borderRadius: 2,
          display: 'grid',
          placeItems: 'center',
          color: '#fff',
          fontWeight: 800,
          fontSize: '1.05rem',
          background: 'linear-gradient(135deg, #5c7cfa 0%, #3b5bdb 100%)',
          boxShadow: '0 2px 8px rgba(59,91,219,0.35)',
        }}
      >
        M
      </Box>
      <Box sx={{ lineHeight: 1 }}>
        <Typography sx={{ fontWeight: 800, fontSize: '1rem', letterSpacing: '-0.02em' }}>
          MARAM
        </Typography>
        <Typography sx={{ fontSize: '0.7rem', color: 'text.secondary', fontWeight: 600, letterSpacing: '0.08em' }}>
          {t('brand.subtitle')}
        </Typography>
      </Box>
    </Box>
  );
}

/** App shell — top bar with user menu + left navigation drawer. */
export default function MainLayout({ children }: MainLayoutProps) {
  const { user, logout, hasPermission } = useAuth();
  const { t } = useTranslation();
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
  const sections = [...new Set(visibleNav.map((i) => i.sectionKey))];

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar sx={{ gap: 2 }}>
          <Box sx={{ width: DRAWER_WIDTH - 32, display: { xs: 'none', md: 'block' } }}>
            <BrandMark />
          </Box>
          <Box sx={{ flexGrow: 1 }} />
          <LanguageSwitcher />
          {user && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, ml: 1 }}>
              <Box sx={{ textAlign: 'right', display: { xs: 'none', sm: 'block' } }}>
                <Typography variant="body2" sx={{ lineHeight: 1.2, fontWeight: 600 }}>
                  {user.username}
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  {user.roles.join(', ')}
                </Typography>
              </Box>
              <Tooltip title={t('account.account')}>
                <IconButton onClick={(e: MouseEvent<HTMLElement>) => setAnchor(e.currentTarget)} sx={{ p: 0.5 }}>
                  <Avatar
                    sx={{
                      width: 38,
                      height: 38,
                      fontSize: '0.85rem',
                      fontWeight: 700,
                      background: 'linear-gradient(135deg, #5c7cfa 0%, #3b5bdb 100%)',
                    }}
                  >
                    {initials}
                  </Avatar>
                </IconButton>
              </Tooltip>
              <Menu
                anchorEl={anchor}
                open={Boolean(anchor)}
                onClose={() => setAnchor(null)}
                transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
              >
                <Box sx={{ px: 2, py: 1 }}>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {user.username}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {user.roles.join(', ')}
                  </Typography>
                </Box>
                <Divider />
                <MenuItem onClick={handleLogout} sx={{ mt: 0.5 }}>
                  <ListItemIcon>
                    <LogoutIcon fontSize="small" />
                  </ListItemIcon>
                  {t('account.signOut')}
                </MenuItem>
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
        <Box sx={{ overflow: 'auto', py: 1 }}>
          {sections.map((section) => (
            <List
              key={section}
              subheader={
                <ListSubheader
                  disableSticky
                  sx={{
                    bgcolor: 'transparent',
                    color: 'text.secondary',
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    letterSpacing: '0.08em',
                    textTransform: 'uppercase',
                    lineHeight: '32px',
                    px: 3,
                  }}
                >
                  {t(section)}
                </ListSubheader>
              }
            >
              {visibleNav
                .filter((item) => item.sectionKey === section)
                .map((item) => {
                  const selected =
                    item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
                  return (
                    <ListItemButton key={item.to} component={RouterLink} to={item.to} selected={selected}>
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={t(item.labelKey)} />
                    </ListItemButton>
                  );
                })}
            </List>
          ))}
        </Box>
        <Box sx={{ mt: 'auto', p: 2 }}>
          <Typography variant="caption" color="text.secondary">
            {t('brand.footer')}
          </Typography>
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 4 }, maxWidth: '100%', overflow: 'hidden' }}>
        <Toolbar />
        <Box sx={{ maxWidth: 1280, mx: 'auto' }}>{children}</Box>
      </Box>
    </Box>
  );
}

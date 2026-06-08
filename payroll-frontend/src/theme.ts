import { createTheme, alpha } from '@mui/material/styles';

/**
 * MARAM payroll design system.
 *
 * A calm, professional fintech aesthetic: an indigo-leaning blue primary,
 * cool slate neutrals, soft layered shadows (no harsh Material elevation),
 * hairline borders on surfaces, and an Inter type scale. Component overrides
 * here cascade to every page, so individual screens stay markup-light.
 */

const brand = {
  primary: '#3b5bdb', // indigo-blue — trustworthy, finance-friendly
  primaryDark: '#2f49b0',
  primaryLight: '#5c7cfa',
  secondary: '#0d9488', // teal accent
};

const slate = {
  50: '#f8fafc',
  100: '#f1f5f9',
  200: '#e2e8f0',
  300: '#cbd5e1',
  500: '#64748b',
  700: '#334155',
  900: '#0f172a',
};

// Soft, low-contrast shadow ramp — replaces MUI's default 25-step elevation.
const softShadow = '0 1px 2px rgba(15,23,42,0.04), 0 1px 3px rgba(15,23,42,0.06)';
const cardShadow = '0 1px 2px rgba(15,23,42,0.04), 0 4px 12px rgba(15,23,42,0.05)';
const popShadow = '0 4px 12px rgba(15,23,42,0.08), 0 12px 32px rgba(15,23,42,0.12)';
const shadows: string[] = Array(25).fill('none');
shadows[1] = softShadow;
shadows[2] = cardShadow;
shadows[3] = cardShadow;
shadows[4] = popShadow;
shadows[8] = popShadow;
shadows[24] = popShadow;

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: brand.primary, dark: brand.primaryDark, light: brand.primaryLight },
    secondary: { main: brand.secondary },
    success: { main: '#16a34a' },
    warning: { main: '#d97706' },
    error: { main: '#dc2626' },
    info: { main: brand.primary },
    background: { default: slate[100], paper: '#ffffff' },
    text: { primary: slate[900], secondary: slate[500] },
    divider: slate[200],
    grey: slate,
  },
  shape: { borderRadius: 12 },
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  shadows: shadows as any,
  typography: {
    fontFamily: "'Inter', 'Roboto', system-ui, -apple-system, sans-serif",
    h1: { fontSize: '1.875rem', fontWeight: 700, letterSpacing: '-0.02em' },
    h2: { fontSize: '1.5rem', fontWeight: 700, letterSpacing: '-0.02em' },
    h3: { fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.01em' },
    h4: { fontSize: '1.125rem', fontWeight: 700, letterSpacing: '-0.01em' },
    h5: { fontSize: '1rem', fontWeight: 600 },
    h6: { fontSize: '0.9375rem', fontWeight: 600, letterSpacing: '0' },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 600 },
    button: { fontWeight: 600, letterSpacing: '0' },
    caption: { letterSpacing: '0.01em' },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          scrollbarColor: `${slate[300]} transparent`,
          '&::-webkit-scrollbar, & *::-webkit-scrollbar': { width: 10, height: 10 },
          '&::-webkit-scrollbar-thumb, & *::-webkit-scrollbar-thumb': {
            borderRadius: 8,
            backgroundColor: slate[300],
            border: '2px solid transparent',
            backgroundClip: 'content-box',
          },
          '&::-webkit-scrollbar-thumb:hover, & *::-webkit-scrollbar-thumb:hover': {
            backgroundColor: slate[500],
          },
        },
      },
    },
    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'inherit' },
      styleOverrides: {
        root: {
          backgroundColor: alpha('#ffffff', 0.8),
          backdropFilter: 'blur(8px)',
          color: slate[900],
          borderBottom: `1px solid ${slate[200]}`,
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: { backgroundColor: '#ffffff', borderRight: `1px solid ${slate[200]}` },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { borderColor: slate[200] },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: `1px solid ${slate[200]}`,
          borderRadius: 16,
          boxShadow: cardShadow,
        },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { textTransform: 'none', borderRadius: 10, paddingInline: 16, fontWeight: 600 },
        sizeLarge: { paddingBlock: 10 },
        containedPrimary: {
          boxShadow: `0 1px 2px ${alpha(brand.primary, 0.4)}`,
          '&:hover': { backgroundColor: brand.primaryDark },
        },
        outlined: { borderColor: slate[300] },
      },
    },
    MuiIconButton: {
      styleOverrides: { root: { borderRadius: 10 } },
    },
    MuiTextField: {
      defaultProps: { size: 'small' },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          backgroundColor: '#ffffff',
          '& .MuiOutlinedInput-notchedOutline': { borderColor: slate[200] },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: slate[300] },
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          marginInline: 8,
          marginBlock: 2,
          paddingBlock: 8,
          color: slate[700],
          '& .MuiListItemIcon-root': { color: slate[500], minWidth: 38 },
          '&:hover': { backgroundColor: slate[100] },
          '&.Mui-selected': {
            backgroundColor: alpha(brand.primary, 0.1),
            color: brand.primary,
            '& .MuiListItemIcon-root': { color: brand.primary },
            '&:hover': { backgroundColor: alpha(brand.primary, 0.14) },
          },
        },
      },
    },
    MuiListItemText: {
      styleOverrides: {
        primary: { fontSize: '0.875rem', fontWeight: 500 },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 8 },
        sizeSmall: { fontSize: '0.75rem' },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            backgroundColor: slate[50],
            color: slate[500],
            fontWeight: 600,
            fontSize: '0.75rem',
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
            borderBottom: `1px solid ${slate[200]}`,
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderBottom: `1px solid ${slate[100]}` },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: { '&:last-child .MuiTableCell-root': { borderBottom: 'none' } },
      },
    },
    MuiMenu: {
      styleOverrides: {
        paper: { borderRadius: 12, border: `1px solid ${slate[200]}`, boxShadow: popShadow },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: { backgroundColor: slate[900], borderRadius: 8, fontSize: '0.75rem', fontWeight: 500 },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 999, backgroundColor: slate[100] },
        bar: { borderRadius: 999 },
      },
    },
    MuiAlert: {
      styleOverrides: { root: { borderRadius: 12 } },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: { height: 3, borderRadius: 3 },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 600, minHeight: 48 },
      },
    },
  },
});

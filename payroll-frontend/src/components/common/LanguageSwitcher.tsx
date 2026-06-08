import { useState, type MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, IconButton, ListItemText, Menu, MenuItem, Tooltip } from '@mui/material';
import TranslateIcon from '@mui/icons-material/Translate';
import CheckIcon from '@mui/icons-material/Check';
import { SUPPORTED_LANGUAGES } from '@/i18n';

/** Language picker — flag + label, persists choice via i18next's localStorage cache. */
export default function LanguageSwitcher({ color = 'inherit' }: { color?: string }) {
  const { i18n, t } = useTranslation();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  const current = SUPPORTED_LANGUAGES.find((l) => i18n.language?.startsWith(l.code)) ?? SUPPORTED_LANGUAGES[0];

  const choose = (code: string) => {
    i18n.changeLanguage(code);
    setAnchor(null);
  };

  return (
    <>
      <Tooltip title={t('common.language')}>
        <IconButton
          onClick={(e: MouseEvent<HTMLElement>) => setAnchor(e.currentTarget)}
          sx={{ color, gap: 0.5 }}
          size="small"
        >
          <TranslateIcon fontSize="small" />
          <Box component="span" sx={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase' }}>
            {current.code}
          </Box>
        </IconButton>
      </Tooltip>
      <Menu
        anchorEl={anchor}
        open={Boolean(anchor)}
        onClose={() => setAnchor(null)}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        {SUPPORTED_LANGUAGES.map((lang) => (
          <MenuItem key={lang.code} selected={lang.code === current.code} onClick={() => choose(lang.code)} sx={{ gap: 1.5 }}>
            <Box component="span" sx={{ fontSize: '1.1rem' }}>
              {lang.flag}
            </Box>
            <ListItemText>{lang.label}</ListItemText>
            {lang.code === current.code && <CheckIcon fontSize="small" color="primary" />}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}

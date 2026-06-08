import { Chip, type ChipProps, alpha } from '@mui/material';
import { useTranslation } from 'react-i18next';

/** Maps domain status codes to a tint colour. Covers employment + payment statuses. */
const STATUS_COLORS: Record<string, string> = {
  ACTIVE: '#16a34a',
  APPROVED: '#16a34a',
  PAID: '#16a34a',
  INACTIVE: '#64748b',
  DRAFT: '#64748b',
  LEAVE: '#d97706',
  PENDING: '#d97706',
  PROCESSING: '#3b5bdb',
  TERMINATED: '#dc2626',
  REJECTED: '#dc2626',
};

interface StatusChipProps extends Omit<ChipProps, 'color' | 'label'> {
  status: string;
}

/** Soft, tinted status pill with a leading dot — consistent across lists and detail views. */
export default function StatusChip({ status, size = 'small', sx, ...rest }: StatusChipProps) {
  const { t } = useTranslation();
  const code = status?.toUpperCase();
  const color = STATUS_COLORS[code] ?? '#64748b';
  // Translate known status codes; fall back to the raw value for anything unmapped.
  const label = t(`status.${code}`, { defaultValue: status });
  return (
    <Chip
      size={size}
      label={label}
      sx={{
        bgcolor: alpha(color, 0.12),
        color,
        fontWeight: 600,
        '& .MuiChip-icon': { color },
        '&::before': {
          content: '""',
          display: 'inline-block',
          width: 6,
          height: 6,
          borderRadius: '50%',
          bgcolor: color,
          ml: 1.25,
          mr: -0.25,
        },
        ...sx,
      }}
      {...rest}
    />
  );
}

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Chip,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { importService } from '@/services/import.service';
import PageHeader from '@/components/common/PageHeader';
import type { ImportPreview } from '@/types/import';

const EXPECTED_COLUMNS =
  'employeeId, firstName, lastName, hireDate (YYYY-MM-DD), departmentCode, categoryCode, echelon, email';

export default function EmployeeImport() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportPreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const onSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFile(e.target.files?.[0] ?? null);
    setPreview(null);
    setError(null);
    setSuccess(null);
  };

  const runPreview = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      setPreview(await importService.preview(file));
    } catch (err) {
      const apiError = err as AxiosError<{ message?: string }>;
      setError(apiError.response?.data?.message ?? t('employeeImport.readError'));
    } finally {
      setBusy(false);
    }
  };

  const runImport = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const result = await importService.commit(file);
      setSuccess(result.message);
      setTimeout(() => navigate('/employees'), 1200);
    } catch (err) {
      const apiError = err as AxiosError<ImportPreview & { message?: string }>;
      // 400 returns the preview with per-row errors.
      if (apiError.response?.data?.rows) {
        setPreview(apiError.response.data);
        setError(t('employeeImport.rejected'));
      } else {
        setError(apiError.response?.data?.message ?? t('employeeImport.failed'));
      }
    } finally {
      setBusy(false);
    }
  };

  const canImport = preview !== null && preview.invalidRows === 0 && preview.totalRows > 0;

  return (
    <Stack spacing={3}>
      <PageHeader backTo="/employees" title={t('employeeImport.title')} />

      <Paper sx={{ p: 3 }}>
        <Typography color="text.secondary" gutterBottom>
          {t('employeeImport.intro')}
        </Typography>
        <Typography variant="body2" sx={{ fontFamily: 'monospace', mb: 2 }}>
          {EXPECTED_COLUMNS}
        </Typography>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <Button component="label" variant="outlined" startIcon={<UploadFileIcon />}>
            {file ? file.name : t('employeeImport.choose')}
            <input
              type="file"
              hidden
              accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              onChange={onSelect}
            />
          </Button>
          <Button variant="contained" onClick={runPreview} disabled={!file || busy}>
            {t('employeeImport.preview')}
          </Button>
          <Button variant="contained" color="success" onClick={runImport} disabled={!canImport || busy}>
            {t('employeeImport.import')} {preview ? `(${preview.validRows})` : ''}
          </Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}

      {preview && (
        <Paper>
          <Box sx={{ p: 2, display: 'flex', gap: 2 }}>
            <Chip label={t('employeeImport.total', { count: preview.totalRows })} />
            <Chip color="success" label={t('employeeImport.valid', { count: preview.validRows })} />
            <Chip color={preview.invalidRows ? 'error' : 'default'} label={t('employeeImport.invalid', { count: preview.invalidRows })} />
          </Box>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('employeeImport.colRow')}</TableCell>
                  <TableCell>{t('employeeImport.colId')}</TableCell>
                  <TableCell>{t('employeeImport.colName')}</TableCell>
                  <TableCell>{t('employeeImport.colStatus')}</TableCell>
                  <TableCell>{t('employeeImport.colErrors')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {preview.rows.map((row) => (
                  <TableRow key={row.rowNumber} sx={{ bgcolor: row.valid ? undefined : 'error.light' }}>
                    <TableCell>{row.rowNumber}</TableCell>
                    <TableCell>{row.employeeId ?? '—'}</TableCell>
                    <TableCell>{row.fullName ?? '—'}</TableCell>
                    <TableCell>
                      <Chip size="small" color={row.valid ? 'success' : 'error'} label={row.valid ? t('employeeImport.ok') : t('employeeImport.error')} />
                    </TableCell>
                    <TableCell>
                      {row.errors.length === 0 ? '—' : row.errors.join('; ')}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}
    </Stack>
  );
}

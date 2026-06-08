import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { attendanceService } from '@/services/attendance.service';
import PageHeader from '@/components/common/PageHeader';
import type { AttendanceImportPreview } from '@/types/attendance';

export default function AttendanceImport() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const now = new Date();
  const months = t('common.months', { returnObjects: true }) as string[];
  const [file, setFile] = useState<File | null>(null);
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [preview, setPreview] = useState<AttendanceImportPreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const runPreview = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      setPreview(await attendanceService.importPreview(file, year, month));
    } catch (err) {
      setError((err as AxiosError<{ message?: string }>).response?.data?.message ?? t('attendanceImport.readError'));
    } finally {
      setBusy(false);
    }
  };

  const runImport = async () => {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const result = await attendanceService.importCommit(file, year, month);
      setSuccess(result.message);
      setTimeout(() => navigate('/attendance'), 1200);
    } catch (err) {
      const apiError = err as AxiosError<AttendanceImportPreview & { message?: string }>;
      if (apiError.response?.data?.rows) {
        setPreview(apiError.response.data);
        setError(t('attendanceImport.rejected'));
      } else {
        setError(apiError.response?.data?.message ?? t('attendanceImport.failed'));
      }
    } finally {
      setBusy(false);
    }
  };

  const canImport = preview !== null && preview.invalidRows === 0 && preview.totalRecords > 0;

  return (
    <Stack spacing={3}>
      <PageHeader backTo="/attendance" title={t('attendanceImport.title')} />

      <Paper sx={{ p: 3 }}>
        <Typography color="text.secondary" gutterBottom>
          {t('attendanceImport.intro')}
        </Typography>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ mt: 2 }}>
          <TextField select label={t('periods.month')} size="small" value={month}
            onChange={(e) => setMonth(Number(e.target.value))} sx={{ minWidth: 140 }}>
            {months.map((m, i) => (
              <MenuItem key={m} value={i + 1}>{m}</MenuItem>
            ))}
          </TextField>
          <TextField label={t('common.year')} type="number" size="small" value={year}
            onChange={(e) => setYear(Number(e.target.value))} sx={{ width: 110 }} />
          <Button component="label" variant="outlined" startIcon={<UploadFileIcon />}>
            {file ? file.name : t('attendanceImport.choose')}
            <input type="file" hidden accept=".xlsx" onChange={(e) => { setFile(e.target.files?.[0] ?? null); setPreview(null); }} />
          </Button>
          <Button variant="contained" onClick={runPreview} disabled={!file || busy}>{t('attendanceImport.preview')}</Button>
          <Button variant="contained" color="success" onClick={runImport} disabled={!canImport || busy}>
            {t('attendanceImport.import')} {preview ? `(${preview.totalRecords})` : ''}
          </Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}

      {preview && (
        <Paper>
          <Box sx={{ p: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Chip label={t('attendanceImport.rows', { count: preview.totalRows })} />
            <Chip color="success" label={t('attendanceImport.valid', { count: preview.validRows })} />
            <Chip color={preview.invalidRows ? 'error' : 'default'} label={t('attendanceImport.invalid', { count: preview.invalidRows })} />
            <Chip label={t('attendanceImport.records', { count: preview.totalRecords })} />
          </Box>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('attendanceImport.colRow')}</TableCell>
                  <TableCell>{t('attendanceImport.colId')}</TableCell>
                  <TableCell>{t('attendanceImport.colDays')}</TableCell>
                  <TableCell>{t('attendanceImport.colStatus')}</TableCell>
                  <TableCell>{t('attendanceImport.colErrors')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {preview.rows.map((row) => (
                  <TableRow key={row.rowNumber} sx={{ bgcolor: row.valid ? undefined : 'error.light' }}>
                    <TableCell>{row.rowNumber}</TableCell>
                    <TableCell>{row.employeeId}</TableCell>
                    <TableCell>{row.recognizedDays}</TableCell>
                    <TableCell>
                      <Chip size="small" color={row.valid ? 'success' : 'error'} label={row.valid ? t('attendanceImport.ok') : t('attendanceImport.error')} />
                    </TableCell>
                    <TableCell>{row.errors.length ? row.errors.join('; ') : '—'}</TableCell>
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

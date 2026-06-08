import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DownloadIcon from '@mui/icons-material/Download';
import { reportService } from '@/services/report.service';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { employeeService } from '@/services/employee.service';
import { attendanceService } from '@/services/attendance.service';
import { useAuth } from '@/context/AuthContext';
import {
  ATTENDANCE_STATUSES,
  STATUS_COLOR,
  type AttendanceRecord,
  type AttendanceStatus,
} from '@/types/attendance';

interface EditState {
  day: number;
  date: string;
  existing: AttendanceRecord | null;
}

export default function AttendanceCalendar() {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const canEdit = hasPermission('attendance.record');
  const weekdays = t('common.weekdays', { returnObjects: true }) as string[];
  const months = t('common.months', { returnObjects: true }) as string[];

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1); // 1-based
  const [employeeId, setEmployeeId] = useState<number | ''>('');
  const [edit, setEdit] = useState<EditState | null>(null);
  const [status, setStatus] = useState<AttendanceStatus>('PRESENT');
  const [hours, setHours] = useState<string>('');

  const { data: employeesPage } = useQuery({
    queryKey: ['employees-all'],
    queryFn: () => employeeService.list({ page: 0, limit: 200 }),
  });

  const { data: summary } = useQuery({
    queryKey: ['attendance-summary', employeeId, year, month],
    queryFn: () => attendanceService.summary(Number(employeeId), year, month),
    enabled: employeeId !== '',
  });

  const recordsByDay = useMemo(() => {
    const map = new Map<number, AttendanceRecord>();
    summary?.records.forEach((r) => map.set(Number(r.attendanceDate.slice(8, 10)), r));
    return map;
  }, [summary]);

  const daysInMonth = new Date(year, month, 0).getDate();
  const firstWeekday = new Date(year, month - 1, 1).getDay();

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['attendance-summary', employeeId, year, month] });

  const save = useMutation({
    mutationFn: () => {
      if (!edit) throw new Error('no cell');
      const hoursValue = hours === '' ? undefined : Number(hours);
      return edit.existing
        ? attendanceService.update(edit.existing.id, { status, hoursWorked: hoursValue })
        : attendanceService.record({
            employeeId: Number(employeeId),
            attendanceDate: edit.date,
            status,
            hoursWorked: hoursValue,
          });
    },
    onSuccess: () => {
      invalidate();
      setEdit(null);
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => attendanceService.remove(id),
    onSuccess: () => {
      invalidate();
      setEdit(null);
    },
  });

  const shiftMonth = (delta: number) => {
    const d = new Date(year, month - 1 + delta, 1);
    setYear(d.getFullYear());
    setMonth(d.getMonth() + 1);
  };

  const openCell = (day: number) => {
    if (!canEdit || employeeId === '') return;
    const existing = recordsByDay.get(day) ?? null;
    const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    setStatus((existing?.attendanceStatus as AttendanceStatus) ?? 'PRESENT');
    setHours(existing?.hoursWorked != null ? String(existing.hoursWorked) : '');
    setEdit({ day, date, existing });
  };

  return (
    <Stack spacing={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h1" sx={{ fontSize: '1.75rem' }}>
          {t('attendance.title')}
        </Typography>
        <Stack direction="row" spacing={1}>
          {hasPermission('report.export') && (
            <Button variant="outlined" startIcon={<DownloadIcon />}
              onClick={() => reportService.attendanceExcel(year, month)}>
              {t('attendance.exportMonth')}
            </Button>
          )}
          {hasPermission('attendance.import') && (
            <Button component={RouterLink} to="/attendance/import" variant="outlined" startIcon={<UploadFileIcon />}>
              {t('attendance.import')}
            </Button>
          )}
        </Stack>
      </Stack>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <TextField
            select
            label={t('attendance.employee')}
            value={employeeId}
            onChange={(e) => setEmployeeId(e.target.value === '' ? '' : Number(e.target.value))}
            size="small"
            sx={{ minWidth: 260 }}
          >
            <MenuItem value="">{t('attendance.selectEmployee')}</MenuItem>
            {employeesPage?.data.map((emp) => (
              <MenuItem key={emp.id} value={emp.id}>
                {emp.employeeId} — {emp.fullName}
              </MenuItem>
            ))}
          </TextField>

          <Box sx={{ flexGrow: 1 }} />

          <Stack direction="row" spacing={1} alignItems="center">
            <IconButton onClick={() => shiftMonth(-1)}>
              <ChevronLeftIcon />
            </IconButton>
            <Typography sx={{ minWidth: 150, textAlign: 'center' }}>
              {months[month - 1]} {year}
            </Typography>
            <IconButton onClick={() => shiftMonth(1)}>
              <ChevronRightIcon />
            </IconButton>
          </Stack>
        </Stack>
      </Paper>

      {summary && (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <Chip color="success" label={t('attendance.present', { count: summary.presentDays })} />
          <Chip color="warning" label={t('attendance.halfDays', { count: summary.halfDays })} />
          <Chip color="error" label={t('attendance.absent', { count: summary.absentDays })} />
          <Chip color="info" label={t('attendance.leave', { count: summary.leaveDays })} />
          <Chip label={t('attendance.daysWorked', { count: summary.daysWorked })} />
          <Chip label={t('attendance.rate', { value: (summary.attendanceRate * 100).toFixed(1) })} />
        </Stack>
      )}

      {employeeId === '' ? (
        <Paper sx={{ p: 4 }}>
          <Typography color="text.secondary" align="center">
            {t('attendance.selectToView')}
          </Typography>
        </Paper>
      ) : (
        <Paper sx={{ p: 2 }}>
          <Grid container columns={7} spacing={1}>
            {weekdays.map((d) => (
              <Grid item xs={1} key={d}>
                <Typography variant="caption" color="text.secondary" align="center" display="block">
                  {d}
                </Typography>
              </Grid>
            ))}
            {Array.from({ length: firstWeekday }).map((_, i) => (
              <Grid item xs={1} key={`blank-${i}`} />
            ))}
            {Array.from({ length: daysInMonth }, (_, i) => i + 1).map((day) => {
              const rec = recordsByDay.get(day);
              const bg = rec ? STATUS_COLOR[rec.attendanceStatus] : '#fff';
              return (
                <Grid item xs={1} key={day}>
                  <Box
                    onClick={() => openCell(day)}
                    sx={{
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 1,
                      bgcolor: bg,
                      minHeight: 64,
                      p: 1,
                      cursor: canEdit ? 'pointer' : 'default',
                      '&:hover': canEdit ? { outline: '2px solid', outlineColor: 'primary.main' } : {},
                    }}
                  >
                    <Typography variant="body2" fontWeight={600}>
                      {day}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {rec ? t(`attendance.st.${rec.attendanceStatus}`, { defaultValue: rec.attendanceStatus }) : ''}
                    </Typography>
                  </Box>
                </Grid>
              );
            })}
          </Grid>
        </Paper>
      )}

      <Dialog open={Boolean(edit)} onClose={() => setEdit(null)} fullWidth maxWidth="xs">
        <DialogTitle>{t('attendance.dialogTitle', { date: edit?.date })}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              label={t('attendance.statusLabel')}
              value={status}
              onChange={(e) => setStatus(e.target.value as AttendanceStatus)}
              fullWidth
            >
              {ATTENDANCE_STATUSES.map((s) => (
                <MenuItem key={s} value={s}>
                  {t(`attendance.st.${s}`, { defaultValue: s })}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label={t('attendance.hoursWorked')}
              type="number"
              value={hours}
              onChange={(e) => setHours(e.target.value)}
              inputProps={{ min: 0, max: 16, step: 0.5 }}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          {edit?.existing && (
            <Button color="error" onClick={() => remove.mutate(edit.existing!.id)} sx={{ mr: 'auto' }}>
              {t('common.delete')}
            </Button>
          )}
          <Button onClick={() => setEdit(null)}>{t('common.cancel')}</Button>
          <Button variant="contained" onClick={() => save.mutate()} disabled={save.isPending}>
            {t('common.save')}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

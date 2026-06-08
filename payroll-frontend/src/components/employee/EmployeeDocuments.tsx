import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useTranslation } from 'react-i18next';
import {
  Alert,
  Box,
  Button,
  IconButton,
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
  Tooltip,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DownloadIcon from '@mui/icons-material/Download';
import DeleteIcon from '@mui/icons-material/Delete';
import { documentService } from '@/services/document.service';
import { DOCUMENT_TYPES, type EmployeeDocument } from '@/types/document';
import { useAuth } from '@/context/AuthContext';
import { useConfirm } from '@/components/common/ConfirmDialog';

function formatSize(bytes: number | null): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function EmployeeDocuments({ employeeId }: { employeeId: number }) {
  const { hasPermission } = useAuth();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const confirm = useConfirm();
  const canManage = hasPermission('employee.edit');

  const [file, setFile] = useState<File | null>(null);
  const [type, setType] = useState<string>('CONTRACT');
  const [error, setError] = useState<string | null>(null);

  const { data: documents } = useQuery({
    queryKey: ['documents', employeeId],
    queryFn: () => documentService.list(employeeId),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['documents', employeeId] });

  const upload = useMutation({
    mutationFn: () => documentService.upload(employeeId, file as File, type),
    onSuccess: () => {
      setFile(null);
      setError(null);
      invalidate();
    },
    onError: (err) => {
      const apiError = err as AxiosError<{ message?: string }>;
      setError(apiError.response?.data?.message ?? t('employees.documents.uploadFailed'));
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => documentService.remove(id),
    onSuccess: invalidate,
  });

  const handleDownload = (doc: EmployeeDocument) => {
    documentService.download(doc).catch(() => setError(t('employees.documents.downloadFailed')));
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        {t('employees.documents.title')}
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {canManage && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }} alignItems={{ sm: 'center' }}>
          <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} size="small">
            {file ? file.name : t('employees.documents.chooseFile')}
            <input
              type="file"
              hidden
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </Button>
          <TextField
            select
            label={t('employees.documents.type')}
            size="small"
            value={type}
            onChange={(e) => setType(e.target.value)}
            sx={{ minWidth: 160 }}
          >
            {DOCUMENT_TYPES.map((t) => (
              <MenuItem key={t} value={t}>
                {t}
              </MenuItem>
            ))}
          </TextField>
          <Button
            variant="contained"
            size="small"
            disabled={!file || upload.isPending}
            onClick={() => upload.mutate()}
          >
            {t('employees.documents.upload')}
          </Button>
          <Typography variant="caption" color="text.secondary">
            {t('employees.documents.constraints')}
          </Typography>
        </Stack>
      )}

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('employees.documents.file')}</TableCell>
              <TableCell>{t('employees.documents.type')}</TableCell>
              <TableCell>{t('employees.documents.size')}</TableCell>
              <TableCell>{t('employees.documents.uploadedBy')}</TableCell>
              <TableCell align="right">{t('common.actions')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {documents?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                  <Typography color="text.secondary">{t('employees.documents.none')}</Typography>
                </TableCell>
              </TableRow>
            )}
            {documents?.map((doc) => (
              <TableRow key={doc.id} hover>
                <TableCell>{doc.fileName}</TableCell>
                <TableCell>{doc.documentType}</TableCell>
                <TableCell>{formatSize(doc.fileSize)}</TableCell>
                <TableCell>{doc.uploadedBy ?? '—'}</TableCell>
                <TableCell align="right">
                  <Tooltip title={t('employees.documents.download')}>
                    <IconButton size="small" onClick={() => handleDownload(doc)}>
                      <DownloadIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  {canManage && (
                    <Tooltip title={t('common.delete')}>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={async () => {
                          if (
                            await confirm({
                              title: t('employees.documents.deleteTitle'),
                              message: t('employees.documents.deleteConfirm', { name: doc.fileName }),
                              destructive: true,
                            })
                          )
                            remove.mutate(doc.id);
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Box />
    </Paper>
  );
}

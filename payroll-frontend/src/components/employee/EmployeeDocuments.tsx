import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
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

function formatSize(bytes: number | null): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function EmployeeDocuments({ employeeId }: { employeeId: number }) {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
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
      setError(apiError.response?.data?.message ?? 'Upload failed.');
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => documentService.remove(id),
    onSuccess: invalidate,
  });

  const handleDownload = (doc: EmployeeDocument) => {
    documentService.download(doc).catch(() => setError('Download failed.'));
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Documents
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {canManage && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }} alignItems={{ sm: 'center' }}>
          <Button component="label" variant="outlined" startIcon={<UploadFileIcon />} size="small">
            {file ? file.name : 'Choose file'}
            <input
              type="file"
              hidden
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </Button>
          <TextField
            select
            label="Type"
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
            Upload
          </Button>
          <Typography variant="caption" color="text.secondary">
            PDF, JPEG, PNG, DOC, DOCX · max 10MB
          </Typography>
        </Stack>
      )}

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>File</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Size</TableCell>
              <TableCell>Uploaded by</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {documents?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                  <Typography color="text.secondary">No documents.</Typography>
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
                  <Tooltip title="Download">
                    <IconButton size="small" onClick={() => handleDownload(doc)}>
                      <DownloadIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  {canManage && (
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => {
                          if (window.confirm(`Delete ${doc.fileName}?`)) remove.mutate(doc.id);
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

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Button,
  CircularProgress,
  Alert,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
  Divider,
  IconButton,
  Tooltip,
  Grid,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Restore as RestoreIcon,
  Visibility as ViewIcon,
  CompareArrows as CompareIcon,
} from '@mui/icons-material';
import Navbar from '../components/Navbar';
import versionService from '../services/versionService';

const VersionHistory = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  // View version dialog
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState(null);
  const [viewLoading, setViewLoading] = useState(false);

  // Compare dialog
  const [compareDialogOpen, setCompareDialogOpen] = useState(false);
  const [compareData, setCompareData] = useState(null);
  const [compareLoading, setCompareLoading] = useState(false);
  const [compareA, setCompareA] = useState(null);
  const [compareB, setCompareB] = useState(null);

  useEffect(() => {
    fetchVersions();
  }, [id]);

  const fetchVersions = async () => {
    try {
      const data = await versionService.getHistory(id);
      setVersions(data);
    } catch (err) {
      setError('Failed to load version history');
    } finally {
      setLoading(false);
    }
  };

  const handleViewVersion = async (versionNumber) => {
    setViewLoading(true);
    setViewDialogOpen(true);
    try {
      const data = await versionService.getVersion(id, versionNumber);
      setSelectedVersion(data);
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to load version', severity: 'error' });
      setViewDialogOpen(false);
    } finally {
      setViewLoading(false);
    }
  };

  const handleRestore = async (versionNumber) => {
    if (!window.confirm(`Restore document to version ${versionNumber}? This will create a new version with the restored content.`)) {
      return;
    }
    try {
      await versionService.restore(id, versionNumber);
      setSnackbar({ open: true, message: `Restored to version ${versionNumber}`, severity: 'success' });
      fetchVersions();
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to restore', severity: 'error' });
    }
  };

  const handleSelectForCompare = (versionNumber) => {
    if (compareA === null) {
      setCompareA(versionNumber);
      setSnackbar({ open: true, message: `Version ${versionNumber} selected. Now select another to compare.`, severity: 'info' });
    } else if (compareB === null) {
      setCompareB(versionNumber);
      performCompare(compareA, versionNumber);
    }
  };

  const performCompare = async (vA, vB) => {
    setCompareLoading(true);
    setCompareDialogOpen(true);
    try {
      const data = await versionService.compare(id, vA, vB);
      setCompareData(data);
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to compare versions', severity: 'error' });
      setCompareDialogOpen(false);
    } finally {
      setCompareLoading(false);
    }
  };

  const resetCompare = () => {
    setCompareA(null);
    setCompareB(null);
    setCompareData(null);
    setCompareDialogOpen(false);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Navbar />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Box display="flex" alignItems="center" gap={2} mb={3}>
          <IconButton onClick={() => navigate(`/documents/${id}`)}>
            <BackIcon />
          </IconButton>
          <Typography variant="h5" fontWeight={600}>
            Version History
          </Typography>
          {compareA !== null && (
            <Chip
              label={`Comparing: v${compareA} vs ...`}
              color="info"
              onDelete={resetCompare}
              size="small"
            />
          )}
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box display="flex" justifyContent="center" mt={4}>
            <CircularProgress />
          </Box>
        ) : versions.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: 'center' }}>
            <Typography variant="h6" color="text.secondary">
              No versions yet
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Versions are created each time the document is saved.
            </Typography>
          </Paper>
        ) : (
          <Paper elevation={2}>
            <List>
              {versions.map((version, index) => (
                <React.Fragment key={version.id}>
                  {index > 0 && <Divider />}
                  <ListItem
                    sx={{
                      bgcolor: compareA === version.version ? 'action.selected' : 'transparent',
                    }}
                  >
                    <ListItemText
                      primary={
                        <Box display="flex" alignItems="center" gap={1}>
                          <Chip
                            label={`v${version.version}`}
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                          <Typography variant="body1" fontWeight={500}>
                            {version.title}
                          </Typography>
                        </Box>
                      }
                      secondary={
                        <Typography variant="caption" color="text.secondary">
                          Modified by {version.modifiedByName} &middot; {formatDate(version.createdAt)}
                        </Typography>
                      }
                    />
                    <ListItemSecondaryAction>
                      <Tooltip title="View">
                        <IconButton size="small" onClick={() => handleViewVersion(version.version)}>
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Compare">
                        <IconButton size="small" onClick={() => handleSelectForCompare(version.version)}>
                          <CompareIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Restore">
                        <IconButton size="small" color="warning" onClick={() => handleRestore(version.version)}>
                          <RestoreIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </ListItemSecondaryAction>
                  </ListItem>
                </React.Fragment>
              ))}
            </List>
          </Paper>
        )}

        {/* View Version Dialog */}
        <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="md" fullWidth>
          <DialogTitle>
            {selectedVersion && `Version ${selectedVersion.version} - ${selectedVersion.title}`}
          </DialogTitle>
          <DialogContent>
            {viewLoading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : selectedVersion ? (
              <Box>
                <Typography variant="caption" color="text.secondary" gutterBottom display="block">
                  Modified by {selectedVersion.modifiedByName} on {formatDate(selectedVersion.createdAt)}
                </Typography>
                <Paper variant="outlined" sx={{ p: 2, mt: 1, maxHeight: 400, overflow: 'auto' }}>
                  <Typography
                    variant="body2"
                    sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}
                  >
                    {selectedVersion.content || '(empty)'}
                  </Typography>
                </Paper>
              </Box>
            ) : null}
          </DialogContent>
          <DialogActions>
            {selectedVersion && (
              <Button
                color="warning"
                onClick={() => {
                  setViewDialogOpen(false);
                  handleRestore(selectedVersion.version);
                }}
              >
                Restore This Version
              </Button>
            )}
            <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>

        {/* Compare Dialog */}
        <Dialog open={compareDialogOpen} onClose={resetCompare} maxWidth="lg" fullWidth>
          <DialogTitle>
            {compareData && `Comparing Version ${compareData.versionA.version} vs Version ${compareData.versionB.version}`}
          </DialogTitle>
          <DialogContent>
            {compareLoading ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : compareData ? (
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="subtitle2" gutterBottom>
                    Version {compareData.versionA.version} - {compareData.versionA.title}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                    {formatDate(compareData.versionA.createdAt)}
                  </Typography>
                  <Paper variant="outlined" sx={{ p: 2, maxHeight: 400, overflow: 'auto' }}>
                    <Typography
                      variant="body2"
                      sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '0.8rem' }}
                    >
                      {compareData.contentA || '(empty)'}
                    </Typography>
                  </Paper>
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2" gutterBottom>
                    Version {compareData.versionB.version} - {compareData.versionB.title}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="block" mb={1}>
                    {formatDate(compareData.versionB.createdAt)}
                  </Typography>
                  <Paper variant="outlined" sx={{ p: 2, maxHeight: 400, overflow: 'auto' }}>
                    <Typography
                      variant="body2"
                      sx={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: '0.8rem' }}
                    >
                      {compareData.contentB || '(empty)'}
                    </Typography>
                  </Paper>
                </Grid>
              </Grid>
            ) : null}
          </DialogContent>
          <DialogActions>
            <Button onClick={resetCompare}>Close</Button>
          </DialogActions>
        </Dialog>

        {/* Snackbar */}
        <Snackbar
          open={snackbar.open}
          autoHideDuration={3000}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
            {snackbar.message}
          </Alert>
        </Snackbar>
      </Container>
    </Box>
  );
};

export default VersionHistory;

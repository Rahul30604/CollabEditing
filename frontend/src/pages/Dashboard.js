import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  CardActions,
  Grid,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import Navbar from '../components/Navbar';
import documentService from '../services/documentService';

const Dashboard = () => {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newDocTitle, setNewDocTitle] = useState('');
  const [creating, setCreating] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteEmails, setInviteEmails] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDocuments();
  }, []);

  const fetchDocuments = async () => {
    try {
      const data = await documentService.getAll();
      setDocuments(data);
    } catch (err) {
      setError('Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!newDocTitle.trim()) return;
    setCreating(true);
    try {
      const doc = await documentService.create(newDocTitle.trim());
      // Share with invited emails
      for (const email of inviteEmails) {
        try {
          await documentService.share(doc.id, email, 'EDITOR');
        } catch (shareErr) {
          // Skip if user not found or already shared
        }
      }
      setDocuments([doc, ...documents]);
      setCreateDialogOpen(false);
      setNewDocTitle('');
      setInviteEmails([]);
      setInviteEmail('');
      navigate(`/documents/${doc.id}`);
    } catch (err) {
      setError('Failed to create document');
    } finally {
      setCreating(false);
    }
  };

  const handleAddInvite = () => {
    const email = inviteEmail.trim();
    if (email && !inviteEmails.includes(email)) {
      setInviteEmails([...inviteEmails, email]);
      setInviteEmail('');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this document?')) return;
    try {
      await documentService.delete(id);
      setDocuments(documents.filter((d) => d.id !== id));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete document');
    }
  };

  const getRoleChipColor = (role) => {
    switch (role) {
      case 'OWNER':
        return 'primary';
      case 'EDITOR':
        return 'success';
      case 'VIEWER':
        return 'default';
      default:
        return 'default';
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Navbar />
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" fontWeight={600}>
            My Documents
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateDialogOpen(true)}
          >
            New Document
          </Button>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box display="flex" justifyContent="center" mt={4}>
            <CircularProgress />
          </Box>
        ) : documents.length === 0 ? (
          <Box textAlign="center" mt={6}>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No documents yet
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Create your first document to get started
            </Typography>
            <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setCreateDialogOpen(true)}>
              Create Document
            </Button>
          </Box>
        ) : (
          <Grid container spacing={3}>
            {documents.map((doc) => (
              <Grid item xs={12} sm={6} md={4} key={doc.id}>
                <Card
                  sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    cursor: 'pointer',
                    transition: 'box-shadow 0.2s',
                    '&:hover': { boxShadow: 4 },
                  }}
                  onClick={() => navigate(`/documents/${doc.id}`)}
                >
                  <CardContent sx={{ flexGrow: 1 }}>
                    <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1}>
                      <Typography variant="h6" noWrap sx={{ maxWidth: '70%' }}>
                        {doc.title}
                      </Typography>
                      <Chip
                        label={doc.currentUserRole}
                        size="small"
                        color={getRoleChipColor(doc.currentUserRole)}
                      />
                    </Box>
                    <Typography variant="body2" color="text.secondary">
                      by {doc.ownerName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Updated {formatDate(doc.updatedAt)}
                    </Typography>
                  </CardContent>
                  <CardActions sx={{ justifyContent: 'flex-end', pt: 0 }}>
                    {doc.currentUserRole === 'VIEWER' ? (
                      <IconButton size="small" onClick={(e) => { e.stopPropagation(); navigate(`/documents/${doc.id}`); }}>
                        <ViewIcon fontSize="small" />
                      </IconButton>
                    ) : (
                      <IconButton size="small" onClick={(e) => { e.stopPropagation(); navigate(`/documents/${doc.id}`); }}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    )}
                    {doc.currentUserRole === 'OWNER' && (
                      <IconButton
                        size="small"
                        color="error"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(doc.id);
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    )}
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}

        {/* Create Document Dialog */}
        <Dialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>Create New Document</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              label="Document Title"
              fullWidth
              margin="normal"
              value={newDocTitle}
              onChange={(e) => setNewDocTitle(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && !inviteEmails.length && handleCreate()}
            />
            <Typography variant="subtitle2" color="text.secondary" mt={2} mb={1}>
              Invite Collaborators (optional)
            </Typography>
            <Box display="flex" gap={1} mb={1}>
              <TextField
                label="Email address"
                type="email"
                size="small"
                fullWidth
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleAddInvite()}
                placeholder="colleague@example.com"
              />
              <Button size="small" variant="outlined" onClick={handleAddInvite} disabled={!inviteEmail.trim()}>
                Add
              </Button>
            </Box>
            {inviteEmails.length > 0 && (
              <Box display="flex" flexWrap="wrap" gap={0.5}>
                {inviteEmails.map((email, idx) => (
                  <Chip
                    key={idx}
                    label={email}
                    size="small"
                    onDelete={() => setInviteEmails(inviteEmails.filter((_, i) => i !== idx))}
                  />
                ))}
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { setCreateDialogOpen(false); setInviteEmails([]); setInviteEmail(''); }}>Cancel</Button>
            <Button
              variant="contained"
              onClick={handleCreate}
              disabled={creating || !newDocTitle.trim()}
            >
              {creating ? 'Creating...' : 'Create'}
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </Box>
  );
};

export default Dashboard;

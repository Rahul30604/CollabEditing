import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  TextField,
  IconButton,
  Chip,
  CircularProgress,
  Alert,
  Snackbar,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Tooltip,
  Avatar,
  AvatarGroup,
  Divider,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Save as SaveIcon,
  Share as ShareIcon,
  Delete as DeleteIcon,
  Lock as LockIcon,
  LockOpen as LockOpenIcon,
  History as HistoryIcon,
  AutoAwesome as AiIcon,
} from '@mui/icons-material';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import documentService from '../services/documentService';
import editingKeyService from '../services/editingKeyService';
import websocketService from '../services/websocketService';

const Editor = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [document, setDocument] = useState(null);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [hasChanges, setHasChanges] = useState(false);

  // Key-based editing state
  const [keyHolder, setKeyHolder] = useState(null);
  const [keyHolderName, setKeyHolderName] = useState(null);
  const [hasKey, setHasKey] = useState(false);
  const [requestingKey, setRequestingKey] = useState(false);
  const [queuePosition, setQueuePosition] = useState(0);

  // Presence state
  const [activeUsers, setActiveUsers] = useState([]);

  // Share dialog state
  const [shareDialogOpen, setShareDialogOpen] = useState(false);

  // Ref to debounce live broadcast
  const broadcastTimerRef = useRef(null);
  const isRemoteUpdateRef = useRef(false);

  // Key request notification state
  const [keyRequestNotification, setKeyRequestNotification] = useState(null);

  const [shareEmail, setShareEmail] = useState('');
  const [shareRole, setShareRole] = useState('EDITOR');
  const [permissions, setPermissions] = useState([]);
  const [sharing, setSharing] = useState(false);

  const isReadOnly = document?.currentUserRole === 'VIEWER';
  const isOwner = document?.currentUserRole === 'OWNER';
  const canEdit = hasKey && !isReadOnly;

  // Tutorial bot state
  const [tutorialActive, setTutorialActive] = useState(false);
  const [tutorialKeyRequested, setTutorialKeyRequested] = useState(false);
  const [tutorialPhase, setTutorialPhase] = useState('init'); // init, tips, done
  const tutorialReminderRef = useRef(null);

  useEffect(() => {
    fetchDocument();
    fetchKeyStatus();

    // Connect WebSocket
    const token = localStorage.getItem('token');
    websocketService.connect(token);

    // Subscribe to real-time updates after connection
    websocketService.onConnect(() => {
      websocketService.subscribeToEdits(id, handleRemoteEdit);
      websocketService.subscribeToKeyChanges(id, handleKeyChange);
      websocketService.subscribeToKeyRequests(id, handleKeyRequest);
      websocketService.subscribeToPresence(id, handlePresenceChange);
      websocketService.sendPresence(id, 'JOINED');
    });

    return () => {
      websocketService.sendPresence(id, 'LEFT');
      websocketService.unsubscribeFromDocument(id);
      if (broadcastTimerRef.current) clearTimeout(broadcastTimerRef.current);
    };
  }, [id]);

  // Tutorial reminder every 20 seconds
  useEffect(() => {
    if (tutorialActive && tutorialPhase === 'init') {
      tutorialReminderRef.current = setInterval(() => {
        setContent((prev) => {
          if (prev.includes('Reminder:')) return prev; // Don't stack reminders
          return prev + `\n\n--- Reminder: If you want to edit, click "Request Key" on the top right corner ---`;
        });
      }, 20000);

      return () => {
        if (tutorialReminderRef.current) clearInterval(tutorialReminderRef.current);
      };
    }
  }, [tutorialActive, tutorialPhase]);

  const fetchDocument = async () => {
    try {
      const data = await documentService.getById(id);
      setDocument(data);

      // Check for a draft in sessionStorage
      const draftKey = `draft_${id}`;
      const draft = sessionStorage.getItem(draftKey);
      if (draft) {
        const { title: draftTitle, content: draftContent } = JSON.parse(draft);
        setTitle(draftTitle);
        setContent(draftContent);
        setHasChanges(true);
      } else {
        setTitle(data.title);
        setContent(data.content || '');
      }

      // Activate tutorial bot for new empty documents owned by guest users
      const isGuest = user?.name?.startsWith('Guest_');
      const isEmpty = !data.content || data.content.trim() === '';
      const hasDraft = !!draft;
      if (isGuest && isEmpty && !hasDraft && data.currentUserRole === 'OWNER') {
        setTutorialActive(true);
        setTutorialPhase('init');
        setContent(`Welcome to CollabEditor!

To edit this document, click "Request Key" on the top right corner.

Only one person can edit at a time — this prevents conflicts in real-time collaboration.`);
      }
    } catch (err) {
      setError('Failed to load document');
    } finally {
      setLoading(false);
    }
  };

  const fetchKeyStatus = async () => {
    try {
      const status = await editingKeyService.getStatus(id);
      setKeyHolder(status.holderId);
      setKeyHolderName(status.holderName);
      setHasKey(status.granted);
      setQueuePosition(status.queuePosition);
    } catch (err) {
      // Key status not critical
    }
  };

  // WebSocket handlers
  const handleRemoteEdit = useCallback((message) => {
    if (message.userId !== user?.id) {
      isRemoteUpdateRef.current = true;
      setContent(message.content || '');
      if (message.title) setTitle(message.title);
      // Reset flag after state update
      setTimeout(() => { isRemoteUpdateRef.current = false; }, 0);
    }
  }, [user]);

  const handleKeyChange = useCallback((status) => {
    setKeyHolder(status.holderId);
    setKeyHolderName(status.holderName);
    setHasKey(status.holderId === user?.id);
    setQueuePosition(status.queuePosition || 0);

    if (status.holderId === user?.id) {
      setSnackbar({ open: true, message: 'You now have the editing key!', severity: 'success' });
    }
  }, [user]);

  const handlePresenceChange = useCallback((message) => {
    if (message.action === 'JOINED') {
      setActiveUsers((prev) => {
        if (prev.find((u) => u.userId === message.userId)) return prev;
        return [...prev, { userId: message.userId, userName: message.userName }];
      });
    } else if (message.action === 'LEFT') {
      setActiveUsers((prev) => prev.filter((u) => u.userId !== message.userId));
    }
  }, []);

  // Handle incoming key request notification (shown to key holder)
  const handleKeyRequest = useCallback((message) => {
    if (message.type === 'KEY_DECLINED') {
      // I was the requester and got declined — reset queue state
      setQueuePosition(0);
      setSnackbar({ open: true, message: 'Your key request was declined', severity: 'warning' });
      return;
    }
    if (message.requesterId !== user?.id) {
      setKeyRequestNotification(message);
    }
  }, [user]);

  // Pass the key to the requester
  const handlePassKey = async () => {
    try {
      await editingKeyService.releaseKey(id);
      setHasKey(false);
      setKeyRequestNotification(null);
      setSnackbar({ open: true, message: 'Key passed', severity: 'info' });
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to pass key', severity: 'error' });
    }
  };

  // Dismiss / decline the key request
  const handleDismissRequest = async () => {
    if (keyRequestNotification?.requesterId) {
      try {
        await editingKeyService.declineRequest(id, keyRequestNotification.requesterId);
      } catch (err) {
        // Ignore errors on decline
      }
    }
    setKeyRequestNotification(null);
  };

  // Key actions
  const handleRequestKey = async () => {
    // If tutorial bot is active, show tips in the editor
    if (tutorialActive && tutorialPhase === 'init') {
      setTutorialPhase('tips');
      setTutorialKeyRequested(true);

      // Stop reminders
      if (tutorialReminderRef.current) clearInterval(tutorialReminderRef.current);

      // Show onboarding tips in the editor content
      setContent(`Here's what you can do in CollabEditor:

1. Edit Document — Type anything, your changes sync in real-time to all collaborators
2. Save — Press Cmd/Ctrl+S to save (creates a version snapshot)
3. Version History — Click the clock icon to view, compare, or restore past versions
4. AI Assistant — Click the sparkle icon to ask questions about this document

   Example: Below is some sample data in this document:

   Student Results:
   | Name    | Maths | Science | English |
   | Alice   |  85   |   92    |   78    |
   | Bob     |  72   |   68    |   81    |
   | Charlie |  91   |   88    |   95    |
   | Diana   |  63   |   75    |   70    |

   Now click the sparkle icon and ask: "What is the average maths score?"
   The AI will read this document and respond: "The average maths score is 77.75"

   Try it! Ask anything about the content in this document.

5. Summarize — Get an AI-generated summary of the entire document
6. Semantic Search — Use "Search" in the navbar to find content across all your documents
7. Share — Click "+ Add People" to invite collaborators by email (Editor or Viewer role)
8. Key-Based Editing — Only one person edits at a time, others see changes live`);

      // After 5 seconds, clear content and give key
      setTimeout(() => {
        handleTutorialComplete();
      }, 5000);
      return;
    }

    setRequestingKey(true);
    try {
      const result = await editingKeyService.requestKey(id);
      setHasKey(result.granted);
      setKeyHolder(result.holderId);
      setKeyHolderName(result.holderName);
      setQueuePosition(result.queuePosition);

      if (result.granted) {
        setSnackbar({ open: true, message: 'Editing key acquired!', severity: 'success' });
      } else {
        setSnackbar({ open: true, message: result.message, severity: 'info' });
      }
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to request key', severity: 'error' });
    } finally {
      setRequestingKey(false);
    }
  };

  // Called when tutorial bot finishes onboarding and passes the key
  const handleTutorialComplete = async () => {
    setTutorialActive(false);
    setTutorialKeyRequested(false);
    setTutorialPhase('done');
    // Keep the tips content — don't clear it
    setHasChanges(false);

    // Actually acquire the key now
    try {
      const result = await editingKeyService.requestKey(id);
      setHasKey(result.granted);
      setKeyHolder(result.holderId);
      setKeyHolderName(result.holderName);
      if (result.granted) {
        setSnackbar({ open: true, message: 'Key received! You can now edit the document.', severity: 'success' });
      }
    } catch (err) {
      setHasKey(true); // Fallback: let them edit anyway
    }
  };

  const handleReleaseKey = async () => {
    try {
      await editingKeyService.releaseKey(id);
      setHasKey(false);
      setSnackbar({ open: true, message: 'Editing key released', severity: 'info' });
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to release key', severity: 'error' });
    }
  };

  // Save
  const handleSave = useCallback(async () => {
    if (!canEdit || !hasChanges) return;
    setSaving(true);
    try {
      const updated = await documentService.update(id, { title, content });
      setDocument(updated);
      setHasChanges(false);
      sessionStorage.removeItem(`draft_${id}`);

      setSnackbar({ open: true, message: 'Document saved', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to save', severity: 'error' });
    } finally {
      setSaving(false);
    }
  }, [id, title, content, canEdit, hasChanges]);

  // Keyboard shortcut
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 's') {
        e.preventDefault();
        handleSave();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleSave]);

  const handleTitleChange = (e) => {
    setTitle(e.target.value);
    setHasChanges(true);
    broadcastLive(content, e.target.value);
    saveDraft(content, e.target.value);
  };

  const handleContentChange = (e) => {
    setContent(e.target.value);
    setHasChanges(true);
    broadcastLive(e.target.value, title);
    saveDraft(e.target.value, title);
  };

  // Save draft to sessionStorage
  const saveDraft = (draftContent, draftTitle) => {
    sessionStorage.setItem(`draft_${id}`, JSON.stringify({ title: draftTitle, content: draftContent }));
  };

  // Auto-save draft whenever content or title changes (covers bot updates + typing)
  useEffect(() => {
    if (!loading && document) {
      sessionStorage.setItem(`draft_${id}`, JSON.stringify({ title, content }));
    }
  }, [content, title, id, loading, document]);

  // Broadcast edits live with 100ms debounce
  const broadcastLive = (newContent, newTitle) => {
    if (isRemoteUpdateRef.current) return; // Don't echo back remote changes
    if (broadcastTimerRef.current) {
      clearTimeout(broadcastTimerRef.current);
    }
    broadcastTimerRef.current = setTimeout(() => {
      websocketService.sendEdit(id, newContent, newTitle);
    }, 100);
  };

  // Share functionality
  const handleOpenShare = async () => {
    setShareDialogOpen(true);
    try {
      const perms = await documentService.getPermissions(id);
      setPermissions(perms);
    } catch (err) {
      setPermissions([]);
    }
  };

  const handleShare = async () => {
    if (!shareEmail.trim()) return;
    setSharing(true);
    try {
      await documentService.share(id, shareEmail.trim(), shareRole);
      const perms = await documentService.getPermissions(id);
      setPermissions(perms);
      setShareEmail('');
      setSnackbar({ open: true, message: 'Document shared successfully', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.message || 'Failed to share', severity: 'error' });
    } finally {
      setSharing(false);
    }
  };

  const handleRemovePermission = async (userId) => {
    try {
      await documentService.removePermission(id, userId);
      setPermissions(permissions.filter((p) => p.userId !== userId));
      setSnackbar({ open: true, message: 'Permission removed', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: 'Failed to remove permission', severity: 'error' });
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
        <Navbar />
        <Box display="flex" justifyContent="center" mt={4}>
          <Alert severity="error">{error}</Alert>
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', display: 'flex', flexDirection: 'column' }}>
      <Navbar />

      {/* Editor Toolbar */}
      <Paper elevation={1} sx={{ px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 1, borderRadius: 0, flexWrap: 'wrap' }}>
        <Tooltip title="Back to Dashboard">
          <IconButton onClick={() => navigate('/dashboard')} size="small">
            <BackIcon />
          </IconButton>
        </Tooltip>

        <TextField
          value={title}
          onChange={handleTitleChange}
          variant="standard"
          placeholder="Untitled Document"
          disabled={false}
          InputProps={{ readOnly: !canEdit, disableUnderline: true, sx: { fontSize: '1.1rem', fontWeight: 500 } }}
          sx={{ flexGrow: 1, maxWidth: 300 }}
        />

        <Chip
          label={document?.currentUserRole}
          size="small"
          color={isOwner ? 'primary' : isReadOnly ? 'default' : 'success'}
        />

        {/* Key status indicator */}
        {!isReadOnly && (
          <>
            {hasKey ? (
              <Chip
                icon={<LockIcon />}
                label="Editing"
                size="small"
                color="success"
                variant="outlined"
                onDelete={handleReleaseKey}
                deleteIcon={<LockOpenIcon />}
              />
            ) : keyHolder ? (
              <Chip
                icon={<LockIcon />}
                label={`Locked by ${keyHolderName}`}
                size="small"
                color="warning"
                variant="outlined"
              />
            ) : (
              <Chip
                icon={<LockOpenIcon />}
                label="Available"
                size="small"
                color="default"
                variant="outlined"
              />
            )}
          </>
        )}

        {hasChanges && (
          <Chip label="Unsaved" size="small" variant="outlined" color="warning" />
        )}

        {/* Active users */}
        {activeUsers.length > 0 && (
          <AvatarGroup max={4} sx={{ '& .MuiAvatar-root': { width: 28, height: 28, fontSize: 12 } }}>
            {activeUsers.map((u) => (
              <Tooltip key={u.userId} title={u.userName}>
                <Avatar sx={{ bgcolor: 'primary.main' }}>
                  {u.userName?.charAt(0)?.toUpperCase()}
                </Avatar>
              </Tooltip>
            ))}
          </AvatarGroup>
        )}

        <Box sx={{ flexGrow: 1 }} />

        {/* Request/Release key button */}
        {!isReadOnly && !hasKey && (
          <Button
            variant="outlined"
            size="small"
            startIcon={<LockIcon />}
            onClick={handleRequestKey}
            disabled={requestingKey}
          >
            {requestingKey ? 'Requesting...' : queuePosition > 0 ? `In Queue (${queuePosition})` : 'Request Key'}
          </Button>
        )}

        <Tooltip title="Version History">
          <IconButton onClick={() => navigate(`/documents/${id}/history`)} size="small">
            <HistoryIcon />
          </IconButton>
        </Tooltip>

        <Tooltip title="AI Assistant">
          <IconButton onClick={() => navigate(`/documents/${id}/ai`)} size="small" color="primary">
            <AiIcon />
          </IconButton>
        </Tooltip>

        {isOwner && (
          <Button
            variant="outlined"
            size="small"
            startIcon={<ShareIcon />}
            onClick={handleOpenShare}
          >
            + Add People
          </Button>
        )}

        {canEdit && (
          <Button
            variant="contained"
            size="small"
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveIcon />}
            onClick={handleSave}
            disabled={saving || !hasChanges}
          >
            {saving ? 'Saving...' : 'Save'}
          </Button>
        )}
      </Paper>

      {/* Editor Content */}
      <Box sx={{ flexGrow: 1, p: 3, display: 'flex', justifyContent: 'center', position: 'relative' }}>
        <Paper
          elevation={2}
          sx={{
            width: '100%',
            maxWidth: 900,
            p: 4,
            minHeight: 'calc(100vh - 200px)',
            border: canEdit ? '2px solid #4caf50' : hasKey === false && !isReadOnly ? '2px solid #ff9800' : '2px solid transparent',
            transition: 'border-color 0.3s',
          }}
        >
          {canEdit && (
            <Typography variant="caption" sx={{ color: '#4caf50', fontWeight: 600, mb: 1, display: 'block' }}>
              You are editing
            </Typography>
          )}
          {!canEdit && !isReadOnly && keyHolder && !tutorialActive && (
            <Typography variant="caption" sx={{ color: '#ff9800', fontWeight: 600, mb: 1, display: 'block' }}>
              Viewing only &mdash; {keyHolderName} is editing
            </Typography>
          )}
          <TextField
            multiline
            fullWidth
            value={content}
            onChange={handleContentChange}
            placeholder={
              !canEdit
                ? isReadOnly
                  ? 'This document is read-only'
                  : 'Request the editing key to make changes'
                : 'Start typing your document here...'
            }
            disabled={false}
            variant="standard"
            InputProps={{
              readOnly: !canEdit,
              disableUnderline: true,
              sx: { fontSize: '1rem', lineHeight: 1.8, color: 'text.primary' },
            }}
            sx={{ '& .MuiInputBase-root': { alignItems: 'flex-start' } }}
          />
        </Paper>

        {/* Key Request Notification - fixed to right side */}
        {keyRequestNotification && hasKey && (
          <Paper
            elevation={4}
            sx={{
              position: 'fixed',
              top: 120,
              right: 24,
              p: 2,
              width: 280,
              bgcolor: '#fff3e0',
              border: '1px solid #ff9800',
              borderRadius: 2,
              zIndex: 1000,
            }}
          >
            <Typography variant="subtitle2" fontWeight={600} gutterBottom>
              Key Requested
            </Typography>
            <Typography variant="body2" mb={2}>
              <strong>{keyRequestNotification.requesterName}</strong> is requesting the editing key.
            </Typography>
            <Box display="flex" gap={1}>
              <Button
                variant="contained"
                size="small"
                color="warning"
                onClick={handlePassKey}
                fullWidth
              >
                Pass Key
              </Button>
              <Button
                variant="outlined"
                size="small"
                onClick={handleDismissRequest}
                fullWidth
              >
                Dismiss
              </Button>
            </Box>
          </Paper>
        )}
      </Box>

      {/* Share Dialog */}
      <Dialog open={shareDialogOpen} onClose={() => setShareDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add People to Document</DialogTitle>
        <DialogContent>
          <Box display="flex" gap={1} mt={1} mb={2}>
            <TextField
              label="Email address"
              type="email"
              size="small"
              fullWidth
              value={shareEmail}
              onChange={(e) => setShareEmail(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleShare()}
            />
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Role</InputLabel>
              <Select value={shareRole} onChange={(e) => setShareRole(e.target.value)} label="Role">
                <MenuItem value="EDITOR">Editor</MenuItem>
                <MenuItem value="VIEWER">Viewer</MenuItem>
              </Select>
            </FormControl>
            <Button variant="contained" onClick={handleShare} disabled={sharing || !shareEmail.trim()}>
              Share
            </Button>
          </Box>

          {permissions.length > 0 && (
            <>
              <Typography variant="subtitle2" color="text.secondary" mb={1}>
                People with access
              </Typography>
              <List dense>
                {permissions.map((perm) => (
                  <ListItem key={perm.id} divider>
                    <ListItemText
                      primary={perm.userName}
                      secondary={perm.userEmail}
                    />
                    <Chip label={perm.role} size="small" sx={{ mr: 1 }} />
                    {perm.role !== 'OWNER' && (
                      <ListItemSecondaryAction>
                        <IconButton
                          edge="end"
                          size="small"
                          onClick={() => handleRemovePermission(perm.userId)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </ListItemSecondaryAction>
                    )}
                  </ListItem>
                ))}
              </List>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShareDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default Editor;

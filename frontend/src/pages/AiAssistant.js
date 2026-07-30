import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  TextField,
  Button,
  Paper,
  CircularProgress,
  Alert,
  Chip,
  IconButton,
  Divider,
  List,
  ListItem,
  ListItemText,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  Send as SendIcon,
  Summarize as SummarizeIcon,
  AutoAwesome as AiIcon,
} from '@mui/icons-material';
import Navbar from '../components/Navbar';
import aiService from '../services/aiService';

const AiAssistant = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [error, setError] = useState('');

  const handleAsk = async () => {
    if (!question.trim()) return;
    const currentQuestion = question.trim();
    setQuestion('');
    setLoading(true);
    setError('');

    setConversations((prev) => [
      ...prev,
      { type: 'question', text: currentQuestion },
    ]);

    try {
      const response = await aiService.askQuestion(id, currentQuestion);
      setConversations((prev) => [
        ...prev,
        { type: 'answer', text: response.answer, sources: response.sources },
      ]);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to get AI response');
      setConversations((prev) => prev.slice(0, -1)); // Remove the question on error
    } finally {
      setLoading(false);
    }
  };

  const handleSummarize = async () => {
    setLoading(true);
    setError('');

    setConversations((prev) => [
      ...prev,
      { type: 'question', text: 'Summarize this document' },
    ]);

    try {
      const response = await aiService.summarize(id);
      setConversations((prev) => [
        ...prev,
        { type: 'answer', text: response.answer, sources: [] },
      ]);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to summarize');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Navbar />
      <Container maxWidth="md" sx={{ py: 4 }}>
        {/* Header */}
        <Box display="flex" alignItems="center" gap={2} mb={3}>
          <IconButton onClick={() => navigate(`/documents/${id}`)}>
            <BackIcon />
          </IconButton>
          <AiIcon color="primary" />
          <Typography variant="h5" fontWeight={600}>
            AI Assistant
          </Typography>
        </Box>

        {/* Quick Actions */}
        <Box display="flex" gap={1} mb={3}>
          <Button
            variant="outlined"
            startIcon={<SummarizeIcon />}
            onClick={handleSummarize}
            disabled={loading}
            size="small"
          >
            Summarize Document
          </Button>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        {/* Conversation */}
        <Paper elevation={2} sx={{ minHeight: 400, maxHeight: 500, overflow: 'auto', p: 2, mb: 2 }}>
          {conversations.length === 0 ? (
            <Box
              display="flex"
              flexDirection="column"
              alignItems="center"
              justifyContent="center"
              height={300}
              color="text.secondary"
            >
              <AiIcon sx={{ fontSize: 48, mb: 2, opacity: 0.5 }} />
              <Typography variant="body1">
                Ask questions about this document
              </Typography>
              <Typography variant="body2" color="text.secondary">
                The AI uses the document content to provide accurate answers.
              </Typography>
            </Box>
          ) : (
            <List disablePadding>
              {conversations.map((msg, index) => (
                <ListItem
                  key={index}
                  sx={{
                    flexDirection: 'column',
                    alignItems: msg.type === 'question' ? 'flex-end' : 'flex-start',
                    py: 1,
                  }}
                >
                  <Chip
                    label={msg.type === 'question' ? 'You' : 'AI'}
                    size="small"
                    color={msg.type === 'question' ? 'primary' : 'secondary'}
                    sx={{ mb: 0.5 }}
                  />
                  <Paper
                    variant="outlined"
                    sx={{
                      p: 2,
                      maxWidth: '85%',
                      bgcolor: msg.type === 'question' ? 'primary.50' : 'grey.50',
                    }}
                  >
                    <Typography
                      variant="body2"
                      sx={{ whiteSpace: 'pre-wrap' }}
                    >
                      {msg.text}
                    </Typography>
                    {msg.sources && msg.sources.length > 0 && (
                      <>
                        <Divider sx={{ my: 1 }} />
                        <Typography variant="caption" color="text.secondary" display="block" mb={0.5}>
                          Sources:
                        </Typography>
                        {msg.sources.map((source, i) => (
                          <Typography
                            key={i}
                            variant="caption"
                            color="text.secondary"
                            display="block"
                            sx={{ fontStyle: 'italic', pl: 1 }}
                          >
                            {i + 1}. {source}
                          </Typography>
                        ))}
                      </>
                    )}
                  </Paper>
                </ListItem>
              ))}
              {loading && (
                <ListItem sx={{ justifyContent: 'flex-start' }}>
                  <CircularProgress size={24} />
                  <Typography variant="body2" color="text.secondary" ml={1}>
                    Thinking...
                  </Typography>
                </ListItem>
              )}
            </List>
          )}
        </Paper>

        {/* Input */}
        <Box display="flex" gap={1}>
          <TextField
            fullWidth
            placeholder="Ask a question about this document..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && !e.shiftKey && handleAsk()}
            disabled={loading}
            size="small"
            multiline
            maxRows={3}
          />
          <Button
            variant="contained"
            onClick={handleAsk}
            disabled={loading || !question.trim()}
            sx={{ minWidth: 50 }}
          >
            <SendIcon />
          </Button>
        </Box>
      </Container>
    </Box>
  );
};

export default AiAssistant;

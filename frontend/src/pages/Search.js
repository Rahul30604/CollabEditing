import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  TextField,
  Button,
  Paper,
  CircularProgress,
  Alert,
  List,
  ListItem,
  ListItemText,
  Chip,
  InputAdornment,
  LinearProgress,
} from '@mui/material';
import {
  Search as SearchIcon,
  Description as DocIcon,
} from '@mui/icons-material';
import Navbar from '../components/Navbar';
import aiService from '../services/aiService';

const Search = () => {
  const navigate = useNavigate();

  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState('');

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError('');
    setSearched(true);

    try {
      const data = await aiService.search(query.trim());
      setResults(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Search failed');
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const getRelevanceColor = (score) => {
    if (score >= 0.8) return 'success';
    if (score >= 0.6) return 'primary';
    if (score >= 0.4) return 'warning';
    return 'default';
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Navbar />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Typography variant="h5" fontWeight={600} mb={3}>
          Semantic Search
        </Typography>
        <Typography variant="body2" color="text.secondary" mb={3}>
          Search across all your documents using natural language. Results are ranked by semantic relevance.
        </Typography>

        {/* Search Input */}
        <Paper elevation={2} sx={{ p: 2, mb: 3 }}>
          <Box display="flex" gap={1}>
            <TextField
              fullWidth
              placeholder="e.g. Explain RabbitMQ, Find deployment steps, Show authentication logic..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              disabled={loading}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />
            <Button
              variant="contained"
              onClick={handleSearch}
              disabled={loading || !query.trim()}
              sx={{ minWidth: 100 }}
            >
              {loading ? <CircularProgress size={24} color="inherit" /> : 'Search'}
            </Button>
          </Box>
          {loading && <LinearProgress sx={{ mt: 1 }} />}
        </Paper>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}

        {/* Results */}
        {searched && !loading && results.length === 0 && (
          <Paper sx={{ p: 4, textAlign: 'center' }}>
            <Typography variant="h6" color="text.secondary">
              No results found
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Try a different search query or make sure your documents have been saved.
            </Typography>
          </Paper>
        )}

        {results.length > 0 && (
          <Paper elevation={2}>
            <Box px={2} pt={2}>
              <Typography variant="subtitle2" color="text.secondary">
                {results.length} result{results.length !== 1 ? 's' : ''} found
              </Typography>
            </Box>
            <List>
              {results.map((result, index) => (
                <ListItem
                  key={index}
                  divider={index < results.length - 1}
                  sx={{
                    cursor: 'pointer',
                    '&:hover': { bgcolor: 'action.hover' },
                    alignItems: 'flex-start',
                  }}
                  onClick={() => navigate(`/documents/${result.documentId}`)}
                >
                  <DocIcon sx={{ mt: 1, mr: 2, color: 'primary.main' }} />
                  <ListItemText
                    primary={
                      <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                        <Typography variant="subtitle1" fontWeight={500}>
                          {result.title}
                        </Typography>
                        <Chip
                          label={`${Math.round(result.relevanceScore * 100)}% match`}
                          size="small"
                          color={getRelevanceColor(result.relevanceScore)}
                          variant="outlined"
                        />
                      </Box>
                    }
                    secondary={
                      <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          display: '-webkit-box',
                          WebkitLineClamp: 3,
                          WebkitBoxOrient: 'vertical',
                        }}
                      >
                        {result.snippet}
                      </Typography>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </Paper>
        )}
      </Container>
    </Box>
  );
};

export default Search;

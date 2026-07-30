import api from './api';

const aiService = {
  askQuestion: async (documentId, question) => {
    const response = await api.post(`/documents/${documentId}/ask`, { question });
    return response.data;
  },

  summarize: async (documentId) => {
    const response = await api.post(`/documents/${documentId}/summary`);
    return response.data;
  },

  search: async (query) => {
    const response = await api.post('/search', { query });
    return response.data;
  },
};

export default aiService;

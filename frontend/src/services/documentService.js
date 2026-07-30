import api from './api';

const documentService = {
  getAll: async () => {
    const response = await api.get('/documents');
    return response.data;
  },

  getById: async (id) => {
    const response = await api.get(`/documents/${id}`);
    return response.data;
  },

  create: async (title, content = '') => {
    const response = await api.post('/documents', { title, content });
    return response.data;
  },

  update: async (id, data) => {
    const response = await api.put(`/documents/${id}`, data);
    return response.data;
  },

  delete: async (id) => {
    await api.delete(`/documents/${id}`);
  },

  share: async (documentId, email, role) => {
    const response = await api.post(`/documents/${documentId}/share`, { email, role });
    return response.data;
  },

  getPermissions: async (documentId) => {
    const response = await api.get(`/documents/${documentId}/share`);
    return response.data;
  },

  removePermission: async (documentId, userId) => {
    await api.delete(`/documents/${documentId}/share/${userId}`);
  },
};

export default documentService;

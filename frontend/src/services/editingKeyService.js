import api from './api';

const editingKeyService = {
  requestKey: async (documentId) => {
    const response = await api.post(`/documents/${documentId}/key/request`);
    return response.data;
  },

  releaseKey: async (documentId) => {
    const response = await api.post(`/documents/${documentId}/key/release`);
    return response.data;
  },

  declineRequest: async (documentId, requesterId) => {
    const response = await api.post(`/documents/${documentId}/key/decline/${requesterId}`);
    return response.data;
  },

  getStatus: async (documentId) => {
    const response = await api.get(`/documents/${documentId}/key/status`);
    return response.data;
  },
};

export default editingKeyService;

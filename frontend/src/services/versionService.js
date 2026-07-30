import api from './api';

const versionService = {
  getHistory: async (documentId) => {
    const response = await api.get(`/documents/${documentId}/versions`);
    return response.data;
  },

  getVersion: async (documentId, version) => {
    const response = await api.get(`/documents/${documentId}/versions/${version}`);
    return response.data;
  },

  restore: async (documentId, version) => {
    const response = await api.post(`/documents/${documentId}/versions/${version}/restore`);
    return response.data;
  },

  compare: async (documentId, versionA, versionB) => {
    const response = await api.get(`/documents/${documentId}/versions/compare`, {
      params: { versionA, versionB },
    });
    return response.data;
  },
};

export default versionService;

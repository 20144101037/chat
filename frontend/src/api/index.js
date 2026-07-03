import request from './request';

export const authApi = {
  register: (data) => request.post('/auth/register', data),
  login: (data) => request.post('/auth/login', data),
};

export const roomApi = {
  page: (params) => request.get('/rooms', { params }),
  detail: (id) => request.get(`/rooms/${id}`),
  create: (data) => request.post('/rooms', data),
  update: (id, data) => request.put(`/rooms/${id}`, data),
  remove: (id) => request.delete(`/rooms/${id}`),
  changeStatus: (id, status) => request.patch(`/rooms/${id}/status`, { status }),
  broadcast: (data) => request.post('/rooms/broadcast', data),
  systemNotify: (id, content) => request.post(`/rooms/${id}/system-notify`, { content }),
  join: (id) => request.post(`/rooms/${id}/join`),
  leave: (id) => request.post(`/rooms/${id}/leave`),
  approveMember: (id, userId, pass) => request.post(`/rooms/${id}/members/${userId}/approve`, { pass }),
  members: (id, status) => request.get(`/rooms/${id}/members`, { params: { status } }),
  myRooms: () => request.get('/rooms/mine'),
};

export const messageApi = {
  submit: (roomId, content) => request.post(`/rooms/${roomId}/messages`, { content }),
  history: (roomId, params) => request.get(`/rooms/${roomId}/messages`, { params }),
  myMessages: (params) => request.get('/me/messages', { params }),
};

export const auditApi = {
  pending: (params) => request.get('/audit/pending', { params }),
  approve: (messageId) => request.post(`/audit/${messageId}/approve`),
  reject: (messageId, reason) => request.post(`/audit/${messageId}/reject`, { reason }),
  batch: (data) => request.post('/audit/batch', data),
};

export const configApi = {
  page: (params) => request.get('/configs', { params }),
  create: (data) => request.post('/configs', data),
  update: (id, data) => request.put(`/configs/${id}`, data),
  remove: (id) => request.delete(`/configs/${id}`),
};

export const metricsApi = {
  dashboard: () => request.get('/metrics/dashboard'),
};

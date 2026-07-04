import request from './request';

export const authApi = {
  register: (data) => request.post('/auth/register', data),
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
};

export const userApi = {
  meMenus: () => request.get('/me/menus'),
};

export const adminApi = {
  // 用户管理
  userPage: (params) => request.get('/admin/users', { params }),
  userRoles: (id) => request.get(`/admin/users/${id}/roles`),
  assignUserRoles: (id, ids) => request.put(`/admin/users/${id}/roles`, { ids }),
  updateUserStatus: (id, status) => request.patch(`/admin/users/${id}/status`, { status }),
  resetUserPassword: (id, password) => request.put(`/admin/users/${id}/password`, { password }),
  // 角色管理
  rolePage: (params) => request.get('/admin/roles', { params }),
  roleAll: () => request.get('/admin/roles/all'),
  roleCreate: (data) => request.post('/admin/roles', data),
  roleUpdate: (id, data) => request.put(`/admin/roles/${id}`, data),
  roleDelete: (id) => request.delete(`/admin/roles/${id}`),
  roleMenus: (id) => request.get(`/admin/roles/${id}/menus`),
  assignRoleMenus: (id, ids) => request.put(`/admin/roles/${id}/menus`, { ids }),
  // 菜单管理
  menuTree: () => request.get('/admin/menus/tree'),
  menuCreate: (data) => request.post('/admin/menus', data),
  menuUpdate: (id, data) => request.put(`/admin/menus/${id}`, data),
  menuDelete: (id) => request.delete(`/admin/menus/${id}`),
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
  addMember: (roomId, userId) => request.post(`/rooms/${roomId}/members/${userId}/add`),
  kickMember: (roomId, userId) => request.delete(`/rooms/${roomId}/members/${userId}`),
  memberCandidates: (roomId, keyword) => request.get(`/rooms/${roomId}/member-candidates`, { params: { keyword } }),
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
  update: (id, data) => request.put(`/configs/${id}`, data),
};

export const metricsApi = {
  dashboard: () => request.get('/metrics/dashboard'),
};

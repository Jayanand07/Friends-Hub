import api from './axios';

// Group CRUD
export const createGroup = (name, groupImageUrl, memberIds) =>
    api.post('/chat/groups', { name, groupImageUrl, memberIds });

export const getUserGroups = () => api.get('/chat/groups');

export const getGroupMessages = (groupId) => api.get(`/chat/groups/${groupId}/messages`);

export const getGroupMembers = (groupId) => api.get(`/chat/groups/${groupId}/members`);

// Members
export const addGroupMember = (groupId, userId) =>
    api.post(`/chat/groups/${groupId}/members/add`, { userId });

export const removeGroupMember = (groupId, userId) =>
    api.post(`/chat/groups/${groupId}/members/remove`, { userId });

// Messaging (REST fallback, though WS is preferred)
// WS destination: /app/chat.group.send/{groupId}

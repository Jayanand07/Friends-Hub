import api from './axios';

export const getNotifications = () => api.get('/notifications');

export const getUnreadCount = () => api.get('/notifications/unread-count');

export const markNotificationsRead = () => api.post('/notifications/mark-read');

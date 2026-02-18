import api from './axios';

export const login = (data) => api.post('/auth/login', data);
export const register = (data) => api.post('/auth/register', data);
export const verifyEmail = (token) => api.get(`/auth/verify?token=${token}`);
export const forgotPassword = (email) => api.post(`/auth/forgot-password?email=${email}`);
export const resetPassword = (token, newPassword) => api.post(`/auth/reset-password?token=${token}&newPassword=${newPassword}`);

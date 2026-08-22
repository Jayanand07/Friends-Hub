import api from './axios';

export const login = (data) => api.post('/auth/login', data);
export const register = (data) => api.post('/auth/register', data);
export const verifyEmail = (token) => api.get(`/auth/verify?token=${token}`);
export const forgotPassword = (email) => api.post('/auth/forgot-password', { email });
export const resendVerification = (email) => api.post('/auth/resend-verification', { email });
export const resetPassword = (email, otp, newPassword) => api.post('/auth/reset-password', { email, otp, newPassword });

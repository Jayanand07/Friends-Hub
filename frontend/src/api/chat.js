import api from './axios';

export const getChatHistory = (userId) => api.get(`/chat/history/${userId}`);
export const getMessages = getChatHistory;

export const getChatConversations = () => api.get('/chat/conversations');

export const searchChatUsers = (query) => api.get(`/chat/users/search?query=${encodeURIComponent(query)}`);

export const sendMessageRest = (receiverId, content, imageUrl) =>
    api.post('/chat/send', { receiverId, content, imageUrl });

export const markMessagesAsRead = (senderUserId) => api.post(`/chat/read/${senderUserId}`);

export const deleteChatMessage = (messageId) => api.delete(`/chat/message/${messageId}`);

export const getOnlineUsers = () => api.get('/chat/online');

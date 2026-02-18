import api from './axios';

export const addReaction = (targetType, targetId, emoji, gifUrl = null) =>
    api.post('/reactions', { targetType, targetId, emoji, gifUrl });

export const removeReaction = (targetType, targetId) =>
    api.delete(`/reactions/${targetType}/${targetId}`);

export const getReactions = (targetType, targetId) =>
    api.get(`/reactions/${targetType}/${targetId}`);

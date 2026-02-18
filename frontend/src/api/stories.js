import api from './axios';

export const getStories = () => api.get('/stories');

export const uploadStory = (file, onProgress) => {
    const formData = new FormData();
    formData.append('image', file);
    return api.post('/stories', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
            if (onProgress && e.total) {
                onProgress(Math.round((e.loaded * 100) / e.total));
            }
        },
    });
};

export const viewStory = (storyId) => api.post(`/stories/${storyId}/view`);

export const getStoryViewers = (storyId) => api.get(`/stories/${storyId}/viewers`);

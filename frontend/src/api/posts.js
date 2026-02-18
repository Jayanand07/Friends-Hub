import api from './axios';

export const getAllPosts = (page = 0, size = 10, sort = 'createdAt,desc') =>
    api.get(`/posts?page=${page}&size=${size}&sort=${sort}`);

export const getPostsByUser = (userId, page = 0, size = 10) =>
    api.get(`/posts/user/${userId}?page=${page}&size=${size}&sort=createdAt,desc`);

export const createPost = (data) => api.post('/posts', data);

export const deletePost = (postId) => api.delete(`/posts/${postId}`);

export const toggleLike = (postId) => api.post(`/posts/${postId}/like`);

export const addComment = (postId, data) => api.post(`/posts/${postId}/comment`, data);

export const getComments = (postId) => api.get(`/posts/${postId}/comments`);

export const uploadImage = (file, onProgress) => {
    const formData = new FormData();
    formData.append('image', file);
    return api.post('/posts/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
            if (onProgress && e.total) {
                onProgress(Math.round((e.loaded * 100) / e.total));
            }
        },
    });
};

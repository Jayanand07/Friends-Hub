import api from './axios';

export const getProfile = () => api.get('/users/profile');
export const getUserProfileById = (userId) => api.get(`/users/${userId}`);
export const updateProfile = (data) => api.put('/users/profile', data);
export const removeProfilePicture = () => api.delete('/users/profile/picture');
export const updateProfileSettings = (data) => api.put('/users/profile/settings', data);
export const followUser = (userId) => api.post(`/users/${userId}/follow`);
export const unfollowUser = (userId) => api.delete(`/users/${userId}/follow`);
export const getFollowers = (userId) => api.get(`/users/${userId}/followers`);
export const getFollowing = (userId) => api.get(`/users/${userId}/following`);

export const uploadProfilePic = (file) => {
    const formData = new FormData();
    formData.append('image', file);
    return api.post('/users/profile/picture', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

// Privacy
export const togglePrivateAccount = () => api.post('/users/private/toggle');

// Follow Requests
export const getFollowRequests = () => api.get('/users/follow-requests');
export const acceptFollowRequest = (requestId) => api.post(`/users/follow-request/${requestId}/accept`);
export const rejectFollowRequest = (requestId) => api.post(`/users/follow-request/${requestId}/reject`);
export const acceptFollowRequestFromUser = (userId) => api.post(`/users/follow-request/user/${userId}/accept`);
export const rejectFollowRequestFromUser = (userId) => api.post(`/users/follow-request/user/${userId}/reject`);

// Block
export const blockUser = (userId) => api.post(`/users/${userId}/block`);
export const unblockUser = (userId) => api.post(`/users/${userId}/unblock`);
export const getBlockedUsers = () => api.get('/users/blocked');

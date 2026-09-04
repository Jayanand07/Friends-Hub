import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 15000,
});

// Track online/offline status
window.addEventListener('offline', () => {
  window.dispatchEvent(new CustomEvent('app:offline'));
});
window.addEventListener('online', () => {
  window.dispatchEvent(new CustomEvent('app:online'));
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // FIX: only retry idempotent GET requests. Retrying a POST with no response
    // could duplicate non-idempotent actions (double post / double message send).
    const isGetRequest = String(originalRequest?.method || '').toLowerCase() === 'get';

    // Retry logic for transient failures (network error or 5xx) — max 1 retry
    if (isGetRequest && !originalRequest._retryCount && !error.response?.status && !originalRequest._retry) {
      originalRequest._retryCount = 1;
      // Wait a short beat before retrying
      await new Promise(res => setTimeout(res, 1000));
      return api(originalRequest);
    }
    if (isGetRequest && originalRequest._retryCount > 0 && !originalRequest._retry && error.response?.status && error.response.status >= 500 && error.response.status < 600) {
      originalRequest._retryCount--;
      await new Promise(res => setTimeout(res, 1000));
      return api(originalRequest);
    }

    // FIX: never run the refresh-token flow for /auth/* endpoints — a failed
    // login (401 "Invalid email or password") must surface its message to the
    // user, not trigger a pointless refresh + redirect to /login.
    const isAuthPath = String(originalRequest?.url || '').includes('/auth/');

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthPath) {
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({ resolve, reject });
        })
        .then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return api(originalRequest);
        })
        .catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const storedRefreshToken = localStorage.getItem('refreshToken');
        const res = await axios.post(`${api.defaults.baseURL}/auth/refresh`, {
          refreshToken: storedRefreshToken
        });
        const newToken = res.data.token;
        const newRefreshToken = res.data.refreshToken;

        localStorage.setItem('token', newToken);
        if (newRefreshToken) {
          localStorage.setItem('refreshToken', newRefreshToken);
        }

        api.defaults.headers.common['Authorization'] = 'Bearer ' + newToken;
        originalRequest.headers['Authorization'] = 'Bearer ' + newToken;

        processQueue(null, newToken);
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    // FIX: removed the blanket "any 403 logs the user out" handler. 403 means
    // authenticated-but-forbidden (blocked by a user, private content, missing
    // admin role) — the caller should show the error message instead of
    // destroying the session. Only the 401 refresh failure above clears it.
    return Promise.reject(error);
  }
);

/**
 * SECURITY (H-3): best-effort server-side logout — blacklists the current
 * access token AND revokes the refresh token. Uses a raw axios call so the
 * interceptors above don't kick in during logout.
 */
export async function revokeSession() {
  const token = localStorage.getItem('token');
  const refreshToken = localStorage.getItem('refreshToken');
  if (!token) return;
  try {
    await axios.post(`${api.defaults.baseURL}/auth/logout`, { refreshToken }, {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 5000,
    });
  } catch {
    // Best effort — local state is cleared by the caller regardless.
  }
}

export default api;

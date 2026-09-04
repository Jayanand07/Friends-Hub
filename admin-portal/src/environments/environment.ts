/**
 * Admin portal configuration.
 *
 * FIX: the analytics service previously hardcoded http://localhost:5000/api,
 * which never works in production. Set apiUrl to your deployed Node chat
 * service URL (e.g. https://your-chat-service.onrender.com/api).
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:5000/api'
};

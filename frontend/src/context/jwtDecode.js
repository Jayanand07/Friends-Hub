export function decodeToken(token) {
  if (!token || typeof token !== 'string') return null;
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64));
    if (payload.exp && Date.now() / 1000 > payload.exp) {
      return null;
    }
    return payload;
  } catch {
    return null;
  }
}

export function isTokenValid(token) {
  return decodeToken(token) !== null;
}

export function getTokenExpiry(token) {
  const payload = decodeToken(token);
  return payload?.exp ? new Date(payload.exp * 1000) : null;
}

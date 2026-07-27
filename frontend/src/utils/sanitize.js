/**
 * URL sanitization utilities for XSS prevention.
 *
 * Prevents:
 *   - javascript: URLs (classic XSS vector)
 *   - data: URLs (SVG/HTML injection)
 *   - vbscript: URLs (IE legacy)
 *   - file: URLs (local file access)
 */

const ALLOWED_PROTOCOLS = ['http:', 'https:', 'mailto:'];
const DISALLOWED_PATTERNS = [
  /^javascript:/i,
  /^data:/i,
  /^vbscript:/i,
  /^file:/i,
  /^blob:/i,
];

/**
 * Returns true if the URL is safe to use in href/src/action attributes.
 * Rejects dangerous schemes like javascript:, data:, vbscript:, file:.
 */
export function isSafeUrl(url) {
  if (!url || typeof url !== 'string') return false;

  // Quick check for dangerous patterns
  for (const pattern of DISALLOWED_PATTERNS) {
    if (pattern.test(url.trim())) return false;
  }

  // Allow protocol-relative URLs (//example.com/path)
  if (url.startsWith('//')) return true;

  // Allow relative paths (starting with /, ., or alphanumeric)
  if (/^[a-zA-Z0-9./\\]/.test(url) && !url.includes(':')) return true;

  // Check if the URL has an allowed protocol
  try {
    const parsed = new URL(url);
    return ALLOWED_PROTOCOLS.includes(parsed.protocol);
  } catch {
    // Not a valid URL — could be a relative path without a scheme
    return false;
  }
}

/**
 * Returns a safe URL or empty string if the URL is dangerous.
 */
export function getSafeUrl(url) {
  return isSafeUrl(url) ? url : '';
}

/**
 * Validates file types for uploads — rejects SVG and other dangerous formats.
 */
export function isAllowedImageType(mimeType) {
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
  return allowed.includes(mimeType);
}

/**
 * Formats a date string to a relative time string (e.g., "5m", "2h", "3d").
 * Handles ISO 8601 strings that may be missing the trailing Z.
 * Returns empty string for invalid dates.
 */
export default function timeAgo(dateStr) {
    if (!dateStr) return '';
    let str = String(dateStr).trim();
    // Fix missing trailing Z on ISO strings without timezone info
    if (str.includes('T') && !str.endsWith('Z') && !str.includes('+') && !/-\d{2}:\d{2}$/.test(str)) {
        str += 'Z';
    }
    const date = new Date(str);
    if (isNaN(date.getTime())) return '';
    const diff = Math.max(0, Date.now() - date.getTime());
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d`;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

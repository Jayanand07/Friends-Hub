import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Users, ChevronDown, ChevronUp } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getMutualFriends } from '../api/users';

const PREVIEW_COUNT = 3;

export default function MutualFriendsPanel({ targetUserId }) {
    const [mutuals, setMutuals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expanded, setExpanded] = useState(false);

    useEffect(() => {
        if (!targetUserId) return;
        setLoading(true);
        getMutualFriends(targetUserId)
            .then(res => setMutuals(Array.isArray(res.data) ? res.data : []))
            .catch(() => setMutuals([]))
            .finally(() => setLoading(false));
    }, [targetUserId]);

    if (loading || mutuals.length === 0) return null;

    const preview = mutuals.slice(0, PREVIEW_COUNT);
    const rest = mutuals.slice(PREVIEW_COUNT);
    const names = preview.map(u => u.name?.split(' ')[0] || 'Someone').join(', ');
    const overflow = mutuals.length > PREVIEW_COUNT ? ` and ${mutuals.length - PREVIEW_COUNT} more` : '';

    return (
        <motion.div
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-2"
        >
            {/* Summary row — always visible */}
            <button
                onClick={() => setExpanded(e => !e)}
                className="flex items-center gap-2 cursor-pointer group"
            >
                {/* Stacked avatars */}
                <div className="flex -space-x-2">
                    {preview.map((u, i) => (
                        <div
                            key={u.id}
                            className="w-5 h-5 rounded-full border-2 border-[var(--bg-primary)] avatar text-[7px] flex-shrink-0"
                            style={{ zIndex: PREVIEW_COUNT - i }}
                        >
                            {u.profilePicUrl
                                ? <img src={u.profilePicUrl} alt={u.name} className="w-full h-full object-cover rounded-full" />
                                : u.name?.charAt(0)?.toUpperCase() || '?'}
                        </div>
                    ))}
                </div>

                <span className="text-[12px] text-[var(--text-muted)] group-hover:text-[var(--text-secondary)] transition-colors">
                    <span className="font-medium text-[var(--text-secondary)]">{names}</span>
                    {overflow} follow them too
                </span>

                {expanded
                    ? <ChevronUp size={12} className="text-[var(--text-muted)]" />
                    : <ChevronDown size={12} className="text-[var(--text-muted)]" />}
            </button>

            {/* Expanded list */}
            <AnimatePresence>
                {expanded && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="overflow-hidden"
                    >
                        <div className="mt-3 bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl overflow-hidden">
                            {/* Header */}
                            <div className="flex items-center gap-2 px-3 py-2.5 border-b border-[var(--border-color)]">
                                <Users size={13} className="text-[var(--accent)]" />
                                <span className="text-[12px] font-semibold text-[var(--text-primary)]">
                                    {mutuals.length} Mutual Friend{mutuals.length !== 1 ? 's' : ''}
                                </span>
                            </div>

                            {/* List */}
                            <div className="max-h-52 overflow-y-auto divide-y divide-[var(--border-color)]/50">
                                {mutuals.map((u, i) => (
                                    <motion.div
                                        key={u.id}
                                        initial={{ opacity: 0, x: -6 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: i * 0.03 }}
                                    >
                                        <Link
                                            to={`/profile/${u.id}`}
                                            className="flex items-center gap-3 px-3 py-2.5 hover:bg-[var(--bg-elevated)] transition-colors group/row"
                                        >
                                            <div className="avatar w-8 h-8 text-[10px] flex-shrink-0 group-hover/row:ring-2 ring-[var(--accent)] transition-all">
                                                {u.profilePicUrl
                                                    ? <img src={u.profilePicUrl} alt={u.name} className="w-full h-full object-cover rounded-full" />
                                                    : u.name?.charAt(0)?.toUpperCase() || '?'}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <p className="text-[13px] font-semibold text-[var(--text-primary)] truncate group-hover/row:underline">
                                                    {u.name || 'User'}
                                                </p>
                                                <p className="text-[11px] text-[var(--text-muted)] truncate">
                                                    Mutual connection
                                                </p>
                                            </div>
                                        </Link>
                                    </motion.div>
                                ))}
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}

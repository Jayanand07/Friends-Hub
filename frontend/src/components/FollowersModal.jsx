import { motion, AnimatePresence } from 'framer-motion';
import { X, Users, UserPlus, UserMinus } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function FollowersModal({ open, onClose, title, users }) {
    return (
        <AnimatePresence>
            {open && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="fixed inset-0 z-50 flex items-end sm:items-center justify-center px-0 sm:px-4"
                >
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />

                    <motion.div
                        initial={{ opacity: 0, y: 40 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 40 }}
                        className="glass-strong rounded-t-2xl sm:rounded-2xl w-full sm:max-w-sm relative z-10 max-h-[80vh]"
                    >
                        <div className="flex items-center justify-between p-5 border-b border-[var(--border-color)]">
                            <div className="flex items-center gap-2">
                                <Users size={16} className="text-[var(--accent)]" />
                                <h2 className="text-base font-bold">{title}</h2>
                                {users?.length > 0 && (
                                    <span className="text-[11px] bg-[var(--accent-light)] text-[var(--accent)] px-2 py-0.5 rounded-full font-medium">
                                        {users.length}
                                    </span>
                                )}
                            </div>
                            <button onClick={onClose} className="btn-icon w-8 h-8 border-0">
                                <X size={16} />
                            </button>
                        </div>

                        <div className="max-h-80 overflow-y-auto p-3 space-y-1">
                            {(!users || users.length === 0) && (
                                <div className="text-center py-10">
                                    <Users size={32} className="text-[var(--text-muted)] mx-auto mb-3 opacity-40" />
                                    <p className="text-[13px] text-[var(--text-muted)]">No users found</p>
                                </div>
                            )}
                            {users?.map((u, i) => (
                                <UserRow key={i} user={u} delay={i * 0.04} onUserClick={onClose} />
                            ))}
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

function UserRow({ user: u, delay, onUserClick }) {
    const [following, setFollowing] = useState(false);

    return (
        <motion.div
            initial={{ opacity: 0, x: -12 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay }}
            className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-[var(--bg-elevated)] transition-colors group"
        >
            <Link to={`/profile/${u.id}`} onClick={() => { if (onUserClick) onUserClick(); }}>
                <div className="avatar text-[11px] group-hover:ring-2 ring-[var(--accent)] transition-all">
                    {u.firstName?.charAt(0)?.toUpperCase() || u.email?.charAt(0)?.toUpperCase() || u.name?.charAt(0)?.toUpperCase() || '?'}
                </div>
            </Link>
            <div className="flex-1 min-w-0">
                <Link to={`/profile/${u.id}`} onClick={() => { if (onUserClick) onUserClick(); }}>
                    <p className="text-[13px] font-medium text-[var(--text-primary)] truncate hover:underline">
                        {u.name || (u.firstName && u.lastName ? `${u.firstName} ${u.lastName}` : u.email)}
                    </p>
                </Link>
                {u.email && <p className="text-[11px] text-[var(--text-muted)] truncate">{u.email}</p>}
            </div>
            <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setFollowing(!following)}
                className={`text-[11px] font-medium px-3 py-1.5 rounded-lg transition-all cursor-pointer flex items-center gap-1 ${following
                    ? 'bg-[var(--bg-elevated)] text-[var(--text-secondary)] border border-[var(--border-color)]'
                    : 'bg-[var(--accent)] text-white'
                    }`}
            >
                {following ? <><UserMinus size={11} /> Unfollow</> : <><UserPlus size={11} /> Follow</>}
            </motion.button>
        </motion.div>
    );
}

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, UserX, Loader2 } from 'lucide-react';
import { getBlockedUsers, unblockUser } from '../api/users';
import { useToast } from './Toast';

export default function BlockedUsersList({ onClose }) {
    const [blocked, setBlocked] = useState([]);
    const [loading, setLoading] = useState(true);
    const toast = useToast();

    useEffect(() => {
        loadBlocked();
    }, []);

    const loadBlocked = async () => {
        try {
            const res = await getBlockedUsers();
            setBlocked(res.data);
        } catch (err) {
            console.error(err);
            toast.error('Failed to load blocked users');
        } finally {
            setLoading(false);
        }
    };

    const handleUnblock = async (userId) => {
        try {
            await unblockUser(userId);
            setBlocked(prev => prev.filter(u => u.userId !== userId));
        } catch (err) {
            console.error(err);
            toast.error('Failed to unblock user');
        }
    };

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm"
            onClick={onClose}
        >
            <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.9, opacity: 0 }}
                onClick={(e) => e.stopPropagation()}
                className="bg-[var(--bg-card)] rounded-xl w-full max-w-md mx-4 overflow-hidden border border-[var(--border-color)]"
            >
                <div className="flex items-center justify-between p-4 border-b border-[var(--border-color)]">
                    <h3 className="text-[var(--text-primary)] font-semibold">Blocked Users</h3>
                    <button onClick={onClose} className="text-[var(--text-muted)] hover:text-[var(--text-primary)] cursor-pointer">
                        <X size={20} />
                    </button>
                </div>

                <div className="max-h-[400px] overflow-y-auto">
                    {loading ? (
                        <div className="flex justify-center py-12">
                            <Loader2 className="animate-spin text-[var(--text-muted)]" size={24} />
                        </div>
                    ) : blocked.length === 0 ? (
                        <div className="text-center py-12 text-[var(--text-muted)]">
                            <UserX size={48} className="mx-auto mb-3 opacity-40" />
                            <p className="text-sm">No blocked users</p>
                        </div>
                    ) : (
                        blocked.map(user => (
                            <div key={user.userId} className="flex items-center justify-between px-4 py-3 hover:bg-[var(--bg-elevated)]">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center">
                                        {user.profilePicUrl ? (
                                            <img src={user.profilePicUrl} alt={user.name} className="w-full h-full object-cover" />
                                        ) : (
                                            <span className="text-white text-sm font-semibold">
                                                {user.name?.charAt(0)?.toUpperCase() || '?'}
                                            </span>
                                        )}
                                    </div>
                                    <span className="text-[var(--text-primary)] text-sm font-medium">{user.name}</span>
                                </div>
                                <button
                                    onClick={() => handleUnblock(user.userId)}
                                    className="px-4 py-1.5 text-sm font-semibold text-[var(--text-primary)] bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-lg hover:bg-[var(--bg-hover)] transition-colors cursor-pointer"
                                >
                                    Unblock
                                </button>
                            </div>
                        ))
                    )}
                </div>
            </motion.div>
        </motion.div>
    );
}

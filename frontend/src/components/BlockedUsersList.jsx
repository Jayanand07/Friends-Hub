import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, UserX, Loader2 } from 'lucide-react';
import { getBlockedUsers, unblockUser } from '../api/users';

export default function BlockedUsersList({ onClose }) {
    const [blocked, setBlocked] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadBlocked();
    }, []);

    const loadBlocked = async () => {
        try {
            const res = await getBlockedUsers();
            setBlocked(res.data);
        } catch (err) {
            console.error(err);
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
                className="bg-[#262626] rounded-xl w-full max-w-md mx-4 overflow-hidden"
            >
                <div className="flex items-center justify-between p-4 border-b border-[#363636]">
                    <h3 className="text-white font-semibold">Blocked Users</h3>
                    <button onClick={onClose} className="text-[#a8a8a8] hover:text-white cursor-pointer">
                        <X size={20} />
                    </button>
                </div>

                <div className="max-h-[400px] overflow-y-auto">
                    {loading ? (
                        <div className="flex justify-center py-12">
                            <Loader2 className="animate-spin text-[#a8a8a8]" size={24} />
                        </div>
                    ) : blocked.length === 0 ? (
                        <div className="text-center py-12 text-[#a8a8a8]">
                            <UserX size={48} className="mx-auto mb-3 opacity-40" />
                            <p className="text-sm">No blocked users</p>
                        </div>
                    ) : (
                        blocked.map(user => (
                            <div key={user.userId} className="flex items-center justify-between px-4 py-3 hover:bg-[#1a1a1a]">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center">
                                        {user.profilePicUrl ? (
                                            <img src={user.profilePicUrl} alt="" className="w-full h-full object-cover" />
                                        ) : (
                                            <span className="text-white text-sm font-semibold">
                                                {user.name?.charAt(0)?.toUpperCase() || '?'}
                                            </span>
                                        )}
                                    </div>
                                    <span className="text-white text-sm font-medium">{user.name}</span>
                                </div>
                                <button
                                    onClick={() => handleUnblock(user.userId)}
                                    className="px-4 py-1.5 text-sm font-semibold text-white bg-[#363636] rounded-lg hover:bg-[#464646] transition-colors cursor-pointer"
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

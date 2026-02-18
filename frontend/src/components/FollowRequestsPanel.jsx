import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, X, Loader2, UserPlus } from 'lucide-react';
import { getFollowRequests, acceptFollowRequest, rejectFollowRequest } from '../api/users';

export default function FollowRequestsPanel() {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadRequests();
    }, []);

    const loadRequests = async () => {
        try {
            const res = await getFollowRequests();
            setRequests(res.data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleAccept = async (requestId) => {
        try {
            await acceptFollowRequest(requestId);
            setRequests(prev => prev.filter(r => r.userId !== requestId));
        } catch (err) {
            console.error(err);
        }
    };

    const handleReject = async (requestId) => {
        try {
            await rejectFollowRequest(requestId);
            setRequests(prev => prev.filter(r => r.userId !== requestId));
        } catch (err) {
            console.error(err);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center py-12">
                <Loader2 className="animate-spin text-[#a8a8a8]" size={24} />
            </div>
        );
    }

    return (
        <div>
            <h3 className="text-white font-semibold mb-4 flex items-center gap-2">
                <UserPlus size={18} />
                Follow Requests
                {requests.length > 0 && (
                    <span className="px-2 py-0.5 text-xs font-bold bg-[#ff3040] text-white rounded-full">
                        {requests.length}
                    </span>
                )}
            </h3>

            {requests.length === 0 ? (
                <div className="text-center py-8 text-[#a8a8a8]">
                    <UserPlus size={40} className="mx-auto mb-3 opacity-30" />
                    <p className="text-sm">No pending follow requests</p>
                </div>
            ) : (
                <AnimatePresence>
                    {requests.map(user => (
                        <motion.div
                            key={user.userId}
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: 'auto' }}
                            exit={{ opacity: 0, height: 0 }}
                            className="flex items-center justify-between p-3 rounded-lg hover:bg-[#1a1a1a] mb-1"
                        >
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
                            <div className="flex gap-2">
                                <motion.button
                                    whileTap={{ scale: 0.9 }}
                                    onClick={() => handleAccept(user.userId)}
                                    className="px-4 py-1.5 text-sm font-semibold text-white bg-[#0095f6] rounded-lg hover:bg-[#1aa1f7] transition-colors cursor-pointer"
                                >
                                    <Check size={16} />
                                </motion.button>
                                <motion.button
                                    whileTap={{ scale: 0.9 }}
                                    onClick={() => handleReject(user.userId)}
                                    className="px-4 py-1.5 text-sm font-semibold text-white bg-[#363636] rounded-lg hover:bg-[#464646] transition-colors cursor-pointer"
                                >
                                    <X size={16} />
                                </motion.button>
                            </div>
                        </motion.div>
                    ))}
                </AnimatePresence>
            )}
        </div>
    );
}

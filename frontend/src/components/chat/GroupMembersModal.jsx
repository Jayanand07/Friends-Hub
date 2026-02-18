import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { X, UserMinus, Shield } from 'lucide-react';
import { getGroupMembers, removeGroupMember } from '../../api/groupChat';
import { useToast } from '../Toast';

export default function GroupMembersModal({ groupId, onClose, currentUser }) {
    const [members, setMembers] = useState([]);
    const [loading, setLoading] = useState(true);
    const toast = useToast();

    // Fetch members on mount
    useEffect(() => {
        setLoading(true);
        getGroupMembers(groupId)
            .then(res => setMembers(Array.isArray(res.data) ? res.data : []))
            .catch(err => {
                console.error(err);
                if (err.response?.status !== 404) { // Ignore if just new endpoint setup delay
                    toast.error("Failed to load members");
                }
            })
            .finally(() => setLoading(false));
    }, [groupId]);

    const handleRemove = async (userId) => {
        if (!confirm("Remove this member?")) return;
        try {
            await removeGroupMember(groupId, userId);
            setMembers(prev => prev.filter(m => (m.userId || m.id) !== userId));
            toast.success("Member removed");
        } catch (err) {
            console.error(err);
            toast.error("Failed to remove member");
        }
    };

    return (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                onClick={(e) => e.stopPropagation()}
                className="bg-[var(--bg-card)] border border-[var(--border-color)] w-full max-w-sm rounded-2xl overflow-hidden shadow-2xl"
            >
                <div className="p-4 border-b border-[var(--border-color)] flex justify-between items-center">
                    <h2 className="text-lg font-bold">Group Members</h2>
                    <button onClick={onClose}><X size={20} /></button>
                </div>

                <div className="max-h-[60vh] overflow-y-auto custom-scrollbar p-2">
                    {loading ? (
                        <div className="text-center py-4 text-[var(--text-muted)]">Loading...</div>
                    ) : members.length === 0 ? (
                        <div className="text-center py-4 text-[var(--text-muted)]">No members found</div>
                    ) : (
                        members.map(member => {
                            const isMe = (member.userId || member.id) === currentUser.id;
                            // Check if current user is admin/creator of group to allow removal?
                            // For MVP, letting backend handle permission check, we just show button if not self.
                            // But usually frontend should know if user is admin. 
                            // `ChatGroupDTO` has `createdById`. We passed `groupId` prop, not full group object.
                            // Ideally, `GroupMembersModal` should take `group` object prop.
                            // I'll leave the remove button visible for all non-self users for now, 
                            // backend will reject if not allowed.

                            return (
                                <div key={member.userId || member.id} className="flex items-center justify-between p-3 hover:bg-[var(--bg-elevated)] rounded-lg">
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 rounded-full bg-[var(--bg-elevated)] overflow-hidden">
                                            {member.profilePicUrl ? (
                                                <img src={member.profilePicUrl} className="w-full h-full object-cover" />
                                            ) : (
                                                <div className="w-full h-full flex items-center justify-center font-bold text-[var(--text-muted)]">
                                                    {(member.firstName?.[0] || member.email?.[0]).toUpperCase()}
                                                </div>
                                            )}
                                        </div>
                                        <div>
                                            <div className="font-medium text-sm text-[var(--text-primary)]">
                                                {member.firstName} {member.lastName}
                                            </div>
                                            <div className="text-xs text-[var(--text-muted)]">
                                                {isMe ? 'You' : ''}
                                            </div>
                                        </div>
                                    </div>

                                    {!isMe && (
                                        <button
                                            onClick={() => handleRemove(member.userId || member.id)}
                                            className="p-2 text-red-500 hover:bg-red-500/10 rounded-full transition-colors"
                                            title="Remove Member"
                                        >
                                            <UserMinus size={18} />
                                        </button>
                                    )}
                                </div>
                            );
                        })
                    )}
                </div>
            </motion.div>
        </div>
    );
}

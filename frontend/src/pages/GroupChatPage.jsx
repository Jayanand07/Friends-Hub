import { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Plus, Users } from 'lucide-react';
import { getUserGroups } from '../api/groupChat';
import { useAuth } from '../context/AuthContext';
import GroupChatWindow from '../components/chat/GroupChatWindow';
import CreateGroupModal from '../components/chat/CreateGroupModal';

export default function GroupChatPage() {
    const { user } = useAuth();
    const [groups, setGroups] = useState([]);
    const [selectedGroup, setSelectedGroup] = useState(null);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [loading, setLoading] = useState(true);

    const fetchGroups = () => {
        setLoading(true);
        getUserGroups()
            .then(res => setGroups(Array.isArray(res.data) ? res.data : []))
            .catch(err => console.error("Failed to load groups", err))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchGroups();
    }, []);

    const handleCreateGroup = () => {
        setShowCreateModal(false);
        fetchGroups();
    };

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex h-[calc(100vh-80px)] md:h-[calc(100vh-96px)] -m-4 md:-m-6"
        >
            {/* Group List Sidebar */}
            <div className={`w-full md:w-80 border-r border-[var(--border-color)] bg-[var(--bg-card)]/30 flex-shrink-0 flex flex-col ${selectedGroup ? 'hidden md:flex' : 'flex'
                }`}>

                {/* Header */}
                <div className="p-4 border-b border-[var(--border-color)] flex justify-between items-center bg-[var(--bg-card)]/50 backdrop-blur-md sticky top-0 z-10">
                    <h2 className="text-xl font-bold bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)] bg-clip-text text-transparent">Groups</h2>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="p-2 rounded-full hover:bg-[var(--bg-elevated)] text-[var(--text-primary)] transition-colors"
                        title="Create Group"
                    >
                        <Plus size={24} />
                    </button>
                </div>

                {/* List */}
                <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar">
                    {loading ? (
                        <div className="flex justify-center py-10">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[var(--accent)]"></div>
                        </div>
                    ) : groups.length === 0 ? (
                        <div className="text-center py-10 px-4 text-[var(--text-muted)]">
                            <div className="w-16 h-16 bg-[var(--bg-elevated)] rounded-full flex items-center justify-center mx-auto mb-4">
                                <Users size={32} />
                            </div>
                            <p>No groups yet.</p>
                            <button
                                onClick={() => setShowCreateModal(true)}
                                className="mt-4 text-[var(--accent)] hover:underline"
                            >
                                Create one
                            </button>
                        </div>
                    ) : (
                        groups.map(group => (
                            <div
                                key={group.groupId}
                                onClick={() => setSelectedGroup(group)}
                                className={`p-4 flex items-center gap-3 cursor-pointer hover:bg-[var(--bg-elevated)]/50 transition-colors border-b border-[var(--border-color)]/30 ${selectedGroup?.groupId === group.groupId ? 'bg-[var(--bg-elevated)] border-l-4 border-l-[var(--accent)]' : 'border-l-4 border-l-transparent'
                                    }`}
                            >
                                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[var(--gradient-1)] to-[var(--gradient-2)] p-[2px]">
                                    {group.groupImageUrl ? (
                                        <img src={group.groupImageUrl} alt={group.name} className="w-full h-full rounded-[10px] object-cover bg-[var(--bg-card)]" />
                                    ) : (
                                        <div className="w-full h-full rounded-[10px] bg-[var(--bg-card)] flex items-center justify-center text-[var(--text-primary)] font-bold">
                                            {group.name.charAt(0).toUpperCase()}
                                        </div>
                                    )}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex justify-between items-center mb-0.5">
                                        <h3 className="font-semibold text-[var(--text-primary)] truncate">{group.name}</h3>
                                        {group.lastMessageTime && (
                                            <span className="text-[11px] text-[var(--text-muted)] whitespace-nowrap ml-2">
                                                {new Date(group.lastMessageTime).toLocaleDateString()}
                                            </span>
                                        )}
                                    </div>
                                    <p className="text-[13px] text-[var(--text-muted)] truncate">
                                        {group.lastMessage || `${group.memberCount} members`}
                                    </p>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>

            {/* Chat Window */}
            <div className={`flex-1 flex flex-col ${selectedGroup ? 'flex' : 'hidden md:flex'}`}>
                {selectedGroup ? (
                    <GroupChatWindow
                        group={selectedGroup}
                        currentUser={user}
                        onBack={() => setSelectedGroup(null)}
                        refreshGroups={fetchGroups}
                    />
                ) : (
                    <div className="flex-1 flex flex-col items-center justify-center text-[var(--text-muted)]">
                        <div className="w-20 h-20 bg-[var(--bg-elevated)] rounded-full flex items-center justify-center mb-6 animate-pulse">
                            <Users size={40} opacity={0.5} />
                        </div>
                        <p className="text-lg">Select a group to start chatting</p>
                    </div>
                )}
            </div>

            {/* Create Group Modal */}
            {showCreateModal && (
                <CreateGroupModal
                    onClose={() => setShowCreateModal(false)}
                    onCreated={handleCreateGroup}
                />
            )}
        </motion.div>
    );
}

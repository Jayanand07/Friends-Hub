import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Search, MessageCircle, Plus } from 'lucide-react';
import { searchChatUsers } from '../../api/chat';

export default function UserListSidebar({ conversations, activeUserId, onSelectUser, onlineUsers = [] }) {
    const [search, setSearch] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [searching, setSearching] = useState(false);

    const handleSearch = async (q) => {
        setSearch(q);
        if (q.trim().length < 2) {
            setSearchResults([]);
            return;
        }
        setSearching(true);
        try {
            const res = await searchChatUsers(q);
            setSearchResults(Array.isArray(res.data) ? res.data : []);
        } catch {
            setSearchResults([]);
        } finally {
            setSearching(false);
        }
    };

    const displayList = search.trim().length >= 2 ? searchResults : conversations;
    const isOnline = (userId) => onlineUsers.includes(userId);

    return (
        <div className="flex flex-col h-full">
            {/* Header */}
            <div className="p-4 border-b border-[var(--border-color)]">
                <h2 className="text-base font-bold flex items-center gap-2 mb-3">
                    <MessageCircle size={18} className="text-[var(--accent)]" />
                    Messages
                    {onlineUsers.length > 0 && (
                        <span className="text-[10px] font-medium text-emerald-400 bg-emerald-400/10 px-2 py-0.5 rounded-full">
                            {onlineUsers.length} online
                        </span>
                    )}
                </h2>
                <div className="relative">
                    <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                    <input
                        type="text"
                        className="input-field pl-9 text-[12px] py-2"
                        placeholder="Search users..."
                        value={search}
                        onChange={(e) => handleSearch(e.target.value)}
                    />
                </div>
            </div>

            {/* User list */}
            <div className="flex-1 overflow-y-auto p-2 space-y-0.5">
                {search.trim().length >= 2 && searchResults.length === 0 && !searching && (
                    <p className="text-center text-[11px] text-[var(--text-muted)] py-8">No users found</p>
                )}
                {displayList.length === 0 && search.trim().length < 2 && (
                    <div className="text-center py-10 px-4">
                        <div className="w-14 h-14 rounded-2xl bg-[var(--bg-elevated)] border border-[var(--border-color)] flex items-center justify-center mx-auto mb-3">
                            <Plus size={20} className="text-[var(--text-muted)]" />
                        </div>
                        <p className="text-[12px] text-[var(--text-muted)]">Search for a user to start chatting</p>
                    </div>
                )}
                <AnimatePresence>
                    {displayList.map((user, idx) => {
                        const online = isOnline(user.id) || user.online;
                        return (
                            <motion.button
                                key={user.id}
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ delay: idx * 0.03 }}
                                onClick={() => { onSelectUser(user); setSearch(''); setSearchResults([]); }}
                                className={`w-full flex items-center gap-3 p-3 rounded-xl transition-all cursor-pointer text-left ${activeUserId === user.id
                                    ? 'bg-[var(--accent-light)] border border-[var(--accent)]/30'
                                    : 'hover:bg-[var(--bg-elevated)] border border-transparent'
                                    }`}
                            >
                                { /* Avatar with online indicator */}
                                <div className="relative flex-shrink-0" onClick={(e) => e.stopPropagation()}>
                                    <Link to={`/profile/${user.id}`} className="block group/avatar">
                                        <div className={`avatar text-[11px] group-hover/avatar:ring-2 ring-[var(--accent)] transition-all ${activeUserId === user.id ? 'ring-2 ring-[var(--accent)]' : ''}`}>
                                            {user.name?.charAt(0)?.toUpperCase() || '?'}
                                        </div>
                                    </Link>
                                    {online && (
                                        <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-emerald-400 border-2 border-[var(--bg-secondary)]" />
                                    )}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-[13px] font-medium text-[var(--text-primary)] truncate">{user.name}</p>
                                    <p className="text-[10px] text-[var(--text-muted)] truncate">
                                        {online ? <span className="text-emerald-400">Online</span> : user.email}
                                    </p>
                                </div>
                                {activeUserId === user.id && (
                                    <div className="w-2 h-2 rounded-full bg-[var(--accent)]" />
                                )}
                            </motion.button>
                        );
                    })}
                </AnimatePresence>
            </div>
        </div>
    );
}

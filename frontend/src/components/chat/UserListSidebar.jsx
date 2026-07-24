import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Search, MessageCircle, Plus, Users } from 'lucide-react';
import { searchChatUsers } from '../../api/chat';

export default function UserListSidebar({ conversations, activeUserId, onSelectUser, onlineUsers = [] }) {
    const [search, setSearch] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [showOnlineOnly, setShowOnlineOnly] = useState(false);

    const debounceRef = useRef(null);

    const handleSearch = (q) => {
        setSearch(q);

        if (debounceRef.current) clearTimeout(debounceRef.current);

        if (q.trim().length < 2) {
            setSearchResults([]);
            return;
        }

        debounceRef.current = setTimeout(async () => {
            setSearching(true);
            try {
                const res = await searchChatUsers(q);
                setSearchResults(Array.isArray(res.data) ? res.data : []);
            } catch {
                setSearchResults([]);
            } finally {
                setSearching(false);
            }
        }, 300);
    };

    useEffect(() => {
        return () => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
        };
    }, []);

    // Strict type string coercion for online status check
    const isOnline = (userId) => onlineUsers.some(id => String(id) === String(userId));

    let baseList = search.trim().length >= 2 ? searchResults : conversations;
    if (showOnlineOnly) {
        baseList = baseList.filter(u => isOnline(u.id) || u.online);
    }

    return (
        <div className="flex flex-col h-full">
            {/* Header */}
            <div className="p-4 border-b border-[var(--border-color)]">
                <div className="flex items-center justify-between mb-3">
                    <h2 className="text-base font-bold flex items-center gap-2">
                        <MessageCircle size={18} className="text-[var(--accent)]" />
                        Messages
                    </h2>
                    {onlineUsers.length > 0 && (
                        <button
                            type="button"
                            onClick={() => setShowOnlineOnly(!showOnlineOnly)}
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full transition-all cursor-pointer flex items-center gap-1 ${
                                showOnlineOnly
                                    ? 'bg-emerald-500 text-white shadow-sm'
                                    : 'text-emerald-400 bg-emerald-400/10 hover:bg-emerald-400/20'
                            }`}
                            title={showOnlineOnly ? "Show all users" : "Filter active online users"}
                        >
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                            {onlineUsers.length} online
                        </button>
                    )}
                </div>

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
                {showOnlineOnly && baseList.length === 0 && (
                    <div className="text-center py-10 px-4">
                        <Users size={24} className="text-emerald-400 mx-auto mb-2 opacity-60" />
                        <p className="text-[12px] text-[var(--text-muted)]">No active users online right now</p>
                    </div>
                )}
                {baseList.length === 0 && search.trim().length < 2 && !showOnlineOnly && (
                    <div className="text-center py-10 px-4">
                        <div className="w-14 h-14 rounded-2xl bg-[var(--bg-elevated)] border border-[var(--border-color)] flex items-center justify-center mx-auto mb-3">
                            <Plus size={20} className="text-[var(--text-muted)]" />
                        </div>
                        <p className="text-[12px] text-[var(--text-muted)]">Search for a user to start chatting</p>
                    </div>
                )}
                <AnimatePresence>
                    {baseList.map((user, idx) => {
                        const online = isOnline(user.id) || user.online;
                        return (
                            <motion.button
                                key={user.id}
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                transition={{ duration: 0.15 }}
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
                                        <div className="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full bg-emerald-400 border-2 border-[var(--bg-secondary)] shadow-sm" />
                                    )}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="text-[13px] font-medium text-[var(--text-primary)] truncate">{user.name}</p>
                                    <p className="text-[10px] text-[var(--text-muted)] truncate flex items-center gap-1">
                                        {online ? (
                                            <span className="text-emerald-400 font-semibold flex items-center gap-1">
                                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                                                Online
                                            </span>
                                        ) : "Offline"}
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

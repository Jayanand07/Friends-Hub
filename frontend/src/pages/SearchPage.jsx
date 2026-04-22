import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search as SearchIcon, X, Clock, User, ArrowRight } from 'lucide-react';
import { searchChatUsers } from '../api/chat';
import { useNavigate } from 'react-router-dom';

export default function SearchPage() {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [recentSearches, setRecentSearches] = useState([]);
    const navigate = useNavigate();

    // Load recent searches from localStorage
    useEffect(() => {
        const saved = localStorage.getItem('recent_searches');
        if (saved) setRecentSearches(JSON.parse(saved));
    }, []);

    const handleSearch = async (val) => {
        setQuery(val);
        if (val.trim().length < 2) {
            setResults([]);
            return;
        }

        setSearching(true);
        try {
            const res = await searchChatUsers(val);
            setResults(Array.isArray(res.data) ? res.data : []);
        } catch {
            setResults([]);
        } finally {
            setSearching(false);
        }
    };

    const handleSelectUser = (user) => {
        // Add to recent searches
        const newRecent = [
            { id: user.id || user.userId, name: user.name, profilePic: user.profilePic || user.profilePicUrl },
            ...recentSearches.filter(s => s.id !== (user.id || user.userId))
        ].slice(0, 10);

        setRecentSearches(newRecent);
        localStorage.setItem('recent_searches', JSON.stringify(newRecent));

        // Navigate to profile
        navigate(`/profile/${user.id || user.userId}`);
    };

    const clearRecent = () => {
        setRecentSearches([]);
        localStorage.removeItem('recent_searches');
    };

    const removeOneRecent = (id) => {
        const filtered = recentSearches.filter(s => s.id !== id);
        setRecentSearches(filtered);
        localStorage.setItem('recent_searches', JSON.stringify(filtered));
    };

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="max-w-[600px] mx-auto pt-4 md:pt-10 px-4"
        >
            {/* Search Input */}
            <div className="relative mb-8">
                <div className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)]">
                    <SearchIcon size={20} />
                </div>
                <input
                    type="text"
                    value={query}
                    onChange={(e) => handleSearch(e.target.value)}
                    placeholder="Search for people..."
                    className="w-full pl-12 pr-12 py-4 bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-2xl text-[15px] focus:ring-2 focus:ring-[var(--accent)] outline-none transition-all shadow-xl shadow-black/5"
                    autoFocus
                />
                {query && (
                    <button
                        onClick={() => handleSearch('')}
                        className="absolute right-4 top-1/2 -translate-y-1/2 p-1 text-[var(--text-muted)] hover:text-[var(--text-primary)]"
                    >
                        <X size={18} />
                    </button>
                )}
            </div>

            <div className="space-y-6">
                {/* Search Results */}
                {query.trim().length >= 2 && (
                    <div className="space-y-2">
                        <div className="flex items-center justify-between px-1 mb-2">
                            <h3 className="text-[14px] font-bold text-[var(--text-primary)]">Results</h3>
                            {searching && <Loader size={14} className="animate-spin text-[var(--accent)]" />}
                        </div>
                        <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-2xl overflow-hidden shadow-sm">
                            {results.length === 0 && !searching ? (
                                <div className="p-8 text-center text-[14px] text-[var(--text-muted)]">
                                    No users found for "{query}"
                                </div>
                            ) : (
                                results.map((user, idx) => (
                                    <button
                                        key={user.id || idx}
                                        onClick={() => handleSelectUser(user)}
                                        className="w-full flex items-center gap-4 p-4 hover:bg-[var(--bg-elevated)] transition-colors border-b last:border-none border-[var(--border-color)]/30 text-left group"
                                    >
                                        <div className="avatar w-12 h-12 text-[14px]">
                                            {user.profilePic || user.profilePicUrl ? (
                                                <img src={user.profilePic || user.profilePicUrl} className="w-full h-full object-cover rounded-full" />
                                            ) : (user.name?.charAt(0).toUpperCase() || '?')}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-[14px] font-bold text-[var(--text-primary)] truncate">{user.name}</p>
                                        </div>
                                        <ArrowRight size={16} className="text-[var(--text-muted)] opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                                    </button>
                                ))
                            )}
                        </div>
                    </div>
                )}

                {/* Recent Searches */}
                {query.trim().length < 2 && (
                    <div className="space-y-4">
                        <div className="flex items-center justify-between px-1">
                            <h3 className="text-[14px] font-bold text-[var(--text-primary)]">Recent Searches</h3>
                            {recentSearches.length > 0 && (
                                <button onClick={clearRecent} className="text-[12px] font-semibold text-[var(--accent)] hover:text-[var(--accent-hover)]">
                                    Clear All
                                </button>
                            )}
                        </div>

                        {recentSearches.length === 0 ? (
                            <div className="py-20 flex flex-col items-center justify-center text-center px-6">
                                <div className="w-16 h-16 rounded-full bg-[var(--bg-elevated)] flex items-center justify-center mb-4">
                                    <SearchIcon size={30} className="text-[var(--text-muted)]" />
                                </div>
                                <p className="text-[15px] font-medium text-[var(--text-primary)] mb-1">No recent searches</p>
                                <p className="text-[12px] text-[var(--text-muted)] max-w-[200px]">Search for your friends to quickly access their profiles.</p>
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {recentSearches.map((s) => (
                                    <div
                                        key={s.id}
                                        className="flex items-center gap-3 p-2 group"
                                    >
                                        <button
                                            onClick={() => navigate(`/profile/${s.id}`)}
                                            className="flex-1 flex items-center gap-3 text-left"
                                        >
                                            <div className="avatar w-10 h-10 text-[12px] flex-shrink-0">
                                                {s.profilePic ? (
                                                    <img src={s.profilePic} className="w-full h-full object-cover rounded-full" />
                                                ) : (s.name?.charAt(0).toUpperCase())}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <p className="text-[13px] font-bold text-[var(--text-primary)] truncate">{s.name}</p>
                                                <div className="flex items-center gap-1 text-[11px] text-[var(--text-muted)]">
                                                    <Clock size={10} />
                                                    <span>Recently viewed</span>
                                                </div>
                                            </div>
                                        </button>
                                        <button
                                            onClick={() => removeOneRecent(s.id)}
                                            className="p-2 text-[var(--text-muted)] hover:text-[var(--danger)] opacity-0 group-hover:opacity-100 transition-opacity"
                                        >
                                            <X size={14} />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </motion.div>
    );
}

const Loader = ({ size, className }) => (
    <div className={`animate-spin ${className}`} style={{ width: size, height: size }}>
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
    </div>
);

import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search as SearchIcon, X, Clock, ArrowRight, SlidersHorizontal, MapPin, Users, ArrowUpDown, ChevronDown } from 'lucide-react';
import { searchUsers } from '../api/users';
import { searchChatUsers } from '../api/chat';
import { useNavigate, useSearchParams } from 'react-router-dom';

const SORT_OPTIONS = [
    { value: 'relevance', label: 'Relevance' },
    { value: 'mutual', label: 'Mutual Friends' },
    { value: 'name', label: 'Name' },
];

function useDebounce(value, delay) {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), delay);
        return () => clearTimeout(t);
    }, [value, delay]);
    return debounced;
}

export default function SearchPage() {
    const [query, setQuery] = useState('');
    const [location, setLocation] = useState('');
    const [bio, setBio] = useState('');
    const [mutualOnly, setMutualOnly] = useState(false);
    const [sort, setSort] = useState('relevance');
    const [showFilters, setShowFilters] = useState(false);
    const [showSortMenu, setShowSortMenu] = useState(false);
    const [results, setResults] = useState([]);
    const [searching, setSearching] = useState(false);
    const [recentSearches, setRecentSearches] = useState([]);
    const [hasSearched, setHasSearched] = useState(false);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const sortRef = useRef(null);

    const debouncedQuery = useDebounce(query, 350);
    const debouncedLocation = useDebounce(location, 350);
    const debouncedBio = useDebounce(bio, 350);

    const activeFilterCount = [location, bio, mutualOnly].filter(Boolean).length;

    useEffect(() => {
        const saved = localStorage.getItem('recent_searches');
        if (saved) setRecentSearches(JSON.parse(saved));
    }, []);

    useEffect(() => {
        const q = searchParams.get('q');
        if (q) setQuery(q);
    }, [searchParams]);

    // Close sort dropdown on outside click
    useEffect(() => {
        const handler = (e) => {
            if (sortRef.current && !sortRef.current.contains(e.target)) setShowSortMenu(false);
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    const isFiltered = debouncedQuery.trim().length >= 2 || debouncedLocation || debouncedBio || mutualOnly;

    useEffect(() => {
        if (!isFiltered) {
            setResults([]);
            setHasSearched(false);
            return;
        }

        let cancelled = false;
        setSearching(true);

        const doSearch = async () => {
            try {
                const res = await searchUsers({
                    query: debouncedQuery.trim(),
                    location: debouncedLocation.trim(),
                    bio: debouncedBio.trim(),
                    mutualOnly,
                    sort,
                });
                if (!cancelled) {
                    setResults(Array.isArray(res.data) ? res.data : []);
                    setHasSearched(true);
                }
            } catch {
                if (!cancelled) setResults([]);
            } finally {
                if (!cancelled) setSearching(false);
            }
        };

        doSearch();
        return () => { cancelled = true; };
    }, [debouncedQuery, debouncedLocation, debouncedBio, mutualOnly, sort]);

    const handleSelectUser = (user) => {
        const newRecent = [
            { id: user.id, name: user.name, profilePicUrl: user.profilePicUrl },
            ...recentSearches.filter(s => s.id !== user.id),
        ].slice(0, 10);
        setRecentSearches(newRecent);
        localStorage.setItem('recent_searches', JSON.stringify(newRecent));
        navigate(`/profile/${user.id}`);
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

    const clearAllFilters = () => {
        setLocation('');
        setBio('');
        setMutualOnly(false);
        setSort('relevance');
    };

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="max-w-[600px] mx-auto pt-4 md:pt-10 px-4"
        >
            {/* Search Input */}
            <div className="relative mb-3">
                <div className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)]">
                    <SearchIcon size={20} />
                </div>
                <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search for people..."
                    className="w-full pl-12 pr-24 py-4 bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-2xl text-[15px] focus:ring-2 focus:ring-[var(--accent)] outline-none transition-all shadow-xl shadow-black/5"
                    autoFocus
                />
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1">
                    {query && (
                        <button onClick={() => setQuery('')} className="p-1.5 text-[var(--text-muted)] hover:text-[var(--text-primary)]">
                            <X size={16} />
                        </button>
                    )}
                    <button
                        onClick={() => setShowFilters(f => !f)}
                        className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-[12px] font-semibold transition-all ${showFilters || activeFilterCount > 0
                            ? 'bg-[var(--accent)] text-white'
                            : 'bg-[var(--bg-hover)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                            }`}
                    >
                        <SlidersHorizontal size={13} />
                        {activeFilterCount > 0 ? activeFilterCount : 'Filter'}
                    </button>
                </div>
            </div>

            {/* Filter Panel */}
            <AnimatePresence>
                {showFilters && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="overflow-hidden mb-4"
                    >
                        <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-2xl p-4 space-y-3">
                            {/* Location */}
                            <div className="flex items-center gap-3">
                                <MapPin size={14} className="text-[var(--text-muted)] flex-shrink-0" />
                                <input
                                    type="text"
                                    value={location}
                                    onChange={(e) => setLocation(e.target.value)}
                                    placeholder="Filter by location..."
                                    className="input-field text-[13px] py-2"
                                />
                            </div>

                            {/* Bio / Interests */}
                            <div className="flex items-center gap-3">
                                <span className="text-[var(--text-muted)] flex-shrink-0 text-[13px]">#</span>
                                <input
                                    type="text"
                                    value={bio}
                                    onChange={(e) => setBio(e.target.value)}
                                    placeholder="Filter by interests (bio keyword)..."
                                    className="input-field text-[13px] py-2"
                                />
                            </div>

                            {/* Mutual + Sort row */}
                            <div className="flex items-center justify-between pt-1">
                                <button
                                    onClick={() => setMutualOnly(m => !m)}
                                    className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-[12px] font-semibold border transition-all ${mutualOnly
                                        ? 'bg-[var(--accent-light)] border-[var(--accent)] text-[var(--accent)]'
                                        : 'border-[var(--border-color)] text-[var(--text-secondary)] hover:border-[var(--border-hover)]'
                                        }`}
                                >
                                    <Users size={13} />
                                    Mutual friends only
                                </button>

                                {/* Sort */}
                                <div className="relative" ref={sortRef}>
                                    <button
                                        onClick={() => setShowSortMenu(s => !s)}
                                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[12px] font-semibold border border-[var(--border-color)] hover:border-[var(--border-hover)] text-[var(--text-secondary)] transition-all"
                                    >
                                        <ArrowUpDown size={12} />
                                        {SORT_OPTIONS.find(o => o.value === sort)?.label}
                                        <ChevronDown size={11} />
                                    </button>
                                    <AnimatePresence>
                                        {showSortMenu && (
                                            <motion.div
                                                initial={{ opacity: 0, y: -4, scale: 0.97 }}
                                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                                exit={{ opacity: 0, y: -4, scale: 0.97 }}
                                                transition={{ duration: 0.1 }}
                                                className="absolute right-0 top-9 w-44 bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl shadow-xl z-20 overflow-hidden"
                                            >
                                                {SORT_OPTIONS.map(o => (
                                                    <button
                                                        key={o.value}
                                                        onClick={() => { setSort(o.value); setShowSortMenu(false); }}
                                                        className={`w-full text-left px-4 py-2.5 text-[13px] transition-colors ${sort === o.value
                                                            ? 'text-[var(--accent)] bg-[var(--accent-light)] font-semibold'
                                                            : 'text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)]'
                                                            }`}
                                                    >
                                                        {o.label}
                                                    </button>
                                                ))}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>
                            </div>

                            {activeFilterCount > 0 && (
                                <button
                                    onClick={clearAllFilters}
                                    className="text-[11px] text-[var(--text-muted)] hover:text-[var(--danger)] transition-colors"
                                >
                                    Clear filters
                                </button>
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            <div className="space-y-6">
                {/* Results */}
                {isFiltered && (
                    <div className="space-y-2">
                        <div className="flex items-center justify-between px-1 mb-2">
                            <h3 className="text-[14px] font-bold text-[var(--text-primary)]">
                                {searching ? 'Searching...' : hasSearched ? `${results.length} result${results.length !== 1 ? 's' : ''}` : 'Results'}
                            </h3>
                            {searching && <SpinnerIcon />}
                        </div>
                        <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-2xl overflow-hidden shadow-sm">
                            {!searching && results.length === 0 && hasSearched ? (
                                <div className="p-8 text-center text-[14px] text-[var(--text-muted)]">
                                    No users found matching your filters
                                </div>
                            ) : (
                                results.map((user, idx) => (
                                    <motion.button
                                        key={user.id || idx}
                                        initial={{ opacity: 0, x: -8 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: idx * 0.03 }}
                                        onClick={() => handleSelectUser(user)}
                                        className="w-full flex items-center gap-4 p-4 hover:bg-[var(--bg-elevated)] transition-colors border-b last:border-none border-[var(--border-color)]/30 text-left group"
                                    >
                                        <div className="avatar w-12 h-12 text-[14px] flex-shrink-0">
                                            {user.profilePicUrl ? (
                                                <img src={user.profilePicUrl} alt={user.name || 'User'} className="w-full h-full object-cover rounded-full" />
                                            ) : (user.name?.charAt(0).toUpperCase() || '?')}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-[14px] font-bold text-[var(--text-primary)] truncate">{user.name || 'Unknown'}</p>
                                            <div className="flex items-center gap-3 mt-0.5 flex-wrap">
                                                {user.location && (
                                                    <span className="flex items-center gap-1 text-[11px] text-[var(--text-muted)]">
                                                        <MapPin size={10} /> {user.location}
                                                    </span>
                                                )}
                                                {user.mutualCount > 0 && (
                                                    <span className="flex items-center gap-1 text-[11px] text-[var(--accent)] font-medium">
                                                        <Users size={10} /> {user.mutualCount} mutual
                                                    </span>
                                                )}
                                                {user.bio && (
                                                    <span className="text-[11px] text-[var(--text-muted)] truncate max-w-[160px]">{user.bio}</span>
                                                )}
                                            </div>
                                        </div>
                                        <ArrowRight size={16} className="text-[var(--text-muted)] opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all flex-shrink-0" />
                                    </motion.button>
                                ))
                            )}
                        </div>
                    </div>
                )}

                {/* Recent Searches */}
                {!isFiltered && (
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
                                <p className="text-[12px] text-[var(--text-muted)] max-w-[200px]">Search by name, location, or interests to find people.</p>
                            </div>
                        ) : (
                            <div className="space-y-1">
                                {recentSearches.map((s) => (
                                    <div key={s.id} className="flex items-center gap-3 p-2 group">
                                        <button onClick={() => navigate(`/profile/${s.id}`)} className="flex-1 flex items-center gap-3 text-left">
                                            <div className="avatar w-10 h-10 text-[12px] flex-shrink-0">
                                                {s.profilePicUrl ? (
                                                    <img src={s.profilePicUrl} alt={s.name || 'User'} className="w-full h-full object-cover rounded-full" />
                                                ) : s.name?.charAt(0).toUpperCase()}
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

function SpinnerIcon() {
    return (
        <svg className="animate-spin w-4 h-4 text-[var(--accent)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
    );
}

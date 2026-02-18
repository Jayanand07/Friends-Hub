import { useState, useEffect, useCallback, useRef } from 'react';
import { useOutletContext } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCw } from 'lucide-react';
import { getAllPosts } from '../api/posts';
import { useAuth } from '../context/AuthContext';
import PostCard from '../components/PostCard';
import CreatePostModal from '../components/CreatePostModal';
import SkeletonPost from '../components/SkeletonPost';
import StoriesBar from '../components/StoriesBar';
import InfiniteScrollWrapper from '../components/ui/InfiniteScrollWrapper';

export default function HomePage() {
    const { user } = useAuth();
    const { setShowCreate: setLayoutCreate } = useOutletContext() || {};
    const [posts, setPosts] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [showCreate, setShowCreate] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const sentinelRef = useRef(null);
    const fetchingRef = useRef(false);

    const fetchPosts = useCallback(async (pageNum = 0, append = false) => {
        if (fetchingRef.current) return;
        fetchingRef.current = true;

        if (!append) setLoading(true);
        else setLoadingMore(true);

        try {
            const res = await getAllPosts(pageNum, 8);
            const newPosts = res.data.content || [];
            const totalPages = res.data.totalPages || 0;

            setPosts((prev) => {
                if (!append) return newPosts;
                // Deduplicate
                const existingIds = new Set(prev.map(p => p.id));
                const unique = newPosts.filter(p => !existingIds.has(p.id));
                return [...prev, ...unique];
            });
            setPage(pageNum);
            setHasMore(pageNum < totalPages - 1);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
            setLoadingMore(false);
            setRefreshing(false);
            fetchingRef.current = false;
        }
    }, []);

    useEffect(() => {
        fetchPosts(0);
    }, [fetchPosts]);

    // Refresh events
    useEffect(() => {
        const handler = () => fetchPosts(0);
        window.addEventListener('refreshFeed', handler);
        return () => window.removeEventListener('refreshFeed', handler);
    }, [fetchPosts]);



    const handleDelete = (id) => {
        setPosts((prev) => prev.filter((p) => p.id !== id));
    };

    const handleRefresh = () => {
        setRefreshing(true);
        fetchPosts(0);
    };

    return (
        <div className="max-w-[470px] mx-auto">
            {/* Stories */}
            <StoriesBar />

            {/* Pull-to-refresh indicator */}
            <AnimatePresence>
                {refreshing && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 40, opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="flex items-center justify-center"
                    >
                        <RefreshCw size={16} className="text-[var(--text-muted)] animate-spin" />
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Skeleton Loading */}
            {loading && posts.length === 0 ? (
                <div>
                    <SkeletonPost />
                    <SkeletonPost />
                    <SkeletonPost />
                </div>
            ) : posts.length === 0 ? (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="text-center py-20"
                >
                    <div className="w-24 h-24 rounded-full border-2 border-[var(--border-color)] flex items-center justify-center mx-auto mb-4">
                        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" className="text-[var(--text-muted)]">
                            <rect x="2" y="2" width="20" height="20" rx="5" />
                            <circle cx="12" cy="12" r="5" />
                            <circle cx="17.5" cy="6.5" r="1.5" />
                        </svg>
                    </div>
                    <p className="text-[var(--text-primary)] text-xl font-light mb-1">Share Photos</p>
                    <p className="text-[13px] text-[var(--text-muted)] mb-4">When you share photos, they'll appear here.</p>
                    <button onClick={() => setShowCreate(true)} className="text-[var(--accent)] text-[13px] font-semibold cursor-pointer hover:text-[var(--accent-hover)]">
                        Share your first photo
                    </button>
                </motion.div>
            ) : (
                <InfiniteScrollWrapper
                    onLoadMore={() => fetchPosts(page + 1, true)}
                    hasMore={hasMore}
                    loading={loadingMore}
                >
                    {posts.map((post) => (
                        <PostCard key={post.id} post={post} currentEmail={user?.email} onDelete={handleDelete} />
                    ))}
                </InfiniteScrollWrapper>
            )}

            {/* End of feed */}
            {!hasMore && posts.length > 0 && (
                <div className="text-center py-8 border-t border-[var(--border-color)]">
                    <div className="inline-flex items-center gap-2 text-[var(--text-muted)]">
                        <div className="w-8 h-[1px] bg-[var(--border-color)]" />
                        <span className="text-[12px]">You're all caught up</span>
                        <div className="w-8 h-[1px] bg-[var(--border-color)]" />
                    </div>
                </div>
            )}

            {/* Desktop create modal */}
            <CreatePostModal open={showCreate} onClose={() => setShowCreate(false)} onPostCreated={() => fetchPosts(0)} />
        </div>
    );
}

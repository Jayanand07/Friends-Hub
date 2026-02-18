import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Send, Loader } from 'lucide-react';
import { getComments, addComment } from '../api/posts';
import { useToast } from './Toast';

function timeAgo(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '';
    const diff = Date.now() - date.getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'now';
    if (mins < 60) return `${mins}m`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h`;
    return `${Math.floor(hours / 24)}d`;
}

export default function CommentSection({ postId, onCommentAdded }) {
    const [comments, setComments] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const [text, setText] = useState('');
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const toast = useToast();

    const fetchComments = async (pageNum = 0, append = false) => {
        try {
            if (append) setLoadingMore(true);
            else setFetching(true);

            const res = await getComments(postId, pageNum);
            const newComments = res.data.content || [];

            setComments(prev => append ? [...prev, ...newComments] : newComments);
            setHasMore(!res.data.last);
            setPage(pageNum);
        } catch (err) {
            console.error(err);
        } finally {
            setFetching(false);
            setLoadingMore(false);
        }
    };

    useEffect(() => {
        fetchComments(0);
    }, [postId]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!text.trim()) return;
        setLoading(true);
        try {
            await addComment(postId, { content: text });
            setText('');
            onCommentAdded?.();
            await fetchComments(0); // Refresh list
        } catch {
            toast.error('Failed to add comment');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="border-t border-[var(--border-color)]">
            <div className="max-h-60 overflow-y-auto px-4 py-2 space-y-2">
                {fetching ? (
                    <div className="flex justify-center py-4">
                        <Loader size={16} className="text-[var(--text-muted)] animate-spin" />
                    </div>
                ) : comments.length === 0 ? (
                    <p className="text-[13px] text-[var(--text-muted)] text-center py-3">No comments yet</p>
                ) : (
                    comments.map((c, idx) => (
                        <motion.div
                            key={c.id || idx}
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            transition={{ delay: idx * 0.03 }}
                            className="flex gap-2.5 items-start"
                        >
                            <div className="avatar w-7 h-7 text-[9px] flex-shrink-0 mt-0.5">
                                {c.authorProfilePic ? (
                                    <img src={c.authorProfilePic} alt="" className="w-full h-full object-cover rounded-full" />
                                ) : (c.authorName?.charAt(0)?.toUpperCase() || '?')}
                            </div>
                            <div className="flex-1 min-w-0">
                                <p className="text-[13px] text-[var(--text-primary)] leading-snug">
                                    <span className="font-semibold mr-1.5">{c.authorName}</span>
                                    <span className="text-[var(--text-secondary)] font-normal">{c.content}</span>
                                </p>
                                <div className="flex gap-3 mt-0.5">
                                    <span className="text-[11px] text-[var(--text-muted)]">{timeAgo(c.createdAt)}</span>
                                </div>
                            </div>
                        </motion.div>
                    ))
                )}

                {hasMore && (
                    <button
                        onClick={() => fetchComments(page + 1, true)}
                        disabled={loadingMore}
                        className="w-full text-xs text-[var(--text-muted)] hover:text-[var(--accent)] py-2 text-center"
                    >
                        {loadingMore ? 'Loading...' : 'View more comments'}
                    </button>
                )}
            </div>

            <form onSubmit={handleSubmit} className="flex items-center gap-2 px-4 py-3 border-t border-[var(--border-color)]">
                <input
                    type="text"
                    className="flex-1 bg-transparent text-[13px] text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none"
                    placeholder="Add a comment..."
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                />
                <button
                    type="submit"
                    disabled={loading || !text.trim()}
                    className={`text-[13px] font-semibold cursor-pointer transition-colors ${text.trim() ? 'text-[var(--accent)] hover:text-[var(--accent-hover)]' : 'text-[var(--accent)]/30'}`}
                >
                    {loading ? <Loader size={14} className="animate-spin" /> : 'Post'}
                </button>
            </form>
        </div >
    );
}

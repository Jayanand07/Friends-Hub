import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Heart, MessageCircle, Trash2, MoreHorizontal, Share2, Bookmark, Send } from 'lucide-react';
import { toggleLike, deletePost } from '../api/posts';
import { useToast } from './Toast';
import CommentSection from './CommentSection';
import EmojiReactionPicker from './EmojiReactionPicker';

function timeAgo(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '';
    const diff = Date.now() - date.getTime();
    const secs = Math.floor(diff / 1000);
    if (secs < 60) return 'Just now';
    const mins = Math.floor(secs / 60);
    if (mins < 60) return `${mins}m`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d`;
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export default function PostCard({ post, currentEmail, onDelete }) {
    const [liked, setLiked] = useState(false);
    const [likeCount, setLikeCount] = useState(post.likeCount || 0);
    const [showComments, setShowComments] = useState(false);
    const [commentCount, setCommentCount] = useState(post.commentCount || 0);
    const [showMenu, setShowMenu] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [heartBurst, setHeartBurst] = useState(false);
    const [showFloatHeart, setShowFloatHeart] = useState(false);
    const toast = useToast();
    const lastTap = useRef(0);

    const initial = post.authorName?.charAt(0)?.toUpperCase() || '?';
    const profilePic = post.authorProfilePic || post.profilePicUrl;

    const handleLike = async () => {
        setLiked(!liked);
        setLikeCount((c) => (liked ? c - 1 : c + 1));
        if (!liked) {
            setHeartBurst(true);
            setTimeout(() => setHeartBurst(false), 500);
        }
        try {
            await toggleLike(post.id);
        } catch {
            setLiked(liked);
            setLikeCount((c) => (liked ? c + 1 : c - 1));
            toast.error('Failed to update like');
        }
    };

    // Double-tap to like (Instagram-style)
    const handleDoubleTap = () => {
        const now = Date.now();
        if (now - lastTap.current < 300) {
            if (!liked) handleLike();
            setShowFloatHeart(true);
            setTimeout(() => setShowFloatHeart(false), 800);
        }
        lastTap.current = now;
    };

    const handleDelete = async () => {
        setDeleting(true);
        try {
            await deletePost(post.id);
            toast.success('Post deleted');
            onDelete?.(post.id);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to delete');
            setDeleting(false);
        }
    };

    return (
        <motion.div
            layout
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
            className="bg-[var(--bg-card)] border-b border-[var(--border-color)] sm:border sm:rounded-lg sm:mb-3"
        >
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3">
                <Link to={`/profile/${post.authorId || post.userId}`} className="flex items-center gap-3 group">
                    <div className="avatar-story">
                        <div className="avatar w-[34px] h-[34px] text-[11px] group-hover:scale-105 transition-transform">
                            {profilePic ? (
                                <img src={profilePic} alt={post.authorName} className="w-full h-full object-cover rounded-full" />
                            ) : initial}
                        </div>
                    </div>
                    <div>
                        <p className="text-[13px] font-semibold text-[var(--text-primary)] leading-tight group-hover:underline">{post.authorName}</p>
                        <p className="text-[11px] text-[var(--text-muted)]">{timeAgo(post.createdAt)}</p>
                    </div>
                </Link>
                <div className="relative">
                    <button onClick={() => setShowMenu(!showMenu)} className="btn-icon w-8 h-8">
                        <MoreHorizontal size={18} className="text-[var(--text-secondary)]" />
                    </button>
                    <AnimatePresence>
                        {showMenu && (
                            <>
                                <div className="fixed inset-0 z-10" onClick={() => setShowMenu(false)} />
                                <motion.div
                                    initial={{ opacity: 0, scale: 0.9 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    exit={{ opacity: 0, scale: 0.9 }}
                                    className="absolute right-0 mt-1 bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-xl p-1 min-w-[140px] z-20 shadow-xl"
                                >
                                    <button
                                        onClick={() => { handleDelete(); setShowMenu(false); }}
                                        disabled={deleting}
                                        className="w-full flex items-center gap-2 px-3 py-2 text-[13px] text-[var(--danger)] hover:bg-[var(--danger)]/10 rounded-lg cursor-pointer disabled:opacity-50 transition-colors"
                                    >
                                        <Trash2 size={13} /> {deleting ? 'Deleting...' : 'Delete'}
                                    </button>
                                </motion.div>
                            </>
                        )}
                    </AnimatePresence>
                </div>
            </div>

            {/* Content text (before image, like IG captions can be before or after) */}
            {post.content && !post.imageUrl && (
                <div className="px-4 pb-3">
                    <p className="text-[14px] text-[var(--text-primary)] leading-relaxed whitespace-pre-wrap">{post.content}</p>
                </div>
            )}

            {/* Image — edge-to-edge like Instagram */}
            {post.imageUrl && (
                <div className="relative" onClick={handleDoubleTap}>
                    <img
                        src={post.imageUrl}
                        alt="Post"
                        className="w-full object-cover max-h-[520px]"
                        onError={(e) => { e.target.parentElement.style.display = 'none'; }}
                    />
                    {/* Double-tap heart animation */}
                    <AnimatePresence>
                        {showFloatHeart && (
                            <motion.div
                                initial={{ scale: 0, opacity: 1 }}
                                animate={{ scale: 1.2, opacity: 0 }}
                                exit={{ opacity: 0 }}
                                transition={{ duration: 0.6 }}
                                className="absolute inset-0 flex items-center justify-center pointer-events-none"
                            >
                                <Heart size={80} fill="white" className="text-white drop-shadow-2xl" />
                            </motion.div>
                        )}
                    </AnimatePresence>
                </div>
            )}

            {/* Action buttons — Instagram style */}
            <div className="px-4 py-2 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <motion.button
                        whileTap={{ scale: 0.75 }}
                        onClick={handleLike}
                        className="p-1 cursor-pointer"
                    >
                        <Heart
                            size={24}
                            fill={liked ? '#ed4956' : 'none'}
                            className={`transition-colors ${liked ? 'text-[var(--danger)]' : 'text-[var(--text-primary)] hover:text-[var(--text-muted)]'} ${heartBurst ? 'heart-burst' : ''}`}
                        />
                    </motion.button>
                    <button onClick={() => setShowComments(!showComments)} className="p-1 cursor-pointer">
                        <MessageCircle size={24} className={`transition-colors ${showComments ? 'text-[var(--accent)]' : 'text-[var(--text-primary)] hover:text-[var(--text-muted)]'}`} />
                    </button>
                    <button className="p-1 cursor-pointer">
                        <Send size={22} className="text-[var(--text-primary)] hover:text-[var(--text-muted)] transition-colors -rotate-12" />
                    </button>
                    <EmojiReactionPicker targetType="POST" targetId={post.id} />
                </div>
                <button className="p-1 cursor-pointer">
                    <Bookmark size={24} className="text-[var(--text-primary)] hover:text-[var(--text-muted)] transition-colors" />
                </button>
            </div>

            {/* Like count + caption */}
            <div className="px-4 pb-2">
                <p className="text-[13px] font-semibold text-[var(--text-primary)] mb-1">
                    {likeCount.toLocaleString()} {likeCount === 1 ? 'like' : 'likes'}
                </p>
                {post.content && post.imageUrl && (
                    <p className="text-[13px] text-[var(--text-primary)] leading-snug">
                        <Link to={`/profile/${post.authorId || post.userId}`} className="font-semibold mr-1.5 hover:underline">{post.authorName}</Link>
                        <span className="text-[var(--text-secondary)]">{post.content}</span>
                    </p>
                )}
                {commentCount > 0 && !showComments && (
                    <button onClick={() => setShowComments(true)} className="text-[13px] text-[var(--text-muted)] mt-1 cursor-pointer hover:text-[var(--text-secondary)]">
                        View all {commentCount} comments
                    </button>
                )}
            </div>

            {/* Comments */}
            <AnimatePresence>
                {showComments && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="overflow-hidden"
                    >
                        <CommentSection postId={post.id} onCommentAdded={() => setCommentCount((c) => c + 1)} />
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}

import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Heart, MessageCircle, Trash2, MoreHorizontal, Bookmark, Send } from 'lucide-react';
import { toggleLike, toggleSavePost, deletePost } from '../api/posts';
import { useToast } from './Toast';
import CommentSection from './CommentSection';
import EmojiReactionPicker from './EmojiReactionPicker';
import timeAgo from '../utils/timeAgo';

export default function PostCard({ post, currentEmail, currentUserId, onDelete, onLikeToggle }) {
    const toast = useToast();
    const [liked, setLiked] = useState(post.liked || post.isLiked);
    const [saved, setSaved] = useState(post.saved || post.isSaved || false);
    const [likeCount, setLikeCount] = useState(post.likeCount || post.likesCount || 0);
    const [commentCount, setCommentCount] = useState(post.commentCount || post.commentsCount || 0);
    const [showComments, setShowComments] = useState(false);
    const [showMenu, setShowMenu] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [showFloatHeart, setShowFloatHeart] = useState(false);
    const [heartBurst, setHeartBurst] = useState(false);
    const [imageFailed, setImageFailed] = useState(false);
    const lastTap = useRef(0);

    // Synchronize local liked/likeCount/saved with incoming post prop updates
    useEffect(() => {
        setLiked(post.liked || post.isLiked);
        setSaved(post.saved || post.isSaved || false);
        setLikeCount(post.likeCount || post.likesCount || 0);
    }, [post.id, post.liked, post.isLiked, post.saved, post.isSaved, post.likeCount, post.likesCount]);

    const initial = post.authorName?.charAt(0)?.toUpperCase() || '?';
    const profilePic = post.authorProfilePic || post.profilePicUrl;
    const isOwner = currentUserId != null && String(post.authorId ?? post.userId) === String(currentUserId);

    const handleLike = async () => {
        const wasLiked = liked;
        const newLiked = !wasLiked;
        const newCount = wasLiked ? Math.max(0, likeCount - 1) : likeCount + 1;
        setLiked(newLiked);
        setLikeCount(newCount);
        if (onLikeToggle) onLikeToggle(post.id, newLiked, newCount);
        if (!wasLiked) {
            setHeartBurst(true);
            setTimeout(() => setHeartBurst(false), 500);
        }
        try {
            await toggleLike(post.id);
        } catch {
            setLiked(wasLiked);
            setLikeCount(likeCount);
            if (onLikeToggle) onLikeToggle(post.id, wasLiked, likeCount);
            toast.error('Failed to update like');
        }
    };

    const handleSave = async () => {
        const wasSaved = saved;
        setSaved(!wasSaved);
        try {
            await toggleSavePost(post.id);
            toast.success(wasSaved ? 'Post removed from saved' : 'Post saved');
        } catch {
            setSaved(wasSaved);
            toast.error('Failed to update saved post');
        }
    };

    const handleDoubleTap = () => {
        const now = Date.now();
        if (now - lastTap.current < 300) {
            const wasLiked = liked;
            if (!wasLiked) handleLike();
            setShowFloatHeart(true);
            setTimeout(() => setShowFloatHeart(false), 800);
        }
        lastTap.current = now;
    };

    const handleShare = async () => {
        const shareUrl = `${window.location.origin}/?post=${post.id}`;
        const shareData = {
            title: `${post.authorName} on Friends-Hub`,
            text: post.content || 'Check out this post on Friends-Hub',
            url: shareUrl,
        };
        try {
            if (navigator.share) {
                await navigator.share(shareData);
            } else {
                await navigator.clipboard.writeText(shareUrl);
                toast.success('Link copied to clipboard');
            }
        } catch (err) {
            if (err.name !== 'AbortError') {
                toast.error('Failed to share post');
            }
        }
    };

    const handleDelete = async () => {
        if (!isOwner || deleting) return;

        setDeleting(true);
        try {
            await deletePost(post.id);
            onDelete?.(post.id);
            toast.success('Post deleted');
        } catch {
            toast.error('Failed to delete post');
        } finally {
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
                    <div className="avatar w-[34px] h-[34px] text-[11px] group-hover:scale-105 transition-transform overflow-hidden rounded-full">
                        {profilePic ? (
                            <img src={profilePic} alt={post.authorName} className="w-full h-full object-cover rounded-full" />
                        ) : initial}
                    </div>
                    <div>
                        <p className="text-[13px] font-semibold text-[var(--text-primary)] leading-tight group-hover:underline">{post.authorName}</p>
                        <p className="text-[11px] text-[var(--text-muted)]">{timeAgo(post.createdAt)}</p>
                    </div>
                </Link>
                {isOwner && (
                    <div className="relative">
                        <button onClick={() => setShowMenu(!showMenu)} className="btn-icon w-8 h-8" aria-label="Post menu">
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
                )}
            </div>

            {/* Content text (before image, like IG captions can be before or after) */}
            {post.content && !post.imageUrl && (
                <div className="px-4 pb-3">
                    <p className="text-[14px] text-[var(--text-primary)] leading-relaxed whitespace-pre-wrap">{post.content}</p>
                </div>
            )}

            {/* Image — edge-to-edge like Instagram */}
            {post.imageUrl && !imageFailed && (
                <div className="relative" onClick={handleDoubleTap}>
                    <img
                        src={post.imageUrl}
                        alt="Post"
                        className="w-full object-cover max-h-[520px]"
                        onError={() => setImageFailed(true)}
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
                        aria-label={liked ? 'Unlike' : 'Like'}
                    >
                        <Heart
                            size={24}
                            fill={liked ? '#ed4956' : 'none'}
                            className={`transition-colors ${liked ? 'text-[var(--danger)]' : 'text-[var(--text-primary)] hover:text-[var(--text-muted)]'} ${heartBurst ? 'heart-burst' : ''}`}
                        />
                    </motion.button>
                    <button onClick={() => setShowComments(!showComments)} className="p-1 cursor-pointer" aria-label={showComments ? 'Close comments' : 'Open comments'}>
                        <MessageCircle size={24} className={`transition-colors ${showComments ? 'text-[var(--accent)]' : 'text-[var(--text-primary)] hover:text-[var(--text-muted)]'}`} />
                    </button>
                    <button onClick={handleShare} className="p-1 cursor-pointer" aria-label="Share post">
                        <Send size={22} className="text-[var(--text-primary)] hover:text-[var(--text-muted)] transition-colors -rotate-12" />
                    </button>
                    <EmojiReactionPicker targetType="POST" targetId={post.id} />
                </div>
                <button onClick={handleSave} className="p-1 cursor-pointer" aria-label={saved ? 'Unsave post' : 'Save post'}>
                    <Bookmark
                        size={24}
                        fill={saved ? 'currentColor' : 'none'}
                        className={`transition-colors ${saved ? 'text-[var(--accent)]' : 'text-[var(--text-primary)] hover:text-[var(--text-muted)]'}`}
                    />
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
                        <CommentSection
                            postId={post.id}
                            currentUserId={currentUserId}
                            onCommentAdded={() => setCommentCount((c) => c + 1)}
                            onCommentDeleted={() => setCommentCount((c) => Math.max(0, c - 1))}
                        />
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}

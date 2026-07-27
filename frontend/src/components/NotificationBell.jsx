import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bell, Heart, MessageSquare, UserPlus, Send, Check, X } from 'lucide-react';
import { getNotifications, getUnreadCount, markNotificationsRead, markNotificationRead } from '../api/notifications';
import { acceptFollowRequestFromUser, rejectFollowRequestFromUser } from '../api/users';
import { useNavigate } from 'react-router-dom';
import { useToast } from './Toast';
import timeAgo from '../utils/timeAgo';

const typeIcons = {
    LIKE: { icon: Heart, color: 'text-rose-400', bg: 'bg-rose-400/10' },
    COMMENT: { icon: MessageSquare, color: 'text-blue-400', bg: 'bg-blue-400/10' },
    FOLLOW: { icon: UserPlus, color: 'text-emerald-400', bg: 'bg-emerald-400/10' },
    FOLLOW_REQUEST: { icon: UserPlus, color: 'text-[var(--accent)]', bg: 'bg-[var(--accent-light)]' },
    MESSAGE: { icon: Send, color: 'text-purple-400', bg: 'bg-purple-400/10' },
};

export default function NotificationBell({ pendingNotifications }) {
    const [open, setOpen] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const dropdownRef = useRef(null);
    const processedCountRef = useRef(0);
    const navigate = useNavigate();
    const toast = useToast();

    // Fetch unread count on mount
    useEffect(() => {
        getUnreadCount()
            .then((res) => setUnreadCount(res.data?.count || 0))
            .catch(() => toast.error('Failed to load notifications'));
    }, []);

    // Handle new realtime notifications — process all new items from the queue
    useEffect(() => {
        if (pendingNotifications.length > processedCountRef.current) {
            const newItems = pendingNotifications.slice(processedCountRef.current);
            setUnreadCount((c) => c + newItems.length);
            setNotifications((prev) => [...newItems.reverse(), ...prev]);
            processedCountRef.current = pendingNotifications.length;
        }
    }, [pendingNotifications]);

    // Fetch full list when dropdown opens
    useEffect(() => {
        if (!open) return;
        setLoading(true);
        getNotifications()
            .then((res) => setNotifications(Array.isArray(res.data) ? res.data : []))
            .catch(() => toast.error('Failed to load notifications'))
            .finally(() => setLoading(false));
    }, [open]);

    // Close on outside click
    useEffect(() => {
        const handleClick = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, []);

    const handleMarkRead = async () => {
        const prevUnreadCount = unreadCount;
        const prevNotifications = notifications.map(n => ({ ...n }));
        try {
            await markNotificationsRead();
            setUnreadCount(0);
            setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
        } catch {
            setUnreadCount(prevUnreadCount);
            setNotifications(prevNotifications);
        }
    };

    const handleMarkOneAsRead = async (e, notificationId) => {
        e.stopPropagation();
        const prevUnreadCount = unreadCount;
        const prevNotifications = notifications.map(n => ({ ...n }));
        setNotifications(prev => prev.map(n =>
            n.id === notificationId ? { ...n, isRead: true } : n
        ));
        setUnreadCount(c => Math.max(0, c - 1));
        try {
            await markNotificationRead(notificationId);
        } catch (error) {
            setUnreadCount(prevUnreadCount);
            setNotifications(prevNotifications);
            console.error(error);
        }
    };

    const handleAccept = async (e, notification) => {
        e.stopPropagation();
        if (!notification.actorId && !notification.actor?.id) return;
        const targetActorId = notification.actorId || notification.actor?.id;
        const prevUnreadCount = unreadCount;
        const prevNotifications = notifications.map(n => ({ ...n }));
        setNotifications(prev => prev.map(n =>
            n.id === notification.id ? { ...n, isRead: true, content: 'Request accepted' } : n
        ));
        setUnreadCount(c => Math.max(0, c - 1));
        try {
            await acceptFollowRequestFromUser(targetActorId);
        } catch (error) {
            setUnreadCount(prevUnreadCount);
            setNotifications(prevNotifications);
            console.error(error);
        }
    };

    const handleReject = async (e, notification) => {
        e.stopPropagation();
        if (!notification.actorId && !notification.actor?.id) return;
        const targetActorId = notification.actorId || notification.actor?.id;
        const prevUnreadCount = unreadCount;
        const prevNotifications = notifications.map(n => ({ ...n }));
        setNotifications(prev => prev.map(n =>
            n.id === notification.id ? { ...n, isRead: true, content: 'Request removed' } : n
        ));
        setUnreadCount(c => Math.max(0, c - 1));
        try {
            await rejectFollowRequestFromUser(targetActorId);
        } catch (error) {
            setUnreadCount(prevUnreadCount);
            setNotifications(prevNotifications);
            console.error(error);
        }
    };

    const handleNotificationClick = async (notification) => {
        const prevUnreadCount = unreadCount;
        const prevNotifications = notifications.map(n => ({ ...n }));
        if (!notification.isRead) {
            setNotifications(prev => prev.map(n =>
                n.id === notification.id ? { ...n, isRead: true } : n
            ));
            setUnreadCount(c => Math.max(0, c - 1));
            try {
                await markNotificationRead(notification.id);
            } catch (error) {
                setUnreadCount(prevUnreadCount);
                setNotifications(prevNotifications);
                console.error(error);
            }
        }

        setOpen(false);

        switch (notification.type) {
            case 'FOLLOW':
            case 'FOLLOW_REQUEST':
                if (notification.actorId) navigate(`/profile/${notification.actorId}`);
                break;
            case 'MESSAGE':
                if (notification.actorId) navigate(`/chat?userId=${notification.actorId}`);
                break;
            case 'LIKE':
            case 'COMMENT':
                break;
            default:
                break;
        }
    };

    return (
        <div className="relative" ref={dropdownRef}>
            {/* Bell button */}
            <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.9 }}
                onClick={() => setOpen(!open)}
                className="relative btn-icon w-9 h-9 border-0 cursor-pointer"
            >
                <Bell size={18} className="text-[var(--text-secondary)]" />
                <AnimatePresence>
                    {unreadCount > 0 && (
                        <motion.span
                            initial={{ scale: 0 }}
                            animate={{ scale: 1 }}
                            exit={{ scale: 0 }}
                            className="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[9px] font-bold flex items-center justify-center"
                        >
                            {unreadCount > 99 ? '99+' : unreadCount}
                        </motion.span>
                    )}
                </AnimatePresence>
            </motion.button>

            {/* Dropdown */}
            <AnimatePresence>
                {open && (
                    <motion.div
                        initial={{ opacity: 0, y: -8, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: -8, scale: 0.95 }}
                        transition={{ duration: 0.15 }}
                        className="absolute right-0 top-12 w-80 max-h-96 bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl shadow-2xl shadow-black/20 overflow-hidden z-50"
                    >
                        {/* Header */}
                        <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--border-color)]">
                            <h3 className="text-sm font-bold text-[var(--text-primary)]">Notifications</h3>
                            <div className="flex items-center gap-2">
                                {notifications.length > 0 && (
                                    <button
                                        onClick={handleMarkRead}
                                        className="text-[11px] font-semibold text-[var(--accent)] hover:underline cursor-pointer flex items-center gap-1"
                                    >
                                        <Check size={12} />
                                        Mark all read
                                    </button>
                                )}
                                <button onClick={() => setOpen(false)} className="btn-icon w-6 h-6 border-0 cursor-pointer">
                                    <X size={12} />
                                </button>
                            </div>
                        </div>

                        {/* List */}
                        <div className="overflow-y-auto max-h-80">
                            {loading && (
                                <div className="flex justify-center py-8">
                                    <div className="w-5 h-5 border-2 border-[var(--accent)] border-t-transparent rounded-full animate-spin" />
                                </div>
                            )}
                            {!loading && notifications.length === 0 && (
                                <div className="text-center py-10 px-4">
                                    <Bell size={24} className="text-[var(--text-muted)] opacity-30 mx-auto mb-2" />
                                    <p className="text-[12px] text-[var(--text-muted)]">No notifications yet</p>
                                </div>
                            )}
                            {!loading &&
                                notifications.map((n, idx) => {
                                    const typeInfo = typeIcons[n.type] || typeIcons.MESSAGE;
                                    const IconComp = typeInfo.icon;
                                    return (
                                        <motion.div
                                            key={n.id || idx}
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            transition={{ delay: idx * 0.02 }}
                                            onClick={() => handleNotificationClick(n)}
                                            className={`flex items-start gap-3 px-4 py-3 border-b border-[var(--border-color)]/50 transition-colors cursor-pointer ${!n.isRead ? 'bg-[var(--accent-light)]' : 'hover:bg-[var(--bg-elevated)]'
                                                }`}
                                        >
                                            <div className={`w-8 h-8 rounded-lg ${typeInfo.bg} flex items-center justify-center flex-shrink-0`}>
                                                <IconComp size={14} className={typeInfo.color} />
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <p className="text-[12px] text-[var(--text-primary)] leading-snug">{n.content}</p>
                                                <p className="text-[10px] text-[var(--text-muted)] mt-0.5">{timeAgo(n.createdAt)}</p>

                                                {n.type === 'FOLLOW_REQUEST' && !n.isRead && n.content !== 'Request accepted' && n.content !== 'Request removed' && (
                                                    <div className="flex gap-2 mt-2">
                                                        <button
                                                            onClick={(e) => handleAccept(e, n)}
                                                            className="px-3 py-1 bg-[var(--accent)] text-white text-[11px] font-semibold rounded-lg hover:bg-[var(--accent-hover)] transition-colors cursor-pointer"
                                                        >
                                                            Confirm
                                                        </button>
                                                        <button
                                                            onClick={(e) => handleReject(e, n)}
                                                            className="px-3 py-1 bg-[var(--bg-elevated)] text-[var(--text-primary)] border border-[var(--border-color)] text-[11px] font-semibold rounded-lg hover:bg-[var(--bg-hover)] transition-colors cursor-pointer"
                                                        >
                                                            Delete
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                            {!n.isRead && (
                                                <div className="flex items-center gap-1.5 flex-shrink-0 mt-1">
                                                    <button
                                                        onClick={(e) => handleMarkOneAsRead(e, n.id)}
                                                        className="p-1 rounded-md hover:bg-[var(--bg-card)] text-[var(--accent)] transition-colors cursor-pointer"
                                                        title="Mark as read"
                                                    >
                                                        <Check size={13} />
                                                    </button>
                                                    <div className="w-2 h-2 rounded-full bg-[var(--accent)]" />
                                                </div>
                                            )}
                                        </motion.div>
                                    );
                                })}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

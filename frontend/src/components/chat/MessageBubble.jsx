import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, CheckCheck, Trash2, Image as ImageIcon } from 'lucide-react';

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
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

export default function MessageBubble({ message, isOwn, onDelete }) {
    const [showMenu, setShowMenu] = useState(false);
    const isDeleted = message.isDeleted;
    const hasImage = message.imageUrl && !isDeleted;

    return (
        <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.2 }}
            className={`flex ${isOwn ? 'justify-end' : 'justify-start'} mb-1.5`}
        >
            <div
                className="max-w-[75%] group relative"
                onMouseEnter={() => isOwn && !isDeleted && setShowMenu(true)}
                onMouseLeave={() => setShowMenu(false)}
            >
                {/* Delete button (own messages only) */}
                <AnimatePresence>
                    {showMenu && isOwn && !isDeleted && (
                        <motion.button
                            initial={{ opacity: 0, scale: 0.8 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.8 }}
                            onClick={() => onDelete?.(message.id)}
                            className="absolute -left-8 top-1/2 -translate-y-1/2 w-6 h-6 rounded-full bg-red-500/10 hover:bg-red-500/20 flex items-center justify-center transition-colors cursor-pointer z-10"
                        >
                            <Trash2 size={11} className="text-red-400" />
                        </motion.button>
                    )}
                </AnimatePresence>

                {/* Image preview */}
                {hasImage && (
                    <div className={`mb-1 rounded-xl overflow-hidden ${isOwn ? 'rounded-br-md' : 'rounded-bl-md'}`}>
                        <img
                            src={message.imageUrl}
                            alt="Shared"
                            className="max-w-full max-h-60 object-cover rounded-xl cursor-pointer hover:opacity-90 transition-opacity"
                            onClick={() => window.open(message.imageUrl, '_blank')}
                        />
                    </div>
                )}

                {/* Content bubble */}
                <div
                    className={`px-3.5 py-2 text-[13px] leading-relaxed ${isDeleted
                        ? 'bg-[var(--bg-elevated)]/50 text-[var(--text-muted)] italic rounded-2xl border border-[var(--border-color)] border-dashed'
                        : isOwn
                            ? 'bg-gradient-to-br from-[var(--accent)] to-purple-600 text-white rounded-2xl rounded-br-md'
                            : 'bg-[var(--bg-elevated)] text-[var(--text-primary)] rounded-2xl rounded-bl-md border border-[var(--border-color)]'
                        }`}
                >
                    {isDeleted ? (
                        <span className="flex items-center gap-1.5">
                            <Trash2 size={11} /> This message was deleted
                        </span>
                    ) : (
                        message.content
                    )}
                </div>

                {/* Timestamp + read receipts */}
                <div className={`flex items-center gap-1 mt-0.5 px-1 ${isOwn ? 'justify-end' : 'justify-start'}`}>
                    <span className="text-[9px] text-[var(--text-muted)] opacity-0 group-hover:opacity-100 transition-opacity">
                        {timeAgo(message.timestamp)}
                    </span>
                    {isOwn && !isDeleted && (
                        message.isRead ? (
                            <CheckCheck size={12} className="text-blue-400" />
                        ) : (
                            <Check size={10} className="text-[var(--text-muted)]" />
                        )
                    )}
                </div>
            </div>
        </motion.div>
    );
}

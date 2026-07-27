import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Smile, X } from 'lucide-react';
import { addReaction, getReactions } from '../api/reactions';
import { useToast } from './Toast';

const EMOJI_LIST = ['❤️', '🔥', '😂', '😍', '😢', '😡', '👏', '🎉', '💯', '🙌', '😎', '🤔', '💀', '🥺', '✨', '💪'];

export default function EmojiReactionPicker({ targetType, targetId, onReactionAdded }) {
    const [open, setOpen] = useState(false);
    const [reactions, setReactions] = useState([]);
    const pickerRef = useRef(null);
    const toast = useToast();

    useEffect(() => {
        loadReactions();
    }, [targetType, targetId]);

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (pickerRef.current && !pickerRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        if (open) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [open]);

    const loadReactions = async () => {
        try {
            const res = await getReactions(targetType, targetId);
            setReactions(res.data);
        } catch (err) {
            console.error(err);
        }
    };

    const handleEmojiClick = async (emoji) => {
        try {
            await addReaction(targetType, targetId, emoji);
            await loadReactions();
            onReactionAdded?.();
            setOpen(false);
        } catch (err) {
            console.error(err);
            toast.error('Failed to add reaction');
        }
    };

    // Count reactions by emoji
    const emojiCounts = reactions.reduce((acc, r) => {
        if (r.emoji) {
            acc[r.emoji] = (acc[r.emoji] || 0) + 1;
        }
        return acc;
    }, {});

    return (
        <div className="relative inline-flex items-center gap-1.5" ref={pickerRef}>
            {/* Reaction display bar */}
            {Object.entries(emojiCounts).length > 0 && (
                <div className="flex gap-1 mr-1">
                    {Object.entries(emojiCounts).slice(0, 5).map(([emoji, count]) => (
                        <motion.span
                            key={emoji}
                            initial={{ scale: 0 }}
                            animate={{ scale: 1 }}
                            className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded-full bg-[var(--bg-elevated)] text-xs cursor-default"
                        >
                            <span>{emoji}</span>
                            {count > 1 && <span className="text-[var(--text-muted)] text-[10px]">{count}</span>}
                        </motion.span>
                    ))}
                </div>
            )}

            {/* Emoji trigger */}
            <button
                onClick={() => setOpen(!open)}
                className="text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-colors cursor-pointer"
                aria-label="Open reaction picker"
            >
                <Smile size={18} />
            </button>

            {/* Picker popup */}
            <AnimatePresence>
                {open && (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.8, y: 10 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.8, y: 10 }}
                        className="absolute bottom-full right-0 mb-2 p-3 bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-xl shadow-2xl z-50"
                        style={{ minWidth: '200px' }}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-[var(--text-muted)] text-xs font-semibold">React</span>
                            <button onClick={() => setOpen(false)} className="text-[var(--text-muted)] hover:text-[var(--text-primary)] cursor-pointer">
                                <X size={12} />
                            </button>
                        </div>
                        <div className="grid grid-cols-8 gap-1">
                            {EMOJI_LIST.map(emoji => (
                                <motion.button
                                    key={emoji}
                                    whileHover={{ scale: 1.3 }}
                                    whileTap={{ scale: 0.9 }}
                                    onClick={() => handleEmojiClick(emoji)}
                                    className="w-7 h-7 flex items-center justify-center text-lg rounded hover:bg-[var(--bg-hover)] cursor-pointer transition-colors"
                                    aria-label={`React with ${emoji}`}
                                >
                                    {emoji}
                                </motion.button>
                            ))}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

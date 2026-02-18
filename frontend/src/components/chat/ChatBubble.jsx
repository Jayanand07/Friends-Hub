import { motion } from 'framer-motion';
import { format } from 'date-fns';
import { Link } from 'react-router-dom';

export default function ChatBubble({ message, isMe, onImageClick }) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ type: "spring", stiffness: 300, damping: 20 }}
            className={`flex ${isMe ? 'justify-end' : 'justify-start'} mb-4`}
        >
            <div className={`flex max-w-[75%] md:max-w-[60%] gap-2 ${isMe ? 'flex-row-reverse' : 'flex-row'}`}>
                {/* Avatar */}
                {!isMe && (
                    <Link to={`/profile/${message.senderId}`} className="flex-shrink-0 self-end mb-1 hover:opacity-80 transition-opacity">
                        <div className="w-8 h-8 rounded-full overflow-hidden bg-[var(--bg-elevated)]">
                            {message.senderProfilePic ? (
                                <img src={message.senderProfilePic} alt={message.senderName} className="w-full h-full object-cover" />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center text-[10px] font-bold text-[var(--text-muted)]">
                                    {message.senderName?.charAt(0).toUpperCase()}
                                </div>
                            )}
                        </div>
                    </Link>
                )}

                {/* Bubble */}
                <div className={`flex flex-col ${isMe ? 'items-end' : 'items-start'}`}>
                    {!isMe && (
                        <Link to={`/profile/${message.senderId}`} className="text-[11px] text-[var(--text-muted)] ml-1 mb-1 block hover:underline">
                            {message.senderName}
                        </Link>
                    )}

                    <div
                        className={`relative px-4 py-2.5 shadow-sm ${isMe
                            ? 'bg-gradient-to-br from-[var(--gradient-1)] to-[var(--gradient-2)] text-white rounded-2xl rounded-tr-sm'
                            : 'bg-[var(--bg-elevated)] text-[var(--text-primary)] border border-[var(--border-color)] rounded-2xl rounded-tl-sm'
                            }`}
                    >
                        {message.imageUrl && (
                            <div className="mb-2 rounded-lg overflow-hidden cursor-pointer" onClick={() => onImageClick(message.imageUrl)}>
                                <img src={message.imageUrl} alt="Shared" className="max-w-full h-auto max-h-[300px] object-cover hover:scale-105 transition-transform duration-300" />
                            </div>
                        )}
                        {message.content && <p className="text-[14px] leading-relaxed break-words whitespace-pre-wrap">{message.content}</p>}

                        <span className={`text-[9px] block mt-1 ${isMe ? 'text-white/70 text-right' : 'text-[var(--text-muted)] text-left'}`}>
                            {(() => {
                                try {
                                    const date = new Date(message.timestamp || message.createdAt);
                                    return !isNaN(date.getTime()) ? format(date, 'h:mm a') : '';
                                } catch {
                                    return '';
                                }
                            })()}
                        </span>
                    </div>
                </div>
            </div>
        </motion.div>
    );
}

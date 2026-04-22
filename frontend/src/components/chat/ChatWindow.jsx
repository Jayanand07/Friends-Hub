import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Send, ImagePlus, User, Loader, X, ArrowLeft, Smile } from 'lucide-react';
import ChatBubble from './ChatBubble';
import ImagePreviewModal from './ImagePreviewModal';
import { getMessages, sendMessageRest } from '../../api/chat';
import { uploadImage } from '../../api/posts';
import { sendChatMessage, sendTypingIndicator, isConnected } from '../../socket/chatSocket';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../Toast';

export default function ChatWindow({
    selectedUser,
    currentUserId,
    messages,
    setMessages,
    onBack,
    typingUser,
    onlineUsers
}) {
    const { user: currentUser } = useAuth();
    const [text, setText] = useState('');
    const [loading, setLoading] = useState(false);
    const [sending, setSending] = useState(false);
    const [imageFile, setImageFile] = useState(null);
    const [imagePreview, setImagePreview] = useState('');
    const [previewImage, setPreviewImage] = useState(null);
    const [showEmojis, setShowEmojis] = useState(false);
    const EMOJIS = ['😊', '😂', '❤️', '👍', '🔥', '😢', '😍', '👏', '🎉', '🤔', '🙌', '✨'];
    const messagesEndRef = useRef(null);
    const fileRef = useRef(null);
    const toast = useToast();

    // Fetch history when user changes
    useEffect(() => {
        if (!selectedUser?.id) return;

        setLoading(true);
        getMessages(selectedUser.id)
            .then(res => {
                setMessages(Array.isArray(res.data) ? res.data : []);
            })
            .catch(() => {
                toast.error("Failed to load chat history");
            })
            .finally(() => {
                setLoading(false);
            });
    }, [selectedUser?.id, setMessages]);

    // Scroll to bottom
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSend = async (e) => {
        e.preventDefault();
        if ((!text.trim() && !imageFile) || sending) return;

        setSending(true);
        const messageText = text.trim();
        try {
            let imageUrl = null;
            if (imageFile) {
                const uploadRes = await uploadImage(imageFile);
                imageUrl = uploadRes.data.imageUrl;
            }

            if (imageUrl || !isConnected()) {
                const res = await sendMessageRest(selectedUser.id, messageText, imageUrl);
                const sentMessage = res.data || {
                    id: Date.now(), 
                    senderId: currentUser?.id,
                    receiverId: selectedUser.id,
                    content: messageText,
                    imageUrl: imageUrl,
                    sentAt: new Date().toISOString(),
                    isRead: false,
                };
                setMessages((prev) => {
                    if (prev.some((m) => m.id === sentMessage.id)) return prev;
                    return [...prev, sentMessage];
                });
            } else {
                sendChatMessage(selectedUser.id, messageText, currentUser?.email, imageUrl);
            }

            setText('');
            setImageFile(null);
            setImagePreview('');
        } catch (error) {
            console.error(error);
            toast.error("Failed to send message");
        } finally {
            setSending(false);
        }
    };

    const handleTyping = (e) => {
        setText(e.target.value);
        // Throttle typing indicator
        // sendTypingIndicator(currentUserId, null, selectedUser.id);
    };

    const handleImageSelect = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 5 * 1024 * 1024) {
                toast.error("Image must be under 5MB");
                return;
            }
            setImageFile(file);
            const reader = new FileReader();
            reader.onloadend = () => setImagePreview(reader.result);
            reader.readAsDataURL(file);
        }
    };

    if (!selectedUser) {
        return (
            <div className="hidden md:flex flex-1 flex-col items-center justify-center bg-[var(--bg-card)]/50 text-[var(--text-muted)]">
                <div className="w-16 h-16 rounded-full bg-[var(--bg-elevated)] flex items-center justify-center mb-4">
                    <User size={32} opacity={0.5} />
                </div>
                <p>Select a user to start chatting</p>
            </div>
        );
    }

    const isOtherUserOnline = onlineUsers.some(u => u === selectedUser.id || u?.userId === selectedUser.id || u?.id === selectedUser.id);

    return (
        <div className="flex flex-col h-full bg-[var(--bg-card)]/50 backdrop-blur-sm relative">
            {/* Header */}
            <div className="p-3 md:p-4 border-b border-[var(--border-color)] flex items-center gap-3 bg-[var(--bg-card)]/80 backdrop-blur-md z-10">
                <button onClick={onBack} className="md:hidden p-1 -ml-1 text-[var(--text-muted)] hover:text-[var(--text-primary)]">
                    <ArrowLeft size={24} />
                </button>
                <div className="relative">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-[var(--gradient-1)] to-[var(--gradient-2)] p-[1.5px]">
                        <div className="w-full h-full rounded-full bg-[var(--bg-card)] overflow-hidden flex items-center justify-center">
                            {selectedUser.profilePicUrl ? (
                                <img src={selectedUser.profilePicUrl} className="w-full h-full object-cover" />
                            ) : (
                                <span className="text-sm font-bold">{selectedUser.name?.charAt(0).toUpperCase()}</span>
                            )}
                        </div>
                    </div>
                    {isOtherUserOnline && (
                        <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-emerald-500 border-2 border-[var(--bg-card)]" />
                    )}
                </div>
                <div className="flex-1 min-w-0">
                    <h3 className="font-bold text-[var(--text-primary)] truncate">{selectedUser.name}</h3>
                    <p className="text-[10px] text-[var(--text-muted)] flex items-center gap-1">
                        {isOtherUserOnline ? (
                            <><span className="w-1 h-1 rounded-full bg-emerald-500"></span> Active now</>
                        ) : (
                            'Offline'
                        )}
                    </p>
                </div>
            </div>

            {/* Messages Area */}
            <div className="flex-1 overflow-y-auto p-4 custom-scrollbar bg-[var(--bg-primary)]/20">
                {loading ? (
                    <div className="flex flex-col items-center justify-center h-full gap-2">
                        <Loader className="animate-spin text-[var(--accent)]" size={24} />
                        <span className="text-[11px] text-[var(--text-muted)] font-medium">Loading messages...</span>
                    </div>
                ) : (
                    <div className="flex flex-col gap-1">
                        {messages.length === 0 && (
                            <div className="text-center py-20">
                                <p className="text-[12px] text-[var(--text-muted)]">No messages yet. Say hi! 👋</p>
                            </div>
                        )}
                        {messages.map((msg, idx) => {
                            const isMe = msg.senderId === currentUserId;
                            return (
                                <ChatBubble
                                    key={msg.id || idx}
                                    message={msg}
                                    isMe={isMe}
                                    onImageClick={setPreviewImage}
                                />
                            );
                        })}
                        {typingUser && typingUser.senderId === selectedUser.id && (
                            <motion.div
                                initial={{ opacity: 0, y: 5 }}
                                animate={{ opacity: 1, y: 0 }}
                                className="flex items-center gap-2 ml-1 mt-2"
                            >
                                <div className="flex gap-1">
                                    <span className="w-1.5 h-1.5 rounded-full bg-[var(--text-muted)] animate-bounce" style={{ animationDelay: '0ms' }}></span>
                                    <span className="w-1.5 h-1.5 rounded-full bg-[var(--text-muted)] animate-bounce" style={{ animationDelay: '150ms' }}></span>
                                    <span className="w-1.5 h-1.5 rounded-full bg-[var(--text-muted)] animate-bounce" style={{ animationDelay: '300ms' }}></span>
                                </div>
                                <span className="text-[10px] text-[var(--text-muted)] italic">{selectedUser.name} is typing...</span>
                            </motion.div>
                        )}
                        <div ref={messagesEndRef} />
                    </div>
                )}
            </div>

            {/* Input Area */}
            <div className="p-4 bg-[var(--bg-card)]/80 backdrop-blur-md border-t border-[var(--border-color)]">
                {imagePreview && (
                    <div className="relative w-20 h-20 mb-3 rounded-xl overflow-hidden group border border-[var(--border-color)] shadow-lg shadow-black/20">
                        <img src={imagePreview} className="w-full h-full object-cover" />
                        <button
                            onClick={() => { setImageFile(null); setImagePreview(''); }}
                            className="absolute top-1 right-1 bg-black/60 p-1 rounded-full text-white hover:bg-red-500 transition-colors"
                        >
                            <X size={12} />
                        </button>
                    </div>
                )}
                <form onSubmit={handleSend} className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => fileRef.current?.click()}
                        className="p-2.5 text-[var(--text-muted)] hover:text-[var(--accent)] hover:bg-[var(--accent-light)] rounded-xl transition-all"
                    >
                        <ImagePlus size={22} />
                    </button>
                    <input
                        type="file"
                        ref={fileRef}
                        onChange={handleImageSelect}
                        accept="image/*"
                        className="hidden"
                    />
                    <div className="flex-1 relative">
                        {showEmojis && (
                            <div className="absolute bottom-full mb-2 left-0 bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-2 shadow-xl shadow-black/10 flex flex-wrap gap-1 w-[220px]">
                                {EMOJIS.map(emoji => (
                                    <button 
                                        key={emoji} 
                                        type="button" 
                                        onClick={() => { setText(prev => prev + emoji); setShowEmojis(false); }}
                                        className="w-8 h-8 flex items-center justify-center hover:bg-[var(--bg-elevated)] rounded-lg text-xl transition-colors"
                                    >
                                        {emoji}
                                    </button>
                                ))}
                            </div>
                        )}
                        <input
                            type="text"
                            value={text}
                            onChange={handleTyping}
                            placeholder="Type a message..."
                            className="w-full bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-2xl pl-4 pr-10 py-2.5 text-sm focus:ring-2 focus:ring-[var(--accent)] focus:border-transparent outline-none text-[var(--text-primary)] transition-all placeholder-[var(--text-muted)]"
                        />
                        <button
                            type="button"
                            onClick={() => setShowEmojis(!showEmojis)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-[var(--text-muted)] hover:text-[var(--accent)] transition-colors"
                        >
                            <Smile size={18} />
                        </button>
                    </div>
                    <button
                        type="submit"
                        disabled={(!text.trim() && !imageFile) || sending}
                        className="p-2.5 bg-[var(--accent)] text-white rounded-2xl shadow-lg shadow-[var(--accent-glow)] hover:scale-105 active:scale-95 transition-all disabled:opacity-50 disabled:grayscale disabled:scale-100"
                    >
                        {sending ? <Loader className="animate-spin" size={20} /> : <Send size={20} />}
                    </button>
                </form>
            </div>

            <ImagePreviewModal
                src={previewImage}
                onClose={() => setPreviewImage(null)}
            />
        </div>
    );
}

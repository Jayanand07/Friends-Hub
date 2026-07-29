import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Send, ImagePlus, MoreVertical, X, Loader, Smile } from 'lucide-react';
import ChatBubble from './ChatBubble';
import ImagePreviewModal from './ImagePreviewModal';
import { getGroupMessages, sendGroupMessageRest } from '../../api/groupChat';
import { uploadImage } from '../../api/posts';
import { useToast } from '../Toast';
import GroupMembersModal from './GroupMembersModal';
import { stompClient } from '../../socket/chatSocket';
import { getPublicKeyForUser, decryptMessage, encryptGroupMessage, decryptGroupMessage } from '../../crypto/e2ee';
import { isAllowedImageType } from '../../utils/sanitize';

export default function GroupChatWindow({ group, currentUser, onBack, refreshGroups, isSocketConnected }) {
    const [messages, setMessages] = useState([]);
    const [text, setText] = useState('');
    const [loading, setLoading] = useState(false);
    const [sending, setSending] = useState(false);
    const [showMembersModal, setShowMembersModal] = useState(false);
    const [imageFile, setImageFile] = useState(null);
    const [imagePreview, setImagePreview] = useState('');
    const [previewImage, setPreviewImage] = useState(null); // Full screen preview
    const [showEmojis, setShowEmojis] = useState(false);
    const [groupKeyB64, setGroupKeyB64] = useState(null);
    const groupKeyRef = useRef(null);
    const EMOJIS = ['😊', '😂', '❤️', '👍', '🔥', '😢', '😍', '👏', '🎉', '🤔', '🙌', '✨'];
    const messagesEndRef = useRef(null);
    const fileRef = useRef(null);
    const subscriptionRef = useRef(null);
    const toast = useToast();

    // Decrypt and cache group key on load or group change
    useEffect(() => {
        groupKeyRef.current = null;
        setGroupKeyB64(null);
        if (!group?.groupKeys || !currentUser) return;

        (async () => {
            try {
                const keysMap = typeof group.groupKeys === 'string' ? JSON.parse(group.groupKeys) : group.groupKeys;
                const myUserId = currentUser.id || currentUser.userId;
                const myEncryptedKey = keysMap[myUserId];
                if (myEncryptedKey && myEncryptedKey.ciphertext && myEncryptedKey.iv) {
                    const creatorPubKey = await getPublicKeyForUser(group.createdById);
                    if (creatorPubKey) {
                        const decryptedKey = await decryptMessage(myEncryptedKey.ciphertext, myEncryptedKey.iv, creatorPubKey);
                        if (decryptedKey && decryptedKey !== "[Unable to decrypt this message]") {
                            setGroupKeyB64(decryptedKey);
                            groupKeyRef.current = decryptedKey;
                        }
                    }
                }
            } catch (e) {
                console.error("Failed to decrypt group key:", e);
            }
        })();
    }, [group?.groupId, group?.groupKeys, group?.createdById, currentUser]);

    // Load messages — with race condition guard
    useEffect(() => {
        if (!group) return;
        let cancelled = false;
        setLoading(true);
        getGroupMessages(group.groupId)
            .then(async (res) => {
                if (cancelled) return;
                const raw = Array.isArray(res.data) ? res.data : [];

                // Attempt key recovery if state not updated yet
                let activeGroupKey = groupKeyB64 || groupKeyRef.current;
                if (!activeGroupKey && group.groupKeys && currentUser) {
                    try {
                        const keysMap = typeof group.groupKeys === 'string' ? JSON.parse(group.groupKeys) : group.groupKeys;
                        const myUserId = currentUser.id || currentUser.userId;
                        const myEncryptedKey = keysMap[myUserId];
                        if (myEncryptedKey?.ciphertext && myEncryptedKey?.iv) {
                            const creatorPubKey = await getPublicKeyForUser(group.createdById);
                            if (creatorPubKey) {
                                activeGroupKey = await decryptMessage(myEncryptedKey.ciphertext, myEncryptedKey.iv, creatorPubKey);
                                if (activeGroupKey && activeGroupKey !== "[Unable to decrypt this message]") {
                                    setGroupKeyB64(activeGroupKey);
                                    groupKeyRef.current = activeGroupKey;
                                }
                            }
                        }
                    } catch (e) {
                        console.warn("Group key resolution failed:", e);
                    }
                }

                const decryptedArray = await Promise.all(
                    raw.map(async (msg) => {
                        if (msg.content && msg.iv && activeGroupKey) {
                            const plaintext = await decryptGroupMessage(msg.content, msg.iv, activeGroupKey);
                            return { ...msg, content: plaintext };
                        }
                        return msg;
                    })
                );
                if (!cancelled) {
                    setMessages(decryptedArray);
                }
            })
            .catch(() => { if (!cancelled) toast.error("Failed to load messages"); })
            .finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [group?.groupId, groupKeyB64, currentUser]);

    // WebSocket Subscription
    useEffect(() => {
        if (!group || !stompClient || !isSocketConnected) return;

        subscriptionRef.current = stompClient.subscribe(`/topic/group-${group.groupId}`, async (message) => {
            const msg = JSON.parse(message.body);
            let processedMsg = msg;

            const activeKey = groupKeyRef.current || groupKeyB64;
            if (msg.content && msg.iv && activeKey) {
                const plaintext = await decryptGroupMessage(msg.content, msg.iv, activeKey);
                processedMsg = { ...msg, content: plaintext };
            }

            setMessages(prev => {
                if (prev.some(m => m.id === processedMsg.id)) return prev;
                return [...prev, processedMsg];
            });
            refreshGroups();
        });

        return () => {
            if (subscriptionRef.current) {
                subscriptionRef.current.unsubscribe();
            }
        };
    }, [group?.groupId, groupKeyB64, refreshGroups, isSocketConnected]);

    // Auto-scroll
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

            const activeKey = groupKeyRef.current || groupKeyB64;
            let ciphertext = messageText;
            let iv = null;

            if (messageText && activeKey) {
                const encrypted = await encryptGroupMessage(messageText, activeKey);
                ciphertext = encrypted.ciphertext;
                iv = encrypted.iv;
            }

            const payload = {
                content: ciphertext,
                iv: iv,
                imageUrl: imageUrl
            };

            if (imageUrl || !isSocketConnected || !stompClient || !stompClient.connected) {
                const sentMsg = await sendGroupMessageRest(group.groupId, ciphertext, imageUrl, iv);
                const optimisticMsg = {
                    ...sentMsg.data,
                    content: messageText
                };
                setMessages(prev => {
                    if (prev.some(m => m.id === optimisticMsg.id)) return prev;
                    return [...prev, optimisticMsg];
                });
            } else {
                stompClient.publish({
                    destination: `/app/chat.group.send/${group.groupId}`,
                    body: JSON.stringify(payload)
                });
            }

            setText('');
            setImageFile(null);
            setImagePreview('');
        } catch (error) {
            console.error(error);
            toast.error("Failed to send");
        } finally {
            setSending(false);
        }
    };

    const handleImageSelect = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 5 * 1024 * 1024) {
                toast.error("Image must be under 5MB");
                return;
            }
            if (!isAllowedImageType(file.type)) {
                toast.error("Only JPEG, PNG, GIF, and WebP images are allowed");
                return;
            }
            setImageFile(file);
            const reader = new FileReader();
            reader.onloadend = () => setImagePreview(reader.result);
            reader.readAsDataURL(file);
        }
    };

    return (
        <div className="flex flex-col h-full bg-[var(--bg-card)]/50 backdrop-blur-sm relative">
            {/* Header */}
            <div className="p-4 border-b border-[var(--border-color)] flex items-center justify-between bg-[var(--bg-card)]/80 backdrop-blur-md z-10">
                <div className="flex items-center gap-3">
                    <button onClick={onBack} className="md:hidden p-1 -ml-1 text-[var(--text-muted)] hover:text-[var(--text-primary)]">
                        <X size={24} />
                    </button>
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[var(--gradient-1)] to-[var(--gradient-2)] p-[2px]">
                        {group.groupImageUrl ? (
                            <img src={group.groupImageUrl} alt={group.name} className="w-full h-full rounded-full object-cover" />
                        ) : (
                            <div className="w-full h-full rounded-full bg-[var(--bg-card)] flex items-center justify-center font-bold">
                                {group.name.charAt(0).toUpperCase()}
                            </div>
                        )}
                    </div>
                    <div>
                        <h3 className="font-bold text-[var(--text-primary)]">{group.name}</h3>
                        <p className="text-xs text-[var(--text-muted)] cursor-pointer hover:underline" onClick={() => setShowMembersModal(true)}>
                            {group.memberCount} members
                        </p>
                    </div>
                </div>
                <button onClick={() => setShowMembersModal(true)} className="p-2 text-[var(--text-muted)] hover:text-[var(--text-primary)]">
                    <MoreVertical size={20} />
                </button>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
                <AnimatePresence initial={false}>
                    {loading ? (
                        <div className="flex justify-center pt-10"><Loader className="animate-spin text-[var(--accent)]" /></div>
                    ) : (
                        messages.map((msg, idx) => {
                            const isMe = msg.senderId === currentUser.id || msg.senderId === currentUser.userId;
                            return (
                                <ChatBubble
                                    key={msg.id || idx}
                                    message={msg}
                                    isMe={isMe}
                                    onImageClick={setPreviewImage}
                                />
                            );
                        })
                    )}
                </AnimatePresence>
                <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="p-4 border-t border-[var(--border-color)] bg-[var(--bg-card)]/80 backdrop-blur-md">
                {imagePreview && (
                    <div className="relative w-24 h-24 mb-2 rounded-lg overflow-hidden group border border-[var(--border-color)]">
                        <img src={imagePreview} className="w-full h-full object-cover" />
                        <button
                            onClick={() => { setImageFile(null); setImagePreview(''); }}
                            className="absolute top-1 right-1 bg-black/50 p-1 rounded-full text-white opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                            <X size={12} />
                        </button>
                    </div>
                )}
                <form onSubmit={handleSend} className="flex items-center gap-2">
                    <button
                        type="button"
                        onClick={() => fileRef.current?.click()}
                        className="p-2 text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-colors"
                    >
                        <ImagePlus size={24} />
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
                            onChange={e => setText(e.target.value)}
                            placeholder="Message..."
                            className="w-full bg-[var(--bg-elevated)] border-none rounded-2xl pl-4 pr-10 py-2.5 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none text-[var(--text-primary)] transition-all placeholder-[var(--text-muted)]"
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
                        className="p-2 text-[var(--accent)] hover:bg-[var(--accent-light)] rounded-full transition-colors disabled:opacity-50 hover:scale-105 active:scale-95"
                    >
                        {sending ? <Loader className="animate-spin" size={24} /> : <Send size={24} />}
                    </button>
                </form>
            </div>

            {/* Modals */}
            {showMembersModal && (
                <GroupMembersModal
                    groupId={group.groupId}
                    onClose={() => setShowMembersModal(false)}
                    currentUser={currentUser}
                />
            )}

            <ImagePreviewModal
                src={previewImage}
                onClose={() => setPreviewImage(null)}
            />
        </div>
    );
}

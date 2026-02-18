import { useState, useEffect, useCallback, useRef } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { getChatConversations, getOnlineUsers } from '../api/chat';
import { connectChat, disconnectChat } from '../socket/chatSocket';
import UserListSidebar from '../components/chat/UserListSidebar';
import ChatWindow from '../components/chat/ChatWindow';

export default function ChatPage() {
    const { user } = useAuth();
    const currentUserId = user?.id;
    const [conversations, setConversations] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [messages, setMessages] = useState([]);
    const [onlineUsers, setOnlineUsers] = useState([]);
    const [typingUser, setTypingUser] = useState(null);
    const typingTimerRef = useRef(null);

    // Initial setup
    useEffect(() => {
        if (!currentUserId) return;

        getChatConversations()
            .then((res) => setConversations(Array.isArray(res.data) ? res.data : []))
            .catch(() => { });

        // Initial online users fetch
        getOnlineUsers()
            .then((res) => setOnlineUsers(Array.isArray(res.data) ? res.data : []))
            .catch(() => { });
    }, [currentUserId]);

    // Connect WebSocket with all handlers
    useEffect(() => {
        if (!currentUserId) return;

        connectChat(currentUserId, {
            onMessage: (msg) => {
                // Handle different message types
                if (msg.type === 'delete') {
                    setMessages((prev) =>
                        prev.map((m) => m.id === msg.id ? { ...m, isDeleted: true, content: 'This message was deleted' } : m)
                    );
                    return;
                }
                if (msg.type === 'read') {
                    // Mark all own messages to this user as read
                    setMessages((prev) =>
                        prev.map((m) => m.senderId === currentUserId ? { ...m, isRead: true } : m)
                    );
                    return;
                }
                // Regular message
                setMessages((prev) => {
                    if (prev.some((m) => m.id === msg.id)) return prev;
                    return [...prev, msg];
                });
                // Update conversations if new partner
                setConversations((prev) => {
                    const partnerId = msg.senderId === currentUserId ? msg.receiverId : msg.senderId;
                    const partnerName = msg.senderId === currentUserId ? msg.receiverName : msg.senderName;
                    const partnerEmail = msg.senderId === currentUserId ? msg.receiverEmail : msg.senderEmail;
                    if (!prev.find((c) => c.id === partnerId)) {
                        return [{ id: partnerId, name: partnerName, email: partnerEmail }, ...prev];
                    }
                    return prev;
                });
            },
            onTyping: (data) => {
                setTypingUser(data);
                clearTimeout(typingTimerRef.current);
                typingTimerRef.current = setTimeout(() => setTypingUser(null), 2500);
            },
            onOnlineUsers: (users) => {
                setOnlineUsers(Array.isArray(users) ? users : []);
            },
        });

        return () => disconnectChat();
    }, [currentUserId]);

    const handleSelectUser = useCallback((u) => {
        setSelectedUser(u);
        setMessages([]);
        setTypingUser(null);
    }, []);

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex h-[calc(100vh-80px)] md:h-[calc(100vh-96px)] -m-4 md:-m-6"
        >
            {/* Sidebar */}
            <div className={`w-full md:w-80 border-r border-[var(--border-color)] bg-[var(--bg-card)]/30 flex-shrink-0 ${selectedUser ? 'hidden md:flex md:flex-col' : 'flex flex-col'
                }`}>
                <UserListSidebar
                    conversations={conversations}
                    activeUserId={selectedUser?.id}
                    onSelectUser={handleSelectUser}
                    onlineUsers={onlineUsers}
                />
            </div>

            {/* Chat Window */}
            <div className={`flex-1 flex flex-col ${selectedUser ? 'flex' : 'hidden md:flex'
                }`}>
                <ChatWindow
                    selectedUser={selectedUser}
                    currentUserId={currentUserId}
                    messages={messages}
                    setMessages={setMessages}
                    onBack={() => setSelectedUser(null)}
                    typingUser={typingUser}
                    onlineUsers={onlineUsers}
                />
            </div>
        </motion.div>
    );
}

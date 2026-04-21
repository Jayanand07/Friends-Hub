import SockJS from 'sockjs-client/dist/sockjs';
import { Client } from '@stomp/stompjs';

const WS_BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace('/api', '');

export let stompClient = null;

export function connectChat(userId, { onMessage, onTyping, onOnlineUsers, onConnected }) {
    const token = localStorage.getItem('token');
    stompClient = new Client({
        webSocketFactory: () => new SockJS(`${WS_BASE}/ws`),
        connectHeaders: {
            Authorization: `Bearer ${token}`
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        debug: () => { },
    });

    stompClient.onConnect = () => {
        if (onConnected) onConnected();
        // Private messages
        stompClient.subscribe(`/queue/messages-${userId}`, (msg) => {
            const body = JSON.parse(msg.body);
            onMessage?.(body);
        });

        // Typing indicator
        stompClient.subscribe(`/queue/typing-${userId}`, (msg) => {
            const body = JSON.parse(msg.body);
            onTyping?.(body);
        });

        // Online users broadcast
        stompClient.subscribe('/topic/online-users', (msg) => {
            const body = JSON.parse(msg.body);
            onOnlineUsers?.(body);
        });

        // Register self as online
        stompClient.publish({
            destination: '/app/chat.register',
            body: JSON.stringify({ userId }),
        });
    };

    stompClient.onStompError = (frame) => {
        console.error('STOMP error:', frame.headers['message']);
    };

    stompClient.activate();
    return stompClient;
}

export function sendChatMessage(receiverId, content, senderEmail, imageUrl) {
    if (stompClient?.connected) {
        stompClient.publish({
            destination: '/app/chat.send',
            body: JSON.stringify({ receiverId, content, senderEmail, imageUrl }),
        });
    }
}

export function sendTypingIndicator(senderId, senderName, receiverId) {
    if (stompClient?.connected) {
        stompClient.publish({
            destination: '/app/chat.typing',
            body: JSON.stringify({ senderId, senderName, receiverId }),
        });
    }
}

export function disconnectChat() {
    if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
    }
}

export function isConnected() {
    return stompClient?.connected ?? false;
}

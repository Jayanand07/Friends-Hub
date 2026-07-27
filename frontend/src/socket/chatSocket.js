import SockJS from 'sockjs-client/dist/sockjs';
import { Client } from '@stomp/stompjs';

const WS_BASE = (import.meta.env.VITE_API_URL).replace('/api', '');

export let stompClient = null;

// Ref-counted connection — tracks how many components are using it
let subscriberCount = 0;

// Per-user notification handlers (so MainLayout shares the same connection)
const notificationHandlers = new Map();

// Connection-level callbacks for chat pages
let chatCallbacks = null;

function subscribeUserTopics(userId, callbacks) {
    if (!stompClient?.connected) return;

    chatCallbacks = callbacks;

    // Private messages
    stompClient.subscribe(`/queue/messages-${userId}`, (msg) => {
        const body = JSON.parse(msg.body);
        if (body.type === 'message' || !body.type) {
            window.dispatchEvent(new CustomEvent('unreadChatMessage', { detail: body }));
        }
        callbacks?.onMessage?.(body);
    });

    // Typing indicator
    stompClient.subscribe(`/queue/typing-${userId}`, (msg) => {
        const body = JSON.parse(msg.body);
        callbacks?.onTyping?.(body);
    });

    // Online users broadcast
    stompClient.subscribe('/topic/online-users', (msg) => {
        const body = JSON.parse(msg.body);
        callbacks?.onOnlineUsers?.(body);
    });

    // Register self as online
    stompClient.publish({
        destination: '/app/chat.register',
        body: JSON.stringify({ userId }),
    });
}

function subscribeNotificationTopics() {
    if (!stompClient?.connected) return;

    for (const [uid, handlers] of notificationHandlers) {
        stompClient.subscribe(`/queue/notifications-${uid}`, (msg) => {
            const body = JSON.parse(msg.body);
            handlers.forEach(handler => handler(body));
        });
    }
}

export function connectChat(userId, callbacks = {}) {
    const token = localStorage.getItem('token');

    // If client exists and is connected, just add new subscriptions
    if (stompClient?.connected) {
        subscribeUserTopics(userId, callbacks);
        subscriberCount++;
        return stompClient;
    }

    // If client is connecting/active but not yet connected, increment count and wait
    if (stompClient?.active) {
        // Store the callbacks — they'll be picked up on next reconnect's onConnect
        chatCallbacks = { ...chatCallbacks, ...callbacks };
        subscriberCount++;
        return stompClient;
    }

    subscriberCount++;

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
        if (chatCallbacks?.onConnected) chatCallbacks.onConnected();
        subscribeUserTopics(userId, chatCallbacks || callbacks);
        subscribeNotificationTopics();
    };

    stompClient.onStompError = (frame) => {
        console.error('STOMP error:', frame.headers['message']);
    };

    stompClient.activate();
    return stompClient;
}

/**
 * Register a notification handler for a given userId.
 * Uses the shared STOMP connection instead of creating a new one.
 * @returns {Function} unsubscribe function
 */
export function addNotificationHandler(userId, handler) {
    const existing = notificationHandlers.get(userId) || [];
    existing.push(handler);
    notificationHandlers.set(userId, existing);

    // If already connected, subscribe immediately
    if (stompClient?.connected) {
        stompClient.subscribe(`/queue/notifications-${userId}`, (msg) => {
            const body = JSON.parse(msg.body);
            handler(body);
        });
    }

    // Return unsubscribe function
    return () => {
        const handlers = notificationHandlers.get(userId);
        if (handlers) {
            const filtered = handlers.filter(h => h !== handler);
            if (filtered.length === 0) {
                notificationHandlers.delete(userId);
            } else {
                notificationHandlers.set(userId, filtered);
            }
        }
    };
}

export function sendChatMessage(receiverId, ciphertext, iv, imageUrl) {
    if (stompClient?.connected) {
        stompClient.publish({
            destination: '/app/chat.send',
            body: JSON.stringify({ receiverId, content: ciphertext, iv, imageUrl }),
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
    subscriberCount = Math.max(0, subscriberCount - 1);
    if (subscriberCount > 0) return; // Other components still need the connection

    if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
    }
    notificationHandlers.clear();
    chatCallbacks = null;
}

export function isConnected() {
    return stompClient?.connected ?? false;
}

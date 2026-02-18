package com.example.socialmedia.config;

import com.example.socialmedia.service.ChatService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // Map sessionId -> userId for disconnect tracking
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    public WebSocketEventListener(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        broadcastOnlineUsers();
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            chatService.removeOnlineUser(userId);
            broadcastOnlineUsers();
        }
    }

    public void registerUserSession(String sessionId, Long userId) {
        sessionUserMap.put(sessionId, userId);
        chatService.addOnlineUser(userId);
        broadcastOnlineUsers();
    }

    private void broadcastOnlineUsers() {
        messagingTemplate.convertAndSend("/topic/online-users", chatService.getOnlineUsers());
    }
}

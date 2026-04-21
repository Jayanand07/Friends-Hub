package com.example.socialmedia.controller;

import com.example.socialmedia.config.WebSocketEventListener;
import com.example.socialmedia.dto.ChatMessageDTO;
import com.example.socialmedia.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener eventListener;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate,
            WebSocketEventListener eventListener) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.eventListener = eventListener;
    }

    // ===== WebSocket STOMP endpoints =====

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatSendRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }
        String senderEmail = authentication.getName();
        ChatMessageDTO message = chatService.sendMessage(senderEmail, request.getReceiverId(),
                request.getContent(), request.getImageUrl());

        messagingTemplate.convertAndSend("/queue/messages-" + request.getReceiverId(), message);
        messagingTemplate.convertAndSend("/queue/messages-" + message.getSenderId(), message);
    }

    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingRequest request) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setSenderId(request.getSenderId());
        dto.setSenderName(request.getSenderName());
        dto.setType("typing");
        messagingTemplate.convertAndSend("/queue/typing-" + request.getReceiverId(), dto);
    }

    @MessageMapping("/chat.register")
    public void registerUser(@Payload Map<String, Long> payload,
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        Long userId = payload.get("userId");
        if (userId != null) {
            String sessionId = headerAccessor.getSessionId();
            eventListener.registerUserSession(sessionId, userId);
        }
    }

    // ===== REST endpoints =====

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(
            @PathVariable Long userId, Authentication authentication) {
        return ResponseEntity.ok(chatService.getConversation(authentication.getName(), userId));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatService.ChatPartnerDTO>> getConversations(Authentication authentication) {
        return ResponseEntity.ok(chatService.getChatPartners(authentication.getName()));
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<ChatService.ChatPartnerDTO>> searchUsers(
            @RequestParam String query, Authentication authentication) {
        return ResponseEntity.ok(chatService.searchUsers(query, authentication.getName()));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageDTO> sendMessageRest(
            @RequestBody ChatSendRequest request, Authentication authentication) {
        ChatMessageDTO message = chatService.sendMessage(authentication.getName(),
                request.getReceiverId(), request.getContent(), request.getImageUrl());
        messagingTemplate.convertAndSend("/queue/messages-" + request.getReceiverId(), message);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/read/{senderUserId}")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long senderUserId, Authentication authentication) {
        int count = chatService.markAsRead(authentication.getName(), senderUserId);
        // Notify sender that messages were read
        ChatMessageDTO readReceipt = new ChatMessageDTO();
        readReceipt.setType("read");
        readReceipt.setReceiverId(senderUserId);
        messagingTemplate.convertAndSend("/queue/messages-" + senderUserId, readReceipt);
        return ResponseEntity.ok(Map.of("marked", count));
    }

    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<ChatMessageDTO> deleteMessage(
            @PathVariable Long messageId, Authentication authentication) {
        ChatMessageDTO deleted = chatService.deleteMessage(messageId, authentication.getName());
        // Notify both parties
        ChatMessageDTO notification = new ChatMessageDTO();
        notification.setId(messageId);
        notification.setType("delete");
        notification.setSenderId(deleted.getSenderId());
        messagingTemplate.convertAndSend("/queue/messages-" + deleted.getReceiverId(), notification);
        messagingTemplate.convertAndSend("/queue/messages-" + deleted.getSenderId(), notification);
        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/online")
    public ResponseEntity<Set<Long>> getOnlineUsers() {
        return ResponseEntity.ok(chatService.getOnlineUsers());
    }

    // ===== Request payloads =====

    public static class ChatSendRequest {
        private Long receiverId;
        private String content;
        private String senderEmail;
        private String imageUrl;

        public Long getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(Long receiverId) {
            this.receiverId = receiverId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSenderEmail() {
            return senderEmail;
        }

        public void setSenderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    public static class TypingRequest {
        private Long senderId;
        private String senderName;
        private Long receiverId;

        public Long getSenderId() {
            return senderId;
        }

        public void setSenderId(Long senderId) {
            this.senderId = senderId;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public Long getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(Long receiverId) {
            this.receiverId = receiverId;
        }
    }
}

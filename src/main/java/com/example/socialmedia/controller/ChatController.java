package com.example.socialmedia.controller;

import com.example.socialmedia.config.WebSocketEventListener;
import com.example.socialmedia.dto.ChatMessageDTO;
import com.example.socialmedia.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener eventListener;
    private final com.example.socialmedia.repository.UserRepository userRepo;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate,
            WebSocketEventListener eventListener,
            com.example.socialmedia.repository.UserRepository userRepo) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.eventListener = eventListener;
        this.userRepo = userRepo;
    }

    // ===== WebSocket STOMP endpoints =====

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatSendRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }
        String senderEmail = authentication.getName();
        ChatMessageDTO message = chatService.sendMessage(senderEmail, request.getReceiverId(),
                request.getContent(), request.getImageUrl(), request.getIv());

        messagingTemplate.convertAndSend("/queue/messages-" + request.getReceiverId(), message);
        messagingTemplate.convertAndSend("/queue/messages-" + message.getSenderId(), message);
    }

    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required for typing indicator");
        }
        // M-2: Load user from DB to use their display name instead of leaking the email
        com.example.socialmedia.entity.User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String displayName = user.getUserInfo() != null
                && user.getUserInfo().getFirstName() != null
                && !user.getUserInfo().getFirstName().isEmpty()
            ? (user.getUserInfo().getFirstName()
                + (user.getUserInfo().getLastName() != null ? " " + user.getUserInfo().getLastName() : ""))
            : authentication.getName().split("@")[0];

        ChatMessageDTO dto = new ChatMessageDTO();
        // SECURITY: Use authenticated identity, NOT client-supplied values
        dto.setSenderName(displayName);
        dto.setType("typing");
        messagingTemplate.convertAndSend("/queue/typing-" + request.getReceiverId(), dto);
    }

    @MessageMapping("/chat.register")
    public void registerUser(@Payload Map<String, Long> payload,
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor,
            org.springframework.security.core.Authentication authentication) {
        Long userId = payload.get("userId");
        if (userId != null) {
            // SECURITY: Verify the userId matches the authenticated user
            // to prevent identity spoofing (user A registering as user B).
            if (authentication == null || authentication.getName() == null) {
                throw new RuntimeException("Authentication required to register");
            }
            com.example.socialmedia.entity.User authenticatedUser = userRepo
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            if (!authenticatedUser.getId().equals(userId)) {
                throw new RuntimeException("User ID mismatch: cannot register as another user");
            }
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
            @RequestParam @Size(max = 200, message = "Search query cannot exceed 200 characters") String query,
            Authentication authentication) {
        return ResponseEntity.ok(chatService.searchUsers(query, authentication.getName()));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageDTO> sendMessageRest(
            @Valid @RequestBody ChatSendRequest request, Authentication authentication) {
        ChatMessageDTO message = chatService.sendMessage(authentication.getName(),
                request.getReceiverId(), request.getContent(), request.getImageUrl(), request.getIv());
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
        @NotNull(message = "Receiver ID is required")
        private Long receiverId;
        @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
        private String content;
        // senderEmail field intentionally removed: sender identity is always sourced
        // from the server-side JWT (Authentication.getName()), never from client input.
        @Pattern(regexp = "^(https?://.*)?$", message = "Image URL must be a valid http/https URL")
        @Size(max = 2048, message = "Image URL cannot exceed 2048 characters")
        private String imageUrl;
        @Size(max = 500, message = "IV cannot exceed 500 characters")
        private String iv;

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

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getIv() {
            return iv;
        }

        public void setIv(String iv) {
            this.iv = iv;
        }
    }

    public static class TypingRequest {
        private Long senderId;
        private String senderName;
        @NotNull(message = "Receiver ID is required")
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

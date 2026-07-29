package com.example.socialmedia.service;

import com.example.socialmedia.dto.ChatMessageDTO;
import com.example.socialmedia.entity.ChatMessage;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.ChatMessageRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final com.example.socialmedia.repository.BlockRepository blockRepo;
    private final PresenceService presenceService;

    public ChatService(ChatMessageRepository chatRepo, UserRepository userRepo,
            NotificationService notificationService,
            com.example.socialmedia.repository.BlockRepository blockRepo,
            PresenceService presenceService) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.blockRepo = blockRepo;
        this.presenceService = presenceService;
    }

    // ===== Online status (delegated to PresenceService / Redis) =====
    public void addOnlineUser(Long userId) {
        presenceService.setUserOnline(userId);
    }

    public void removeOnlineUser(Long userId) {
        presenceService.setUserOffline(userId);
    }

    public Set<Long> getOnlineUsers() {
        return presenceService.getAllOnlineUserIds();
    }

    public boolean isOnline(Long userId) {
        return presenceService.isUserOnline(userId);
    }

    // ===== Messaging =====
    @Transactional
    public ChatMessageDTO sendMessage(String senderEmail, Long receiverId, String content, String imageUrl, String iv) {
        User sender = userRepo.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepo.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // SECURITY: Block check — if either user has blocked the other, reject the message
        if (blockRepo.existsByBlockerAndBlocked(receiver, sender) ||
                blockRepo.existsByBlockerAndBlocked(sender, receiver)) {
            throw new RuntimeException("Cannot send message: user is blocked");
        }

        String cleanContent = iv != null ? content : com.example.socialmedia.util.HtmlSanitizerUtil.sanitize(content);
        ChatMessage message = new ChatMessage(sender, receiver, cleanContent);
        message.setIv(iv);
        message.setImageUrl(imageUrl);
        ChatMessage saved = chatRepo.save(message);

        // Notify receiver
        notificationService.createNotification(
                receiver, NotificationType.MESSAGE,
                getDisplayName(sender) + " sent you a message", sender);

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getConversation(String currentEmail, Long otherUserId) {
        User currentUser = userRepo.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User otherUser = userRepo.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // SECURITY: Block check — prevent blocked users from viewing conversations
        if (blockRepo.existsByBlockerAndBlocked(currentUser, otherUser) ||
                blockRepo.existsByBlockerAndBlocked(otherUser, currentUser)) {
            throw new RuntimeException("Conversation not available");
        }

        return chatRepo.findConversation(currentUser, otherUser, PageRequest.of(0, 100))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public int markAsRead(String readerEmail, Long senderUserId) {
        User reader = userRepo.findByEmail(readerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User sender = userRepo.findById(senderUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // SECURITY: Block check
        if (blockRepo.existsByBlockerAndBlocked(reader, sender) ||
                blockRepo.existsByBlockerAndBlocked(sender, reader)) {
            throw new RuntimeException("Action not allowed");
        }

        return chatRepo.markMessagesAsRead(sender, reader);
    }

    @Transactional
    public ChatMessageDTO deleteMessage(Long messageId, String requesterEmail) {
        ChatMessage msg = chatRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!msg.getSender().getEmail().equals(requesterEmail)) {
            throw new RuntimeException("Not authorized to delete this message");
        }
        msg.setIsDeleted(true);
        msg.setContent("This message was deleted");
        msg.setIv(null);
        chatRepo.save(msg);
        return toDTO(msg);
    }

    // ===== Chat partners =====
    @Transactional(readOnly = true)
    public List<ChatPartnerDTO> getChatPartners(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return chatRepo.findChatPartners(user)
                .stream()
                .filter(partner -> !blockRepo.existsByBlockerAndBlocked(user, partner)
                        && !blockRepo.existsByBlockerAndBlocked(partner, user))
                .map(partner -> {
                    String name = getDisplayName(partner);
                    return new ChatPartnerDTO(partner.getId(), name, isOnline(partner.getId()));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatPartnerDTO> searchUsers(String query, String currentEmail) {
        User currentUser = userRepo.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userRepo.searchUsers(currentUser.getId(), query, null, null, PageRequest.of(0, 20))
                .stream()
                .map(u -> new ChatPartnerDTO(u.getId(), getDisplayName(u), isOnline(u.getId())))
                .collect(Collectors.toList());
    }

    // ===== Helpers =====
    private String getDisplayName(User user) {
        UserInfo info = user.getUserInfo();
        if (info != null && info.getFirstName() != null && !info.getFirstName().isEmpty()) {
            return info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "");
        }
        return "User#" + user.getId();
    }

    private ChatMessageDTO toDTO(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getSender().getId(),
                getDisplayName(msg.getSender()),
                msg.getReceiver().getId(),
                getDisplayName(msg.getReceiver()),
                msg.getContent(),
                msg.getIv(),
                msg.getImageUrl(),
                msg.getTimestamp(),
                msg.getIsRead(),
                msg.getIsDeleted());
    }

    // Inner DTO for chat partners list — NO PII (email excluded)
    public static class ChatPartnerDTO {
        private Long id;
        private String name;
        private boolean online;

        public ChatPartnerDTO(Long id, String name, boolean online) {
            this.id = id;
            this.name = name;
            this.online = online;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isOnline() {
            return online;
        }
    }
}

package com.example.socialmedia.service;

import com.example.socialmedia.dto.ChatMessageDTO;
import com.example.socialmedia.entity.ChatMessage;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.repository.ChatMessageRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    // Tracks online user IDs
    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    public ChatService(ChatMessageRepository chatRepo, UserRepository userRepo,
            NotificationService notificationService) {
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
    }

    // ===== Online status =====
    public void addOnlineUser(Long userId) {
        onlineUsers.add(userId);
    }

    public void removeOnlineUser(Long userId) {
        onlineUsers.remove(userId);
    }

    public Set<Long> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }

    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    // ===== Messaging =====
    public ChatMessageDTO sendMessage(String senderEmail, Long receiverId, String content, String imageUrl) {
        User sender = userRepo.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepo.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        ChatMessage message = new ChatMessage(sender, receiver, content);
        message.setImageUrl(imageUrl);
        ChatMessage saved = chatRepo.save(message);

        // Notify receiver
        notificationService.createNotification(
                receiver, NotificationType.MESSAGE,
                getDisplayName(sender) + " sent you a message", sender);

        return toDTO(saved);
    }

    public List<ChatMessageDTO> getConversation(String currentEmail, Long otherUserId) {
        User currentUser = userRepo.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User otherUser = userRepo.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return chatRepo.findConversation(currentUser, otherUser)
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
        chatRepo.save(msg);
        return toDTO(msg);
    }

    // ===== Chat partners =====
    public List<ChatPartnerDTO> getChatPartners(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return chatRepo.findChatPartners(user)
                .stream()
                .map(partner -> {
                    String name = getDisplayName(partner);
                    return new ChatPartnerDTO(partner.getId(), name, partner.getEmail(), isOnline(partner.getId()));
                })
                .collect(Collectors.toList());
    }

    public List<ChatPartnerDTO> searchUsers(String query, String currentEmail) {
        return userRepo.findAll().stream()
                .filter(u -> !u.getEmail().equals(currentEmail))
                .filter(u -> {
                    String name = getDisplayName(u).toLowerCase();
                    String email = u.getEmail().toLowerCase();
                    String q = query.toLowerCase();
                    return name.contains(q) || email.contains(q);
                })
                .limit(20)
                .map(u -> new ChatPartnerDTO(u.getId(), getDisplayName(u), u.getEmail(), isOnline(u.getId())))
                .collect(Collectors.toList());
    }

    // ===== Helpers =====
    private String getDisplayName(User user) {
        UserInfo info = user.getUserInfo();
        if (info != null && info.getFirstName() != null && !info.getFirstName().isEmpty()) {
            return info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }

    private ChatMessageDTO toDTO(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getSender().getId(),
                getDisplayName(msg.getSender()),
                msg.getSender().getEmail(),
                msg.getReceiver().getId(),
                getDisplayName(msg.getReceiver()),
                msg.getReceiver().getEmail(),
                msg.getContent(),
                msg.getImageUrl(),
                msg.getTimestamp(),
                msg.getIsRead(),
                msg.getIsDeleted());
    }

    // Inner DTO for chat partners list
    public static class ChatPartnerDTO {
        private Long id;
        private String name;
        private String email;
        private boolean online;

        public ChatPartnerDTO(Long id, String name, String email, boolean online) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.online = online;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public boolean isOnline() {
            return online;
        }
    }
}

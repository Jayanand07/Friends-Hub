package com.example.socialmedia.service;

import com.example.socialmedia.entity.Notification;
import com.example.socialmedia.entity.Notification.NotificationType;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.repository.NotificationRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepo,
            UserRepository userRepo,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.messagingTemplate = messagingTemplate;
    }

    public void createNotification(User targetUser, NotificationType type, String content, User actor) {
        // Don't notify yourself
        if (targetUser.getId().equals(actor.getId()))
            return;

        Notification notification = new Notification(targetUser, type, content, actor);
        notificationRepo.save(notification);

        // Push realtime via WebSocket
        messagingTemplate.convertAndSend(
                "/queue/notifications-" + targetUser.getId(),
                toDTO(notification));
    }

    public List<NotificationDTO> getUserNotifications(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepo.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepo.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public int markAllAsRead(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepo.markAllAsRead(user);
    }

    private String getDisplayName(User user) {
        UserInfo info = user.getUserInfo();
        if (info != null && info.getFirstName() != null && !info.getFirstName().isEmpty()) {
            return info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getType().name(),
                n.getContent(),
                n.getIsRead(),
                n.getCreatedAt().toString(),
                n.getActor() != null ? n.getActor().getId() : null,
                n.getActor() != null ? getDisplayName(n.getActor()) : null);
    }

    // DTO
    public static class NotificationDTO {
        private Long id;
        private String type;
        private String content;
        private Boolean isRead;
        private String createdAt;
        private Long actorId;
        private String actorName;

        public NotificationDTO(Long id, String type, String content, Boolean isRead,
                String createdAt, Long actorId, String actorName) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.isRead = isRead;
            this.createdAt = createdAt;
            this.actorId = actorId;
            this.actorName = actorName;
        }

        public Long getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getContent() {
            return content;
        }

        public Boolean getIsRead() {
            return isRead;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public Long getActorId() {
            return actorId;
        }

        public String getActorName() {
            return actorName;
        }
    }
}

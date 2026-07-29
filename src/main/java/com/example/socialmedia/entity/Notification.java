package com.example.socialmedia.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_actor", columnList = "actor_id"),
    @Index(name = "idx_notification_created_at", columnList = "createdAt")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // receiver of notification

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @jakarta.validation.constraints.Size(max = 500, message = "Notification content cannot exceed 500 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private Boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Optional: who triggered the notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    // Optional: the post this notification relates to (for LIKE/COMMENT), used for deep-linking
    @Column(name = "post_id")
    private Long postId;

    public Notification() {
    }

    public Notification(User user, NotificationType type, String content, User actor) {
        this.user = user;
        this.type = type;
        this.content = content;
        this.actor = actor;
    }

    public Notification(User user, NotificationType type, String content, User actor, Long postId) {
        this.user = user;
        this.type = type;
        this.content = content;
        this.actor = actor;
        this.postId = postId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public enum NotificationType {
        LIKE, COMMENT, FOLLOW, MESSAGE, FOLLOW_REQUEST
    }
}

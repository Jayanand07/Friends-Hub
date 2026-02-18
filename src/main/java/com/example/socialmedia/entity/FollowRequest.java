package com.example.socialmedia.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "requester_id", "target_id" })
})
public class FollowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowRequestStatus status = FollowRequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public FollowRequest() {
        this.createdAt = LocalDateTime.now();
    }

    public FollowRequest(User requester, User target) {
        this.requester = requester;
        this.target = target;
        this.status = FollowRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getTarget() {
        return target;
    }

    public void setTarget(User target) {
        this.target = target;
    }

    public FollowRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FollowRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

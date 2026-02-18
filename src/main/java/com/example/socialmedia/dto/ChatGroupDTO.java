package com.example.socialmedia.dto;

import java.time.LocalDateTime;

public class ChatGroupDTO {
    private Long groupId;
    private String name;
    private String groupImageUrl;
    private int memberCount;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long createdById;

    public ChatGroupDTO() {
    }

    public ChatGroupDTO(Long groupId, String name, String groupImageUrl, int memberCount,
            String lastMessage, LocalDateTime lastMessageTime, Long createdById) {
        this.groupId = groupId;
        this.name = name;
        this.groupImageUrl = groupImageUrl;
        this.memberCount = memberCount;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.createdById = createdById;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupImageUrl() {
        return groupImageUrl;
    }

    public void setGroupImageUrl(String groupImageUrl) {
        this.groupImageUrl = groupImageUrl;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }
}

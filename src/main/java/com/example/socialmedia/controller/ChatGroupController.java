package com.example.socialmedia.controller;

import com.example.socialmedia.dto.ChatGroupDTO;
import com.example.socialmedia.dto.ChatGroupMessageDTO;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.service.ChatGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.Set;

@RestController
@RequestMapping("/api/chat/groups")
@Validated
public class ChatGroupController {

    private final ChatGroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatGroupController(ChatGroupService groupService, SimpMessagingTemplate messagingTemplate) {
        this.groupService = groupService;
        this.messagingTemplate = messagingTemplate;
    }

    // ===== WebSocket =====

    // Sent to /app/chat.group.send/{groupId}
    @MessageMapping("/chat.group.send/{groupId}")
    public void sendGroupMessage(@DestinationVariable Long groupId, @Payload GroupMessageRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required for group messages");
        }
        // SECURITY: Always use authenticated identity, never client-supplied email
        String senderEmail = authentication.getName();

        ChatGroupMessageDTO message = groupService.sendGroupMessage(
                groupId, request.getContent(), request.getImageUrl(), request.getIv(), senderEmail);

        // Broadcast to specific group topic
        messagingTemplate.convertAndSend("/topic/group-" + groupId, message);
    }

    // ===== REST =====

    @PostMapping
    public ResponseEntity<ChatGroupDTO> createGroup(@Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(groupService.createGroup(
                request.getName(), request.getGroupImageUrl(), request.getMemberIds(), request.getGroupKeys(), authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ChatGroupDTO>> getUserGroups(Authentication authentication) {
        return ResponseEntity.ok(groupService.getUserGroups(authentication.getName()));
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<ChatGroupMessageDTO>> getGroupMessages(@PathVariable Long groupId,
            Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupMessages(groupId, authentication.getName()));
    }

    @PostMapping("/{groupId}/messages/send")
    public ResponseEntity<ChatGroupMessageDTO> sendGroupMessageRest(
            @PathVariable Long groupId, @Valid @RequestBody GroupMessageRequest request, Authentication authentication) {
        ChatGroupMessageDTO message = groupService.sendGroupMessage(
                groupId, request.getContent(), request.getImageUrl(), request.getIv(), authentication.getName());
        messagingTemplate.convertAndSend("/topic/group-" + groupId, message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<UserProfileResponse>> getGroupMembers(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupMembers(groupId, authentication.getName()));
    }

    @PostMapping("/{groupId}/members/add")
    public ResponseEntity<ChatGroupDTO> addMember(
            @PathVariable Long groupId, @Valid @RequestBody MemberRequest request, Authentication authentication) {
        return ResponseEntity.ok(groupService.addMember(groupId, request.getUserId(), request.getGroupKeys(), authentication.getName()));
    }

    @PostMapping("/{groupId}/members/remove")
    public ResponseEntity<ChatGroupDTO> removeMember(
            @PathVariable Long groupId, @Valid @RequestBody MemberRequest request, Authentication authentication) {
        return ResponseEntity.ok(groupService.removeMember(groupId, request.getUserId(), authentication.getName()));
    }

    // DTOs for requests
    public static class CreateGroupRequest {
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name cannot exceed 100 characters")
        private String name;
        @Pattern(regexp = "^(https?://.*)?$", message = "Group image URL must be a valid http/https URL")
        @Size(max = 2048, message = "Group image URL cannot exceed 2048 characters")
        private String groupImageUrl;
        @NotNull(message = "Member IDs are required")
        private Set<Long> memberIds;
        private String groupKeys;

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

        public Set<Long> getMemberIds() {
            return memberIds;
        }

        public void setMemberIds(Set<Long> memberIds) {
            this.memberIds = memberIds;
        }

        public String getGroupKeys() {
            return groupKeys;
        }

        public void setGroupKeys(String groupKeys) {
            this.groupKeys = groupKeys;
        }
    }

    public static class GroupMessageRequest {
        @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
        private String content;
        @Pattern(regexp = "^(https?://.*)?$", message = "Image URL must be a valid http/https URL")
        @Size(max = 2048, message = "Image URL cannot exceed 2048 characters")
        private String imageUrl;
        @Size(max = 500, message = "IV cannot exceed 500 characters")
        private String iv;
        private String senderEmail; // fallback if auth null

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

        public String getSenderEmail() {
            return senderEmail;
        }

        public void setSenderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
        }
    }

    public static class MemberRequest {
        @NotNull(message = "User ID is required")
        private Long userId;
        private String groupKeys;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getGroupKeys() {
            return groupKeys;
        }

        public void setGroupKeys(String groupKeys) {
            this.groupKeys = groupKeys;
        }
    }
}

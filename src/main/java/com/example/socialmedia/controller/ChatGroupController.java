package com.example.socialmedia.controller;

import com.example.socialmedia.dto.ChatGroupDTO;
import com.example.socialmedia.dto.ChatGroupMessageDTO;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.service.ChatGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.Set;

@RestController
@RequestMapping("/api/chat/groups")
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
        // Handle auth logic if needed, usually authentication principal covers it
        String senderEmail = authentication != null ? authentication.getName() : request.getSenderEmail();

        ChatGroupMessageDTO message = groupService.sendGroupMessage(
                groupId, request.getContent(), request.getImageUrl(), senderEmail);

        // Broadcast to specific group topic
        messagingTemplate.convertAndSend("/topic/group-" + groupId, message);
    }

    // ===== REST =====

    @PostMapping
    public ResponseEntity<ChatGroupDTO> createGroup(@RequestBody CreateGroupRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(groupService.createGroup(
                request.getName(), request.getGroupImageUrl(), request.getMemberIds(), authentication.getName()));
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

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<UserProfileResponse>> getGroupMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupMembers(groupId));
    }

    @PostMapping("/{groupId}/members/add")
    public ResponseEntity<ChatGroupDTO> addMember(
            @PathVariable Long groupId, @RequestBody MemberRequest request, Authentication authentication) {
        return ResponseEntity.ok(groupService.addMember(groupId, request.getUserId(), authentication.getName()));
    }

    @PostMapping("/{groupId}/members/remove")
    public ResponseEntity<ChatGroupDTO> removeMember(
            @PathVariable Long groupId, @RequestBody MemberRequest request, Authentication authentication) {
        return ResponseEntity.ok(groupService.removeMember(groupId, request.getUserId(), authentication.getName()));
    }

    // DTOs for requests
    public static class CreateGroupRequest {
        private String name;
        private String groupImageUrl;
        private Set<Long> memberIds;

        // getters/setters
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
    }

    public static class GroupMessageRequest {
        private String content;
        private String imageUrl;
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

        public String getSenderEmail() {
            return senderEmail;
        }

        public void setSenderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
        }
    }

    public static class MemberRequest {
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}

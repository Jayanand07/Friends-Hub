package com.example.socialmedia.service;

import com.example.socialmedia.dto.ChatGroupDTO;
import com.example.socialmedia.dto.ChatGroupMessageDTO;
import com.example.socialmedia.dto.UserProfileResponse;
import com.example.socialmedia.entity.ChatGroup;
import com.example.socialmedia.entity.ChatGroupMessage;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.repository.ChatGroupRepository;
import com.example.socialmedia.repository.ChatGroupMessageRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatGroupService {

    private final ChatGroupRepository groupRepo;
    private final ChatGroupMessageRepository messageRepo;
    private final UserRepository userRepo;

    public ChatGroupService(ChatGroupRepository groupRepo, ChatGroupMessageRepository messageRepo,
            UserRepository userRepo) {
        this.groupRepo = groupRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public ChatGroupDTO createGroup(String name, String groupImageUrl, Set<Long> memberIds, String creatorEmail) {
        User creator = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatGroup group = new ChatGroup(name, creator);
        group.setGroupImageUrl(groupImageUrl);

        // Add creator and selected members
        group.getMembers().add(creator);
        if (memberIds != null && !memberIds.isEmpty()) {
            List<User> members = userRepo.findAllById(memberIds);
            group.getMembers().addAll(members);
        }

        ChatGroup saved = groupRepo.save(group);
        return toGroupDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatGroupDTO> getUserGroups(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatGroup> groups = groupRepo.findByMembers_Id(user.getId());
        return groups.stream().map(this::toGroupDTO).collect(Collectors.toList());
    }

    @Transactional
    public ChatGroupDTO addMember(Long groupId, Long userId, String requesterEmail) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if requester is admin/creator (simplified: currently only creator)
        if (!group.getCreatedBy().getEmail().equals(requesterEmail)) {
            throw new RuntimeException("Only the group creator can add members");
        }

        User userToAdd = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User to add not found"));

        group.getMembers().add(userToAdd);
        return toGroupDTO(groupRepo.save(group));
    }

    @Transactional
    public ChatGroupDTO removeMember(Long groupId, Long userId, String requesterEmail) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Only creator can remove members
        if (!group.getCreatedBy().getEmail().equals(requesterEmail)) {
            throw new RuntimeException("Only the group creator can remove members");
        }

        // Cannot remove creator
        if (group.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Cannot remove the group creator");
        }

        User userToRemove = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User to remove not found"));

        group.getMembers().remove(userToRemove);
        return toGroupDTO(groupRepo.save(group));
    }

    @Transactional
    public ChatGroupMessageDTO sendGroupMessage(Long groupId, String content, String imageUrl, String senderEmail) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User sender = userRepo.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify sender is member
        if (!group.getMembers().contains(sender)) {
            throw new RuntimeException("You are not a member of this group");
        }

        ChatGroupMessage message = new ChatGroupMessage(group, sender, content);
        message.setImageUrl(imageUrl);
        ChatGroupMessage saved = messageRepo.save(message);

        return toMessageDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatGroupMessageDTO> getGroupMessages(Long groupId, String requesterEmail) {
        User requester = userRepo.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getMembers().contains(requester)) {
            throw new RuntimeException("Not a member of this group");
        }

        return messageRepo.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getGroupMembers(Long groupId) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return group.getMembers().stream()
                .map(this::toUserProfileResponse)
                .collect(Collectors.toList());
    }

    // Helpers
    private ChatGroupDTO toGroupDTO(ChatGroup group) {
        String lastMsg = "";
        java.time.LocalDateTime lastTime = null;

        // This is inefficient for large lists, but fine for MVP.
        // Ideally should optimize with specific query or stored field.
        List<ChatGroupMessage> msgs = messageRepo.findByGroupIdOrderByCreatedAtAsc(group.getId());
        if (!msgs.isEmpty()) {
            ChatGroupMessage latest = msgs.get(msgs.size() - 1);
            lastMsg = latest.getIsDeleted() ? "Message deleted"
                    : (latest.getContent() != null && !latest.getContent().isEmpty()) ? latest.getContent() : "Image";
            lastTime = latest.getCreatedAt();
        }

        return new ChatGroupDTO(
                group.getId(),
                group.getName(),
                group.getGroupImageUrl(),
                group.getMembers().size(),
                lastMsg,
                lastTime,
                group.getCreatedBy().getId());
    }

    private ChatGroupMessageDTO toMessageDTO(ChatGroupMessage msg) {
        String name = getDisplayName(msg.getSender());
        UserInfo info = msg.getSender().getUserInfo();
        String pic = info != null ? info.getProfilePicUrl() : null;

        return new ChatGroupMessageDTO(
                msg.getId(),
                msg.getSender().getId(),
                name,
                pic,
                msg.getContent(),
                msg.getImageUrl(),
                msg.getCreatedAt(),
                msg.getIsDeleted());
    }

    private String getDisplayName(User user) {
        UserInfo info = user.getUserInfo();
        if (info != null && info.getFirstName() != null) {
            return info.getFirstName() + (info.getLastName() != null ? " " + info.getLastName() : "");
        }
        return user.getEmail().split("@")[0];
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        UserInfo info = user.getUserInfo();
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(info != null ? info.getFirstName() : "")
                .lastName(info != null ? info.getLastName() : "")
                .profilePicUrl(info != null ? info.getProfilePicUrl() : null)
                .bio(info != null ? info.getBio() : "")
                .role(user.getRole())
                .build();
    }
}

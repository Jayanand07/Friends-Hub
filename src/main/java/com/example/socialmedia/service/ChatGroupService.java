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
    public ChatGroupDTO createGroup(String name, String groupImageUrl, Set<Long> memberIds, String groupKeys, String creatorEmail) {
        User creator = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatGroup group = new ChatGroup(name, creator);
        group.setGroupImageUrl(groupImageUrl);
        group.setGroupKeys(groupKeys);

        // Add creator and selected members
        group.getMembers().add(creator);
        if (memberIds != null && !memberIds.isEmpty()) {
            if (memberIds.size() >= 50) {
                throw new RuntimeException("Group cannot have more than 50 members");
            }
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

        List<ChatGroup> groups = groupRepo.findGroupsWithMembersByUserId(user.getId());
        if (groups.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> groupIds = groups.stream().map(ChatGroup::getId).collect(Collectors.toList());
        java.util.Map<Long, ChatGroupMessage> latestMap = messageRepo.findLatestMessagesByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(m -> m.getGroup().getId(), m -> m, (m1, m2) -> m1));

        return groups.stream()
                .map(g -> toGroupDTOWithLatest(g, latestMap.get(g.getId())))
                .collect(Collectors.toList());
    }

    private ChatGroupDTO toGroupDTOWithLatest(ChatGroup group, ChatGroupMessage latest) {
        String lastMsg = "";
        java.time.LocalDateTime lastTime = null;

        if (latest != null) {
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
                group.getCreatedBy() != null ? group.getCreatedBy().getId() : null,
                group.getGroupKeys());
    }

    private ChatGroupDTO toGroupDTO(ChatGroup group) {
        var latestOpt = messageRepo.findTopByGroupIdOrderByCreatedAtDesc(group.getId());
        return toGroupDTOWithLatest(group, latestOpt.orElse(null));
    }

    @Transactional
    public ChatGroupDTO addMember(Long groupId, Long userId, String groupKeys, String requesterEmail) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if requester is admin/creator (simplified: currently only creator)
        if (!group.getCreatedBy().getEmail().equals(requesterEmail)) {
            throw new RuntimeException("Only the group creator can add members");
        }

        User userToAdd = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User to add not found"));

        if (group.getMembers().size() >= 50) {
            throw new RuntimeException("Group member limit of 50 reached");
        }

        group.getMembers().add(userToAdd);
        if (groupKeys != null && !groupKeys.isEmpty()) {
            group.setGroupKeys(groupKeys);
        }
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
    public ChatGroupMessageDTO sendGroupMessage(Long groupId, String content, String imageUrl, String iv, String senderEmail) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User sender = userRepo.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify sender is member
        boolean isMember = group.getMembers().stream()
                .anyMatch(member -> member.getId().equals(sender.getId()));
        if (!isMember) {
            throw new RuntimeException("You are not a member of this group");
        }

        ChatGroupMessage message = new ChatGroupMessage(group, sender, content);
        message.setImageUrl(imageUrl);
        message.setIv(iv);
        ChatGroupMessage saved = messageRepo.save(message);

        return toMessageDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatGroupMessageDTO> getGroupMessages(Long groupId, String requesterEmail) {
        User requester = userRepo.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!isMemberOfGroup(group, requester)) {
            throw new RuntimeException("Not a member of this group");
        }

        return messageRepo.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
                .map(this::toMessageDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getGroupMembers(Long groupId, String requesterEmail) {
        ChatGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User requester = userRepo.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isMemberOfGroup(group, requester)) {
            throw new RuntimeException("Not a member of this group");
        }

        return group.getMembers().stream()
                .map(this::toUserProfileResponse)
                .collect(Collectors.toList());
    }

    // Helpers

    /**
     * SECURITY: Compare member by ID instead of using Set.contains()
     * which fails on Hibernate proxies due to reference equality.
     */
    private boolean isMemberOfGroup(ChatGroup group, User user) {
        return group.getMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()));
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
                msg.getIv(),
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

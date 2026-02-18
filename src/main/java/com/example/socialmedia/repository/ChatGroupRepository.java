package com.example.socialmedia.repository;

import com.example.socialmedia.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
    List<ChatGroup> findByMembers_Id(Long userId);

    Optional<ChatGroup> findByIdAndMembers_Id(Long groupId, Long userId);
}

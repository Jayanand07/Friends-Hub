package com.example.socialmedia.repository;

import com.example.socialmedia.entity.ChatGroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatGroupMessageRepository extends JpaRepository<ChatGroupMessage, Long> {
    List<ChatGroupMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId);
}

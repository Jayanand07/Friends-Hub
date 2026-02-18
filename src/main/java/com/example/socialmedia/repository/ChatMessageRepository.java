package com.example.socialmedia.repository;

import com.example.socialmedia.entity.ChatMessage;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

        @Query("SELECT m FROM ChatMessage m WHERE " +
                        "(m.sender = :user1 AND m.receiver = :user2) OR " +
                        "(m.sender = :user2 AND m.receiver = :user1) " +
                        "ORDER BY m.timestamp ASC")
        List<ChatMessage> findConversation(@Param("user1") User user1, @Param("user2") User user2);

        @Query("SELECT DISTINCT u FROM User u WHERE u IN (SELECT m.receiver FROM ChatMessage m WHERE m.sender = :user) OR u IN (SELECT m.sender FROM ChatMessage m WHERE m.receiver = :user)")
        List<User> findChatPartners(@Param("user") User user);

        @Modifying
        @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
        int markMessagesAsRead(@Param("sender") User sender, @Param("receiver") User receiver);
}

package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

    List<Story> findAllByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now);

    List<Story> findAllByExpiresAtBefore(LocalDateTime now);
}

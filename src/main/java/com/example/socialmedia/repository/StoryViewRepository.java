package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Story;
import com.example.socialmedia.entity.StoryView;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByStoryAndViewer(Story story, User viewer);

    List<StoryView> findByStoryId(Long storyId);

    void deleteByStoryIn(List<Story> stories);
}

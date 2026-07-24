package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Story;
import com.example.socialmedia.entity.StoryView;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    boolean existsByStoryAndViewer(Story story, User viewer);

    List<StoryView> findByStoryId(Long storyId);

    void deleteByStoryIn(List<Story> stories);

    // Batch query: count views per story for a list of story IDs (fixes N+1)
    @Query("SELECT sv.story.id, COUNT(sv) FROM StoryView sv WHERE sv.story.id IN :storyIds GROUP BY sv.story.id")
    List<Object[]> countViewsByStoryIds(@Param("storyIds") List<Long> storyIds);

    // Batch query: find which stories a user has viewed (fixes N+1)
    @Query("SELECT sv.story.id FROM StoryView sv WHERE sv.story.id IN :storyIds AND sv.viewer = :viewer")
    List<Long> findViewedStoryIdsByStoryIdsAndViewer(@Param("storyIds") List<Long> storyIds, @Param("viewer") User viewer);
}

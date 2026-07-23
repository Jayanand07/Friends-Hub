package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.SavedPost;
import com.example.socialmedia.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    Optional<SavedPost> findByUserAndPost(User user, Post post);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT CASE WHEN COUNT(sp) > 0 THEN true ELSE false END FROM SavedPost sp WHERE LOWER(sp.user.email) = LOWER(:email) AND sp.post.id = :postId")
    boolean existsByUserEmailAndPostId(@Param("email") String email, @Param("postId") Long postId);

    @Query("SELECT sp.post.id FROM SavedPost sp WHERE LOWER(sp.user.email) = LOWER(:email) AND sp.post.id IN :postIds")
    Set<Long> findSavedPostIdsByUserEmailAndPostIdIn(@Param("email") String email, @Param("postIds") List<Long> postIds);

    @Query("SELECT sp.post FROM SavedPost sp WHERE LOWER(sp.user.email) = LOWER(:email) ORDER BY sp.createdAt DESC")
    Page<Post> findSavedPostsByUserEmail(@Param("email") String email, Pageable pageable);
}

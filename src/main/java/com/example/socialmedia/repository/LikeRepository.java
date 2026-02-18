package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Like;
import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPost(User user, Post post);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);
}

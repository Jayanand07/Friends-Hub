package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAll(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Post p WHERE p.user.isPrivateAccount = false")
    Page<Post> findAllPublicPosts(Pageable pageable);

    Page<Post> findByUserId(Long userId, Pageable pageable);
}

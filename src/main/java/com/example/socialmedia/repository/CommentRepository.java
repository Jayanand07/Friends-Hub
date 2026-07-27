package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    // Count comments user A left on posts authored by user B
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :commenterId AND c.post.user.id = :postOwnerId")
    long countCommentsByCommenterOnOwnerPosts(@Param("commenterId") Long commenterId,
                                              @Param("postOwnerId") Long postOwnerId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    @Query("SELECT c.post.id, COUNT(c.id) FROM Comment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    java.util.List<Object[]> countCommentsByPostIdIn(@Param("postIds") java.util.List<Long> postIds);

    void deleteByPostId(Long postId);
}

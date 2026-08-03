package com.example.socialmedia.repository;

import com.example.socialmedia.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);


    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userInfo WHERE u.id != :userId AND u.id NOT IN (SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId) AND u.id NOT IN (SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId) AND u.id NOT IN (SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId) AND u.verificationStatus = 'VERIFIED'")
    List<User> findSuggestedUsers(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN u.userInfo ui " +
           "WHERE u.id != :requesterId " +
           "AND u.verificationStatus = 'VERIFIED' " +
           "AND (:query IS NULL OR :query = '' OR LOWER(ui.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(ui.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:location IS NULL OR :location = '' OR LOWER(ui.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:bio IS NULL OR :bio = '' OR LOWER(ui.bio) LIKE LOWER(CONCAT('%', :bio, '%'))) " +
           "ORDER BY u.createdAt DESC")
    List<User> searchUsers(@Param("requesterId") Long requesterId,
                           @Param("query") String query,
                           @Param("location") String location,
                           @Param("bio") String bio,
                           Pageable pageable);
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userInfo WHERE u.id != :userId AND u.verificationStatus = 'VERIFIED' " +
           "AND u.id NOT IN (SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId) " +
           "AND u.id NOT IN (SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId) " +
           "AND u.id NOT IN (SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId)")
    List<User> findRecommendationCandidates(@Param("userId") Long userId, Pageable pageable);
}

package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Follow;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    List<Follow> findByFollowingId(Long userId);

    List<Follow> findByFollowerId(Long userId);

    boolean existsByFollowerAndFollowing(User follower, User following);
}

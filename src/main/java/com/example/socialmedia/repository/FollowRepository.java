package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Follow;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    List<Follow> findByFollowingId(Long userId);

    List<Follow> findByFollowerId(Long userId);

    List<Follow> findByFollowerIdIn(List<Long> followerIds);

    List<Follow> findByFollowingIdIn(List<Long> followingIds);

    boolean existsByFollowerAndFollowing(User follower, User following);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long userId);

    long countByFollowerId(Long userId);

    @Query("SELECT COUNT(f1) FROM Follow f1 WHERE f1.following.id = :targetId AND f1.follower.id IN (SELECT f2.following.id FROM Follow f2 WHERE f2.follower.id = :requesterId)")
    int countMutualFriends(@Param("requesterId") Long requesterId, @Param("targetId") Long targetId);

    @Query("SELECT f.follower FROM Follow f LEFT JOIN FETCH f.follower.userInfo WHERE f.following.id = :userId")
    List<User> findFollowersWithUserInfoByFollowingId(@Param("userId") Long userId);

    @Query("SELECT f.following FROM Follow f LEFT JOIN FETCH f.following.userInfo WHERE f.follower.id = :userId")
    List<User> findFollowingWithUserInfoByFollowerId(@Param("userId") Long userId);

    void deleteByFollowerOrFollowing(User follower, User following);
}

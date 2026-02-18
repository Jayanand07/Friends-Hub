package com.example.socialmedia.repository;

import com.example.socialmedia.entity.FollowRequest;
import com.example.socialmedia.entity.FollowRequestStatus;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    List<FollowRequest> findByTargetIdAndStatus(Long targetId, FollowRequestStatus status);

    Optional<FollowRequest> findByRequesterAndTarget(User requester, User target);
}

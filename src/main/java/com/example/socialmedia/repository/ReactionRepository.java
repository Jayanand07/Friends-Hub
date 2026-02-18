package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Reaction;
import com.example.socialmedia.entity.ReactionTargetType;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByTargetTypeAndTargetId(ReactionTargetType targetType, Long targetId);

    Optional<Reaction> findByUserAndTargetTypeAndTargetId(User user, ReactionTargetType targetType, Long targetId);

    void deleteByUserAndTargetTypeAndTargetId(User user, ReactionTargetType targetType, Long targetId);
}

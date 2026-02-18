package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Block;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    List<Block> findByBlockerId(Long blockerId);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
}

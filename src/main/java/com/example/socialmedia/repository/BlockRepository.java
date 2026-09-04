package com.example.socialmedia.repository;

import com.example.socialmedia.entity.Block;
import com.example.socialmedia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    /** PERF: batch-friendly — all IDs the given user has blocked (1 query, no lazy loads). */
    @org.springframework.data.jpa.repository.Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :blockerId")
    java.util.List<Long> findBlockedIdsByBlockerId(@org.springframework.data.repository.query.Param("blockerId") Long blockerId);

    /** PERF: batch-friendly — all IDs that have blocked the given user (1 query, no lazy loads). */
    @org.springframework.data.jpa.repository.Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :blockedId")
    java.util.List<Long> findBlockerIdsByBlockedId(@org.springframework.data.repository.query.Param("blockedId") Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<Block> findByBlockerId(Long blockerId);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    void deleteByBlockerOrBlocked(User blocker, User blocked);
}

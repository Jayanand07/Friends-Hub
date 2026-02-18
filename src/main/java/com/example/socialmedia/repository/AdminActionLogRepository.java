package com.example.socialmedia.repository;

import com.example.socialmedia.entity.AdminActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
    List<AdminActionLog> findAllByOrderByCreatedAtDesc();
}

package com.example.socialmedia.repository;

import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    Optional<UserInfo> findByUser(User user);

    @Query("SELECT ui FROM UserInfo ui WHERE ui.user.id IN :userIds")
    List<UserInfo> findByUserIdIn(@Param("userIds") List<Long> userIds);
}

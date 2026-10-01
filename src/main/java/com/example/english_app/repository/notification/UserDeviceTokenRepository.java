package com.example.english_app.repository.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.english_app.entity.notification.UserDeviceToken;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {
    List<UserDeviceToken> findByUserId(Long userId);
    Optional<UserDeviceToken> findByUserIdAndToken(Long userId, String token);
    void deleteAllByUserId(Long userId);
}

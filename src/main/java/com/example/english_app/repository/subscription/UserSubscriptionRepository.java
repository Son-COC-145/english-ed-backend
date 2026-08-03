package com.example.english_app.repository.subscription;

import com.example.english_app.entity.enums.SubscriptionStatus;
import com.example.english_app.entity.subscription.UserSubscription;
import com.example.english_app.entity.user.User;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    // Tìm gói cước đang active và chưa bị hết hạn
    Optional<UserSubscription> findFirstByUserAndStatusAndEndDateAfterOrderByEndDateDesc(
            User user,
            SubscriptionStatus status,
            LocalDateTime currentDate);
}

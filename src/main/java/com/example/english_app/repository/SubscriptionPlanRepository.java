package com.example.english_app.repository;

import com.example.english_app.entity.enums.PlanName;
import com.example.english_app.entity.subscription.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByName(PlanName name);

    Page<SubscriptionPlan> findByName(PlanName name, Pageable pageable);
}

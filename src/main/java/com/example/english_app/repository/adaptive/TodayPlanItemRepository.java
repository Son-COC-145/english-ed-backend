package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.TodayPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodayPlanItemRepository extends JpaRepository<TodayPlanItem, Long> {

    List<TodayPlanItem> findByPlanIdOrderByPositionAsc(Long planId);

    List<TodayPlanItem> findByPlanIdAndStatusNotOrderByPositionAsc(
            Long planId,
            com.example.english_app.entity.enums.TodayPlanItemStatus status);
}

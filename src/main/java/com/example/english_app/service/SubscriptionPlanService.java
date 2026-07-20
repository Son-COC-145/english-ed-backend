package com.example.english_app.service;

import com.example.english_app.dto.request.SubscriptionPlanRequest;
import com.example.english_app.dto.response.SubscriptionPlanResponse;
import com.example.english_app.entity.enums.PlanName;
import com.example.english_app.entity.subscription.SubscriptionPlan;
import com.example.english_app.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional(readOnly = true)
    public Page<SubscriptionPlanResponse> getAllPlans(PlanName name, Pageable pageable) {
        return subscriptionPlanRepository.findByName(name, pageable)
                .map(SubscriptionPlanResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(Long id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription Plan not found"));
        return SubscriptionPlanResponse.fromEntity(plan);
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(SubscriptionPlanRequest request) {
        if (subscriptionPlanRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Plan name already exists");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .aiPromptLimit(request.getAiPromptLimit())
                .description(request.getDescription())
                .build();

        return SubscriptionPlanResponse.fromEntity(subscriptionPlanRepository.save(plan));
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription Plan not found"));

        if (!plan.getName().equals(request.getName()) &&
                subscriptionPlanRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Plan name already exists");
        }

        plan.setName(request.getName());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setAiPromptLimit(request.getAiPromptLimit());
        plan.setDescription(request.getDescription());

        return SubscriptionPlanResponse.fromEntity(subscriptionPlanRepository.save(plan));
    }

    @Transactional
    public void deletePlan(Long id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription Plan not found"));
        subscriptionPlanRepository.delete(plan);
    }
}

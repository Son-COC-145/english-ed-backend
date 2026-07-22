package com.example.english_app.controller;

import com.example.english_app.dto.request.SubscriptionPlanRequest;
import com.example.english_app.dto.response.SubscriptionPlanResponse;
import com.example.english_app.entity.enums.PlanName;
import com.example.english_app.service.subscription.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plan", description = "Các API quản lý Gói cước (Basic, Premium,...)")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @Operation(summary = "Lấy danh sách gói cước")
    @GetMapping
    public ResponseEntity<Page<SubscriptionPlanResponse>> getAllPlans(
            @RequestParam(required = false) PlanName name,
            Pageable pageable) {
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans(name, pageable));
    }

    @Operation(summary = "Xem chi tiết gói cước")
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> getPlanById(
            @PathVariable Long id) {
        return ResponseEntity.ok(subscriptionPlanService.getPlanById(id));
    }

    @Operation(summary = "Tạo mới gói cước")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SubscriptionPlanResponse> createPlan(
            @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.createPlan(request));
    }

    @Operation(summary = "Cập nhật gói cước")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.updatePlan(id, request));
    }

    @Operation(summary = "Xóa gói cước")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long id) {
        subscriptionPlanService.deletePlan(id);
        return ResponseEntity.ok().build();
    }
}


package com.example.english_app.controller;

import com.example.english_app.dto.request.DeviceTokenRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.NotificationResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "APIs for Push Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/token")
    @Operation(summary = "Register FCM device token for push notifications")
    public ResponseEntity<ApiResponse<String>> registerToken(@Valid @RequestBody DeviceTokenRequest request) {
        notificationService.registerDeviceToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token registered successfully"));
    }

    @DeleteMapping("/token")
    @Operation(summary = "Remove FCM device token (Call this on Logout)")
    public ResponseEntity<ApiResponse<String>> removeToken(@RequestParam String token) {
        notificationService.removeDeviceToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token removed successfully"));
    }

    @GetMapping
    @Operation(summary = "Get paginated user notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(Pageable pageable) {
        PageResponse<NotificationResponse> response = notificationService.getUserNotifications(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all unread notifications as read")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}

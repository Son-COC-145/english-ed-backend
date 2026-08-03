package com.example.english_app.controller;

import com.example.english_app.service.subsription.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment VNPay", description = "Các API liên quan đến thanh toán qua cổng VNPay")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Operation(
        summary = "1. Tạo URL thanh toán VNPay",
        description = "Sinh ra URL thanh toán VNPay. Frontend dùng URL này để redirect người dùng đến trang quét mã QR/nhập thẻ.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Chuỗi URL thanh toán thành công")
        }
    )
    @PostMapping("/create")
    public ResponseEntity<String> createPayment(
            @Parameter(description = "ID của gói cước cần mua (ví dụ: 1)", required = true) @RequestParam Long planId, 
            Authentication authentication, 
            HttpServletRequest request) {
        String email = authentication.getName(); // Lấy email của user đang đăng nhập
        String paymentUrl = paymentService.createPaymentUrl(planId, email, request);
        return ResponseEntity.ok(paymentUrl);
    }

    @Operation(
        summary = "2. Xử lý kết quả trả về từ VNPay (Return URL)",
        description = "API này dành riêng cho VNPay để chuyển hướng (redirect) người dùng về trang Web Frontend (payment-success hoặc payment-failed)."
    )
    @GetMapping("/vnpay-return")
    public RedirectView vnpayReturn(@Parameter(hidden = true) @RequestParam Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            return new RedirectView(frontendUrl + "/payment-success");
        } else {
            return new RedirectView(frontendUrl + "/payment-failed");
        }
    }

    @Operation(
        summary = "3. Hứng IPN Webhook ngầm từ VNPay",
        description = "VNPay tự động gọi ngầm vào API này để cập nhật trạng thái thanh toán vào Database. API này không cần JWT Token.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Chuỗi JSON định dạng {'RspCode': '...', 'Message': '...'}")
        }
    )
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> processVnpayIpn(@Parameter(hidden = true) @RequestParam Map<String, String> params) {
        Map<String, String> result = paymentService.processVnpayIpn(params);
        return ResponseEntity.ok(result);
    }
}

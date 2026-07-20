package com.example.english_app.controller;

import com.example.english_app.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestParam Long planId, Authentication authentication, HttpServletRequest request) {
        String email = authentication.getName(); // Lấy email của user đang đăng nhập
        String paymentUrl = paymentService.createPaymentUrl(planId, email, request);
        return ResponseEntity.ok(paymentUrl);
    }

    @GetMapping("/vnpay-return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            return new RedirectView("http://localhost:3000/payment-success");
        } else {
            return new RedirectView("http://localhost:3000/payment-failed");
        }
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> processVnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> result = paymentService.processVnpayIpn(params);
        return ResponseEntity.ok(result);
    }
}

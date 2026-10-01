package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicAppInfoController {

    @Value("${app.support-email:}")
    private String supportEmail;

    @GetMapping("/app-info")
    public ApiResponse<Map<String, String>> appInfo() {
        return ApiResponse.success(Map.of(
                "appName", "English App",
                "supportEmail", supportEmail));
    }
}

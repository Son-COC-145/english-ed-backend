package com.example.english_app.security.oauth2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    @NotBlank
    private String postLoginUri;

    @NotNull
    private Duration exchangeCodeTtl = Duration.ofMinutes(2);
}

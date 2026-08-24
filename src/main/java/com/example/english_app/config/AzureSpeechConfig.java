package com.example.english_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cho Azure Cognitive Services – Speech / Pronunciation Assessment.
 *
 * <p>Sử dụng Java record để giữ config immutable và không cần boilerplate.
 * Được đăng ký qua {@link AzureSpeechAutoConfig}.
 *
 * <p>Các giá trị lấy từ environment variables:
 * <pre>
 *   AZURE_SPEECH_KEY    → subscriptionKey
 *   AZURE_SPEECH_REGION → region (mặc định: southeastasia)
 * </pre>
 */
@ConfigurationProperties(prefix = "azure.speech")
public record AzureSpeechConfig(

        /** Azure Cognitive Services subscription key */
        String subscriptionKey,

        /** Region của Azure resource, ví dụ: "southeastasia", "eastus" */
        String region,

        /** Timeout tối đa (giây) khi gọi Azure API. Mặc định: 5 */
        int timeoutSeconds
) {
    /** Trả về base URL của Speech-to-Text REST API cho region hiện tại */
    public String sttEndpoint() {
        return String.format("https://%s.stt.speech.microsoft.com", region);
    }

    /** Trả về URL của Text-to-Speech REST API cho region hiện tại */
    public String ttsEndpoint() {
        return String.format("https://%s.tts.speech.microsoft.com/cognitiveservices/v1", region);
    }
}

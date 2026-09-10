package com.example.english_app.service.speaking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpeakingJson {

    private final ObjectMapper mapper;

    public String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid speaking data", e);
        }
    }

    public JsonNode read(String value) {
        try {
            return value == null ? mapper.nullNode() : mapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid speaking JSON", e);
        }
    }
}

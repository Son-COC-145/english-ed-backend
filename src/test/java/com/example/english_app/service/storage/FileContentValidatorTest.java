package com.example.english_app.service.storage;

import com.example.english_app.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class FileContentValidatorTest {
    private final FileContentValidator validator = new FileContentValidator();

    @Test void rejectsSpoofedPdfMime() {
        assertThrows(AppException.class, () -> validator.validate(new MockMultipartFile("file","a.pdf",
                "application/pdf","not a pdf".getBytes())));
    }
    @Test void acceptsPdfSignatureAndTrailer() {
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file","a.pdf",
                "application/pdf","%PDF-1.7\n%%EOF".getBytes())));
    }
    @Test void rejectsExecutableDisguisedAsImage() {
        assertThrows(AppException.class, () -> validator.validate(new MockMultipartFile("file","a.png",
                "image/png",new byte[]{77,90,0,0})));
    }
}

package com.example.english_app.service.storage;

import com.example.english_app.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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

    @Test void acceptsValidDocxStructure() throws Exception {
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("file", "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                openXml("word/document.xml"))));
    }

    @Test void rejectsPptxDisguisedAsDocx() throws Exception {
        byte[] pptx = openXml("ppt/presentation.xml");
        assertThrows(AppException.class, () -> validator.validate(new MockMultipartFile("file", "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", pptx)));
    }

    private byte[] openXml(String documentEntry) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(documentEntry));
            zip.write("<document/>".getBytes());
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}

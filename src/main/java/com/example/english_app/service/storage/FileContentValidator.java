package com.example.english_app.service.storage;

import com.example.english_app.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FileContentValidator {
    public void validate(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String mime = file.getContentType();
            boolean valid = mime != null && switch (mime) {
                case "application/pdf" -> starts(bytes, "%PDF-") && new String(bytes,
                        Math.max(0, bytes.length - 2048), Math.min(bytes.length, 2048), StandardCharsets.ISO_8859_1)
                        .contains("%%EOF");
                case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    validOpenXml(bytes, "ppt/presentation.xml");
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    validOpenXml(bytes, "word/document.xml");
                case "video/mp4" ->
                    bytes.length >= 12 && new String(bytes, 4, 4, StandardCharsets.US_ASCII).equals("ftyp");
                case "audio/mpeg", "audio/mp3" -> starts(bytes, "ID3")
                        || bytes.length >= 4 && (bytes[0] & 255) == 255 && (bytes[1] & 224) == 224;
                case "image/jpeg", "image/png", "image/webp" -> validImage(bytes, mime);
                default -> false;
            };
            if (!valid)
                throw ErrorCode.INVALID_REQUEST.toException();
        } catch (IOException exception) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
    }

    private boolean starts(byte[] bytes, String signature) {
        return bytes.length >= signature.length()
                && new String(bytes, 0, signature.length(), StandardCharsets.US_ASCII).equals(signature);
    }

    private boolean validOpenXml(byte[] bytes, String requiredEntry) throws IOException {
        boolean types = false;
        boolean documentPart = false;
        long expanded = 0;
        int entries = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 2000 || entry.getName().contains(".."))
                    return false;
                types |= entry.getName().equals("[Content_Types].xml");
                documentPart |= entry.getName().equals(requiredEntry);
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    expanded += count;
                    if (expanded > 50L * 1024 * 1024)
                        return false;
                }
            }
        }
        return types && documentPart;
    }

    private boolean validImage(byte[] bytes, String mime) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase();
                return (mime.equals("image/" + format) || mime.equals("image/jpeg") && format.equals("jpg"))
                        && (long) reader.getWidth(0) * reader.getHeight(0) <= 25_000_000
                        && reader.read(0) != null;
            } finally {
                reader.dispose();
            }
        }
    }
}

package com.example.english_app.service.onboarding;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Creates non-reversible fingerprints used to reject idempotency-key payload conflicts. */
final class PlacementSubmissionHasher {

    private PlacementSubmissionHasher() {
    }

    static String answer(Long sessionId, Long questionId, String answer, Integer timeSpentMs) {
        String canonical = "ANSWER\n" + sessionId + "\n" + questionId + "\n"
                + (answer == null ? "" : answer.trim()) + "\n" + timeSpentMs;
        return sha256(canonical.getBytes(StandardCharsets.UTF_8));
    }

    static String pronunciation(
            Long sessionId,
            Long questionId,
            byte[] audioBytes) {
        MessageDigest digest = newDigest();
        digest.update("PRONUNCIATION\n".getBytes(StandardCharsets.UTF_8));
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(sessionId).array());
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(questionId).array());
        digest.update(audioBytes);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(byte[] value) {
        return HexFormat.of().formatHex(newDigest().digest(value));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        }
    }
}

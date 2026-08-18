package com.example.english_app.service.ipa;

import com.example.english_app.entity.ipa.IpaExampleWord;
import com.example.english_app.entity.ipa.IpaMinimalPair;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.IpaMinimalPairRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.service.audio.AzureTtsService;
import com.example.english_app.service.storage.AzureBlobStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service điều phối tự động sinh file âm thanh bằng Azure TTS và upload lên Azure Blob Storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IpaMediaGeneratorService {

    private final IpaPhonemeRepository phonemeRepository;
    private final IpaExampleWordRepository exampleWordRepository;
    private final IpaMinimalPairRepository minimalPairRepository;
    private final AzureTtsService ttsService;
    private final AzureBlobStorageService blobStorageService;

    /**
     * Tự động quét DB, sinh âm thanh qua Azure TTS và lưu vào Azure Blob Storage.
     *
     * @param overwriteAll nếu true sẽ sinh lại toàn bộ kể cả khi đã có link Azure.
     *                     nếu false chỉ sinh cho những record chưa có link Azure Blob Storage.
     * @return Báo cáo kết quả
     */
    @CacheEvict(value = {"ipa_phonemes_v2", "ipa_phoneme_detail_v2"}, allEntries = true)
    @Transactional
    public Map<String, Object> generateAllIpaAudio(boolean overwriteAll) {
        log.info("Starting batch IPA audio generation with Azure TTS (overwriteAll={})...", overwriteAll);

        int phonemesUpdated = 0;
        int wordsUpdated = 0;
        int pairsUpdated = 0;

        // 1. Xử lý âm vị (Phonemes)
        List<IpaPhoneme> phonemes = phonemeRepository.findAll();
        for (IpaPhoneme phoneme : phonemes) {
            boolean needMale = overwriteAll || isPlaceholder(phoneme.getAudioMaleUrl());
            boolean needFemale = overwriteAll || isPlaceholder(phoneme.getAudioFemaleUrl());

            if (needMale) {
                try {
                    byte[] maleAudio = ttsService.synthesizePhoneme(phoneme.getSymbol(), AzureTtsService.VOICE_MALE_US);
                    String blobName = "audio/phonemes/" + sanitize(phoneme.getSymbol()) + "_male.mp3";
                    String url = blobStorageService.uploadAudio(blobName, maleAudio, "audio/mpeg");
                    phoneme.setAudioMaleUrl(url);
                } catch (Exception e) {
                    log.error("Failed to generate male audio for phoneme {}: {}", phoneme.getSymbol(), e.getMessage());
                }
            }

            if (needFemale) {
                try {
                    byte[] femaleAudio = ttsService.synthesizePhoneme(phoneme.getSymbol(), AzureTtsService.VOICE_FEMALE_US);
                    String blobName = "audio/phonemes/" + sanitize(phoneme.getSymbol()) + "_female.mp3";
                    String url = blobStorageService.uploadAudio(blobName, femaleAudio, "audio/mpeg");
                    phoneme.setAudioFemaleUrl(url);
                } catch (Exception e) {
                    log.error("Failed to generate female audio for phoneme {}: {}", phoneme.getSymbol(), e.getMessage());
                }
            }

            if (needMale || needFemale) {
                phonemeRepository.save(phoneme);
                phonemesUpdated++;
            }
        }

        // 2. Xử lý từ ví dụ (Example Words)
        List<IpaExampleWord> words = exampleWordRepository.findAll();
        for (IpaExampleWord word : words) {
            if (overwriteAll || isPlaceholder(word.getAudioUrl())) {
                try {
                    byte[] wordAudio = ttsService.synthesizeWord(word.getWord(), AzureTtsService.VOICE_MALE_US);
                    String blobName = "audio/words/" + sanitize(word.getWord()) + ".mp3";
                    String url = blobStorageService.uploadAudio(blobName, wordAudio, "audio/mpeg");
                    word.setAudioUrl(url);
                    exampleWordRepository.save(word);
                    wordsUpdated++;
                } catch (Exception e) {
                    log.error("Failed to generate audio for word {}: {}", word.getWord(), e.getMessage());
                }
            }
        }

        // 3. Xử lý cặp âm dễ nhầm lẫn (Minimal Pairs)
        List<IpaMinimalPair> pairs = minimalPairRepository.findAll();
        for (IpaMinimalPair pair : pairs) {
            boolean needAudio1 = overwriteAll || isPlaceholder(pair.getAudio1Url());
            boolean needAudio2 = overwriteAll || isPlaceholder(pair.getAudio2Url());

            if (needAudio1) {
                try {
                    byte[] audio1 = ttsService.synthesizeWord(pair.getWord1(), AzureTtsService.VOICE_MALE_US);
                    String blobName = "audio/minimal_pairs/" + sanitize(pair.getWord1()) + ".mp3";
                    String url = blobStorageService.uploadAudio(blobName, audio1, "audio/mpeg");
                    pair.setAudio1Url(url);
                } catch (Exception e) {
                    log.error("Failed to generate audio for pair word1 {}: {}", pair.getWord1(), e.getMessage());
                }
            }

            if (needAudio2) {
                try {
                    byte[] audio2 = ttsService.synthesizeWord(pair.getWord2(), AzureTtsService.VOICE_MALE_US);
                    String blobName = "audio/minimal_pairs/" + sanitize(pair.getWord2()) + ".mp3";
                    String url = blobStorageService.uploadAudio(blobName, audio2, "audio/mpeg");
                    pair.setAudio2Url(url);
                } catch (Exception e) {
                    log.error("Failed to generate audio for pair word2 {}: {}", pair.getWord2(), e.getMessage());
                }
            }

            if (needAudio1 || needAudio2) {
                minimalPairRepository.save(pair);
                pairsUpdated++;
            }
        }

        log.info("Batch generation complete! Updated {} phonemes, {} example words, and {} minimal pairs.", phonemesUpdated, wordsUpdated, pairsUpdated);

        Map<String, Object> result = new HashMap<>();
        result.put("phonemesUpdated", phonemesUpdated);
        result.put("wordsUpdated", wordsUpdated);
        result.put("minimalPairsUpdated", pairsUpdated);
        result.put("status", "SUCCESS");
        result.put("message", String.format("Đã tự động tạo và lưu trữ thành công %d âm vị, %d từ vựng và %d cặp âm lên Azure Blob Storage!", phonemesUpdated, wordsUpdated, pairsUpdated));
        return result;
    }

    private boolean isPlaceholder(String url) {
        return url == null || url.isBlank() || !url.contains(".blob.core.windows.net");
    }

    private String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}


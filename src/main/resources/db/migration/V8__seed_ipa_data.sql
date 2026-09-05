-- V8__seed_ipa_data.sql
-- Seed IPA Phonemes (5 âm cơ bản — dữ liệu đầy đủ 44 âm được bổ sung trong V12)
INSERT INTO ipa_phonemes (symbol, phoneme_type, name_vi, audio_male_url, audio_female_url, cefr_intro_level, is_common_vn_error)
VALUES
  ('iː', 'VOWEL_MONO', 'Âm i dài', 'https://cdn.example.com/phonemes/ii_male.mp3', 'https://cdn.example.com/phonemes/ii_female.mp3', 'A1', false),
  ('ɪ',  'VOWEL_MONO', 'Âm i ngắn', 'https://cdn.example.com/phonemes/i_male.mp3', 'https://cdn.example.com/phonemes/i_female.mp3', 'A1', true),
  ('p',  'CONSONANT', 'Âm p', 'https://cdn.example.com/phonemes/p_male.mp3', 'https://cdn.example.com/phonemes/p_female.mp3', 'A1', false),
  ('b',  'CONSONANT', 'Âm b', 'https://cdn.example.com/phonemes/b_male.mp3', 'https://cdn.example.com/phonemes/b_female.mp3', 'A1', false),
  ('ʃ',  'CONSONANT', 'Âm sh', 'https://cdn.example.com/phonemes/sh_male.mp3', 'https://cdn.example.com/phonemes/sh_female.mp3', 'A2', true);

-- Seed IPA Example Words
INSERT INTO ipa_example_words (phoneme_id, word, ipa_transcription, audio_url)
VALUES
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'iː'), 'sheep', '/ʃiːp/', 'https://cdn.example.com/words/sheep.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'iː'), 'see', '/siː/', 'https://cdn.example.com/words/see.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪ'), 'ship', '/ʃɪp/', 'https://cdn.example.com/words/ship.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪ'), 'sit', '/sɪt/', 'https://cdn.example.com/words/sit.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'p'), 'pen', '/pen/', 'https://cdn.example.com/words/pen.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'p'), 'copy', '/ˈkɒpi/', 'https://cdn.example.com/words/copy.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'b'), 'book', '/bʊk/', 'https://cdn.example.com/words/book.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'b'), 'back', '/bæk/', 'https://cdn.example.com/words/back.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʃ'), 'shoe', '/ʃuː/', 'https://cdn.example.com/words/shoe.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʃ'), 'fish', '/fɪʃ/', 'https://cdn.example.com/words/fish.mp3');

-- GIN Index cho query thống kê JSONB sau này
CREATE INDEX IF NOT EXISTS idx_practice_log_phoneme 
    ON pronunciation_practice_logs USING GIN (phoneme_detail_json);

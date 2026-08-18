-- ============================================================
-- V17: Bổ sung lời khuyên khẩu hình cho người Việt & bảng Cặp âm tối thiểu (Minimal Pairs)
-- ============================================================

-- 1. Thêm cột pronunciation_tip_vi vào bảng ipa_phonemes
ALTER TABLE ipa_phonemes ADD COLUMN IF NOT EXISTS pronunciation_tip_vi TEXT;

-- 2. Cập nhật dữ liệu gợi ý khẩu hình cho các âm người Việt hay phát âm sai
UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Đặt nhẹ đầu lưỡi vào giữa hai hàm răng, thổi nhẹ luồng hơi qua kẽ răng. Tuyệt đối không phát âm thành âm "th" trong tiếng Việt (không chạm môi).'
WHERE symbol = 'θ';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Tương tự âm /θ/, đặt đầu lưỡi giữa hai hàm răng nhưng cần rung dây thanh quản (có tiếng rung trong cổ họng) khi đẩy hơi ra ngoài.'
WHERE symbol = 'ð';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Kéo khóe miệng sang hai bên như đang cười nhẹ, đầu lưỡi nâng cao chạm vòm họng trên, phát âm kéo dài hơn âm "i" tiếng Việt.'
WHERE symbol = 'iː';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Miệng mở tự nhiên, phát âm ngắn, dứt khoát và thả lỏng cơ miệng. Nằm giữa âm "i" và "ê" trong tiếng Việt.'
WHERE symbol = 'ɪ';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Chu môi tròn về phía trước, hai hàm răng khép hờ, đẩy luồng hơi mạnh ra ngoài (như động tác ra hiệu "suỵt" giữ im lặng).'
WHERE symbol = 'ʃ';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Bắt đầu với vị trí của âm /t/ (chặn hơi), sau đó bật mạnh luồng hơi qua âm /ʃ/. Tương tự âm "ch" tiếng Việt nhưng bật hơi dứt khoát hơn.'
WHERE symbol = 'tʃ';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /tʃ/ nhưng cần rung dây thanh quản trong cổ họng khi bật hơi.'
WHERE symbol = 'dʒ';

UPDATE ipa_phonemes 
SET pronunciation_tip_vi = 'Hạ cằm sâu xuống, mở rộng khẩu hình miệng theo chiều dọc lẫn chiều ngang, phát âm lai giữa âm "a" và "e".'
WHERE symbol = 'æ';

-- 3. Tạo bảng ipa_minimal_pairs (Cặp âm dễ nhầm lẫn)
CREATE TABLE IF NOT EXISTS ipa_minimal_pairs (
    id              BIGSERIAL PRIMARY KEY,
    phoneme1_id     SMALLINT NOT NULL REFERENCES ipa_phonemes(id) ON DELETE CASCADE,
    phoneme2_id     SMALLINT NOT NULL REFERENCES ipa_phonemes(id) ON DELETE CASCADE,
    title           VARCHAR(100) NOT NULL,
    description     TEXT,
    word1           VARCHAR(100) NOT NULL,
    ipa1            VARCHAR(100) NOT NULL,
    audio1_url      VARCHAR(500),
    word2           VARCHAR(100) NOT NULL,
    ipa2            VARCHAR(100) NOT NULL,
    audio2_url      VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_minimal_pairs_phonemes ON ipa_minimal_pairs(phoneme1_id, phoneme2_id);

-- 4. Seed dữ liệu mẫu cho các cặp âm kinh điển
INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT 
    p1.id, p2.id,
    'Phân biệt /iː/ (dài) và /ɪ/ (ngắn)',
    'Âm /iː/ kéo dài khóe miệng như cười, trong khi /ɪ/ phát âm dứt khoát và thả lỏng cơ miệng.',
    'sheep', '/ʃiːp/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sheep--_gb_1.mp3',
    'ship', '/ʃɪp/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/ship--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2
WHERE p1.symbol = 'iː' AND p2.symbol = 'ɪ'
ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT 
    p1.id, p2.id,
    'Phân biệt /s/ (nhẹ) và /ʃ/ (nặng)',
    'Âm /s/ xì hơi qua kẽ răng không chu môi, còn /ʃ/ cần chu tròn môi về phía trước.',
    'see', '/siː/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/see--_gb_1.mp3',
    'she', '/ʃiː/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/she--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2
WHERE p1.symbol = 's' AND p2.symbol = 'ʃ'
ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT 
    p1.id, p2.id,
    'Phân biệt /b/ (rung) và /p/ (bật hơi)',
    'Âm /p/ không rung cổ họng mà bật luồng hơi mạnh từ hai môi, âm /b/ rung dây thanh quản.',
    'bin', '/bɪn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bin--_gb_1.mp3',
    'pin', '/pɪn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/pin--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2
WHERE p1.symbol = 'b' AND p2.symbol = 'p'
ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT 
    p1.id, p2.id,
    'Phân biệt /e/ và /æ/ (a bẹt)',
    'Âm /æ/ cần hạ cằm sâu và mở rộng miệng hơn rất nhiều so với âm /e/.',
    'bet', '/bet/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bet--_gb_1.mp3',
    'bat', '/bæt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bat--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2
WHERE p1.symbol = 'e' AND p2.symbol = 'æ'
ON CONFLICT DO NOTHING;

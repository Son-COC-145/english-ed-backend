-- ============================================================
-- V18: Bảng Quy tắc Trọng âm & Ghép âm / Nối âm (Pronunciation & Stress Rules)
-- ============================================================

CREATE TABLE IF NOT EXISTS ipa_pronunciation_rules (
    id                  BIGSERIAL PRIMARY KEY,
    category            VARCHAR(50) NOT NULL, -- WORD_STRESS, LINKING_SOUNDS, ENDING_SOUNDS, INTONATION
    title_vi            VARCHAR(200) NOT NULL,
    summary_vi          VARCHAR(500) NOT NULL,
    content_markdown    TEXT NOT NULL,
    examples_json       JSONB NOT NULL DEFAULT '[]',
    order_index         INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pronunciation_rules_category ON ipa_pronunciation_rules(category, order_index);

-- Seed dữ liệu các quy tắc ngữ âm kinh điển
INSERT INTO ipa_pronunciation_rules (category, title_vi, summary_vi, content_markdown, examples_json, order_index) VALUES
(
    'WORD_STRESS',
    'Quy tắc trọng âm từ 2 âm tiết (Danh từ, Tính từ & Động từ)',
    'Hầu hết Danh từ và Tính từ 2 âm tiết nhấn trọng âm ở âm tiết thứ 1. Đa số Động từ 2 âm tiết nhấn trọng âm ở âm tiết thứ 2.',
    '### 1. Danh từ và Tính từ 2 âm tiết
- Trọng âm thường rơi vào **âm tiết thứ 1**.
- Ví dụ: `PREsent` (món quà), `DOCtor` (bác sĩ), `HAPpy` (hạnh phúc), `CLEVer` (thông minh).

### 2. Động từ 2 âm tiết
- Trọng âm thường rơi vào **âm tiết thứ 2**.
- Ví dụ: `preSENT` (thuyết trình, trao tặng), `deCIDE` (quyết định), `enJOY` (thưởng thức).

### 💡 Lưu ý đặc biệt:
Một số từ có cùng cách viết nhưng khác loại từ thì vị trí trọng âm sẽ thay đổi:
- **Record**: Danh từ `REcord` (bản ghi) vs Động từ `reCORD` (ghi âm).
- **Present**: Danh từ `PREsent` (món quà) vs Động từ `preSENT` (trình bày).',
    '[
        {"word": "Doctor", "ipa": "/ˈdɒktə/", "meaning": "Bác sĩ (Danh từ -> nhấn âm 1)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/doctor--_gb_1.mp3"},
        {"word": "Happy", "ipa": "/ˈhæpi/", "meaning": "Hạnh phúc (Tính từ -> nhấn âm 1)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/happy--_gb_1.mp3"},
        {"word": "Decide", "ipa": "/dɪˈsaɪd/", "meaning": "Quyết định (Động từ -> nhấn âm 2)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/decide--_gb_1.mp3"},
        {"word": "Enjoy", "ipa": "/ɪnˈdʒɔɪ/", "meaning": "Thưởng thức (Động từ -> nhấn âm 2)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/enjoy--_gb_1.mp3"}
    ]',
    1
),
(
    'WORD_STRESS',
    'Quy tắc trọng âm theo Hậu tố (Suffixes: -tion, -ic, -ee, -ese)',
    'Các đuôi -tion, -sion, -ic nhấn vào âm tiết đứng ngay trước nó. Các đuôi -ee, -ese, -ique nhấn vào chính nó.',
    '### 1. Nhấn vào âm tiết NGAY TRƯỚC hậu tố:
- Đuôi **-tion / -sion**: `in-for-MA-tion`, `de-CI-sion`.
- Đuôi **-ic / -ical**: `e-LEC-tric`, `eco-NO-mic`.
- Đuôi **-ity / -phy / -gy**: `a-BI-li-ty`, `pho-TO-gra-phy`, `bi-O-lo-gy`.

### 2. Nhấn vào CHÍNH hậu tố:
- Đuôi **-ee**: `em-ploy-EE` (nhân viên), `trai-nEE`.
- Đuôi **-ese**: `Viet-na-MESE`, `Chi-NESE`.
- Đuôi **-ique**: `u-NIQUE`, `tech-NIQUE`.',
    '[
        {"word": "Information", "ipa": "/ˌɪnfəˈmeɪʃn/", "meaning": "Thông tin (Nhấn trước -tion)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/information--_gb_1.mp3"},
        {"word": "Electric", "ipa": "/ɪˈlektrɪk/", "meaning": "Thuộc về điện (Nhấn trước -ic)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/electric--_gb_1.mp3"},
        {"word": "Vietnamese", "ipa": "/ˌvjetnəˈmiːz/", "meaning": "Tiếng Việt / Người Việt (Nhấn chính -ese)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/vietnamese--_gb_1.mp3"}
    ]',
    2
),
(
    'ENDING_SOUNDS',
    'Quy tắc phát âm đuôi -S / -ES (/s/, /z/, /ɪz/)',
    'Phát âm /s/ sau âm vô thanh (p, k, t, f, th). Phát âm /ɪz/ sau âm gió (s, sh, ch, z, ge). Phát âm /z/ cho các trường hợp còn lại.',
    '### 1. Phát âm là /s/ (Âm vô thanh):
- Tận cùng bằng các âm: **/p/, /k/, /t/, /f/, /θ/** (Mẹo nhớ: *Thời phong kiến phương tây*).
- Ví dụ: `cats` /kæts/, `books` /bʊks/, `stops` /stɒps/.

### 2. Phát âm là /ɪz/ (Âm xuýt, âm gió):
- Tận cùng bằng các âm: **/s/, /z/, /ʃ/, /tʃ/, /ʒ/, /dʒ/** (Chữ cái kết thúc: *s, x, z, ch, sh, ce, ge*).
- Ví dụ: `buses` /ˈbʌsɪz/, `watches` /ˈwɒtʃɪz/, `changes` /ˈtʃeɪndʒɪz/.

### 3. Phát âm là /z/ (Âm hữu thanh & nguyên âm):
- Tận cùng bằng các nguyên âm và phụ âm hữu thanh còn lại.
- Ví dụ: `dogs` /dɒɡz/, `plays` /pleɪz/, `rooms` /ruːmz/.',
    '[
        {"word": "Cats", "ipa": "/kæts/", "meaning": "Đuôi /s/ sau âm /t/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/cats--_gb_1.mp3"},
        {"word": "Watches", "ipa": "/ˈwɒtʃɪz/", "meaning": "Đuôi /ɪz/ sau âm /tʃ/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/watches--_gb_1.mp3"},
        {"word": "Dogs", "ipa": "/dɒɡz/", "meaning": "Đuôi /z/ sau âm hữu thanh /ɡ/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/dogs--_gb_1.mp3"}
    ]',
    3
),
(
    'ENDING_SOUNDS',
    'Quy tắc phát âm đuôi -ED (/t/, /d/, /ɪd/)',
    'Phát âm /ɪd/ sau âm /t/ và /d/. Phát âm /t/ sau các phụ âm vô thanh. Phát âm /d/ cho các trường hợp còn lại.',
    '### 1. Phát âm là /ɪd/:
- Động từ kết thúc bằng âm **/t/** hoặc **/d/**.
- Ví dụ: `wanted` /ˈwɒntɪd/, `needed` /ˈniːdɪd/.

### 2. Phát âm là /t/ (Âm vô thanh):
- Động từ kết thúc bằng các âm vô thanh: **/p/, /k/, /f/, /s/, /ʃ/, /tʃ/, /θ/**.
- Ví dụ: `looked` /lʊkt/, `watched` /wɒtʃt/, `stopped` /stɒpt/, `laughed` /lɑːft/.

### 3. Phát âm là /d/ (Âm hữu thanh):
- Động từ kết thúc bằng các âm còn lại và nguyên âm.
- Ví dụ: `played` /pleɪd/, `cleaned` /kliːnd/, `loved` /lʌvd/.',
    '[
        {"word": "Wanted", "ipa": "/ˈwɒntɪd/", "meaning": "Đuôi /ɪd/ sau âm /t/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/wanted--_gb_1.mp3"},
        {"word": "Looked", "ipa": "/lʊkt/", "meaning": "Đuôi /t/ sau âm vô thanh /k/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/looked--_gb_1.mp3"},
        {"word": "Played", "ipa": "/pleɪd/", "meaning": "Đuôi /d/ sau nguyên âm /eɪ/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/played--_gb_1.mp3"}
    ]',
    4
),
(
    'LINKING_SOUNDS',
    'Quy tắc Nối âm Phụ âm với Nguyên âm (Consonant to Vowel)',
    'Khi một từ kết thúc bằng phụ âm và từ tiếp theo bắt đầu bằng nguyên âm, phụ âm cuối sẽ nối liền sang nguyên âm đầu của từ sau.',
    '### Nguyên tắc hoạt động:
Từ thứ nhất kết thúc bằng **Phụ âm (Consonant)** + Từ thứ hai bắt đầu bằng **Nguyên âm (Vowel)** $\rightarrow$ Đọc nối liền như một từ.

### Ví dụ điển hình:
- `Hold on` $\rightarrow$ Đọc là `/həʊl-dɒn/`
- `An apple` $\rightarrow$ Đọc là `/ə-næpl/`
- `Deep end` $\rightarrow$ Đọc là `/diː-pend/`
- `Turn off` $\rightarrow$ Đọc là `/tɜː-nɒf/`',
    '[
        {"word": "Hold on", "ipa": "/həʊl dɒn/ -> /həʊldɒn/", "meaning": "Nối phụ âm /d/ sang nguyên âm /ɒ/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/hold_on--_gb_1.mp3"},
        {"word": "Turn off", "ipa": "/tɜːn ɒf/ -> /tɜːnɒf/", "meaning": "Nối phụ âm /n/ sang nguyên âm /ɒ/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/turn_off--_gb_1.mp3"}
    ]',
    5
),
(
    'LINKING_SOUNDS',
    'Quy tắc Nối âm Nguyên âm với Nguyên âm (Chèn âm /w/ và /j/)',
    'Khi hai nguyên âm đứng liền nhau, người bản xứ tự động chèn thêm âm lướt /w/ hoặc /j/ để phát âm mượt mà hơn.',
    '### 1. Chèn âm /w/ (Khi từ trước kết thúc bằng nguyên âm tròn môi: /uː/, /ʊ/, /oʊ/, /aʊ/):
- `Go out` $\rightarrow$ Đọc là `/ɡəʊ - w - aʊt/`
- `Do it` $\rightarrow$ Đọc là `/duː - w - ɪt/`
- `You are` $\rightarrow$ Đọc là `/juː - w - ɑː/`

### 2. Chèn âm /j/ (Khi từ trước kết thúc bằng nguyên âm dẹt môi: /iː/, /ɪ/, /eɪ/, /aɪ/, /ɔɪ/):
- `I am` $\rightarrow$ Đọc là `/aɪ - j - æm/`
- `See it` $\rightarrow$ Đọc là `/siː - j - ɪt/`
- `Say it` $\rightarrow$ Đọc là `/seɪ - j - ɪt/`',
    '[
        {"word": "Do it", "ipa": "/duː w ɪt/", "meaning": "Chèn âm /w/ giữa hai nguyên âm tròn môi", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/do_it--_gb_1.mp3"},
        {"word": "I am", "ipa": "/aɪ j æm/", "meaning": "Chèn âm /j/ giữa hai nguyên âm dẹt môi", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/i_am--_gb_1.mp3"}
    ]',
    6
);

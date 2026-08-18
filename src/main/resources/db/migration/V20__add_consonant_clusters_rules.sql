-- =========================================================================
-- V20: Trọn bộ Quy tắc Phụ âm tiết (Syllabic Consonants), Biến âm (Flap T, Glottal Stop) & Ghép âm
-- 1. Syllabic N (/n̩/ - "ừn / ờn"): button, sudden, listen, season, seven, action
-- 2. Syllabic L (/l̩/ - "ồ / ờl"): table, apple, bottle, middle, uncle, castle
-- 3. Syllabic M (/m̩/ - "ừm"): rhythm, prism, realism, bottom
-- 4. Biến âm Flap T (/t/ -> /d/ nhẹ chuẩn US): water, better, city, get out
-- 5. Âm chặn họng Glottal Stop (/ʔ/): cat, football, fitness, button
-- 6. Kỹ thuật ghép Cụm phụ âm đầu (Consonant Blends): blue, play, speak, street
-- =========================================================================

DELETE FROM ipa_pronunciation_rules WHERE category = 'CONSONANT_CLUSTERS';

INSERT INTO ipa_pronunciation_rules (category, title_vi, summary_vi, content_markdown, examples_json, order_index) VALUES
(
    'CONSONANT_CLUSTERS',
    'Quy tắc Âm tiết phụ Syllabic N (/n̩/ - Đuôi "ừn / ờn" trong button, sudden, listen, season)',
    'Hiện tượng phụ âm /n/ tự đóng vai trò như một nguyên âm tạo thành âm tiết không nhấn /n̩/ (nghe như "ừn" hoặc "ờn" nhẹ).',
    '### 1. Bản chất của Syllabic N (/n̩/):
Trong tiếng Anh, khi các đuôi **-en, -on, -an, -in, -tain** đứng sau một phụ âm và không nhận trọng âm, nguyên âm đứng trước bị triệt tiêu hoàn toàn. Lúc này, phụ âm **/n/** sẽ gánh vác vai trò tạo âm tiết, tạo ra âm **"ừn / ờn"** rất ngắn và nhẹ.

### 2. Các nhóm âm Syllabic N kinh điển:
- **Nhóm /t/ + /n/ $\rightarrow$ /tn̩/ (hoặc /ʔn̩/):** 
  - `Button` /ˈbʌtn/ $\rightarrow$ đọc như *"bắt-ừn"* (chặn họng âm /t/ rồi bật /n/)
  - `Kitten` /ˈkɪtn/ $\rightarrow$ đọc như *"kít-ừn"*
  - `Written` /ˈrɪtn/ $\rightarrow$ đọc như *"rít-ừn"*
  - `Mountain` /ˈmaʊntn/ $\rightarrow$ đọc như *"mao-ừn"* (chuẩn US)
- **Nhóm /d/ + /n/ $\rightarrow$ /dn̩/:**
  - `Sudden` /ˈsʌdn/ $\rightarrow$ đọc như *"sát-đừn"*
  - `Hidden` /ˈhɪdn/ $\rightarrow$ đọc như *"hít-đừn"*
  - `Garden` /ˈɡɑːrdn/ $\rightarrow$ đọc như *"ga-đừn"*
- **Nhóm /s, z/ + /n/ $\rightarrow$ /sn̩/, /zn̩/:**
  - `Listen` /ˈlɪsn/ $\rightarrow$ đọc như *"lít-sừn"* (chữ t câm)
  - `Lesson` /ˈlesn/ $\rightarrow$ đọc như *"lét-sừn"*
  - `Season` /ˈsiːzn/ $\rightarrow$ đọc như *"xi-zừn"*
  - `Cousin` /ˈkʌzn/ $\rightarrow$ đọc như *"cớ-zừn"*
  - `Prison` /ˈprɪzn/ $\rightarrow$ đọc như *"prí-zừn"*
- **Nhóm /v, f/ + /n/ $\rightarrow$ /vn̩/, /fn̩/:**
  - `Seven` /ˈsevn/ $\rightarrow$ đọc như *"xét-vừn"*
  - `Eleven` /ɪˈlevn/ $\rightarrow$ đọc như *"i-lét-vừn"*
  - `Oven` /ˈʌvn/ $\rightarrow$ đọc như *"ớ-vừn"*
  - `Often` /ˈɔːfn/ $\rightarrow$ đọc như *"óp-phừn"* (chữ t câm)
- **Nhóm đuôi -tion / -sion $\rightarrow$ /ʃn̩/, /ʒn̩/:**
  - `Action` /ˈækʃn/ $\rightarrow$ đọc như *"ác-sừn"*
  - `Vision` /ˈvɪʒn/ $\rightarrow$ đọc như *"ví-zhừn"*

### 💡 Mẹo phát âm:
Giữ nguyên đầu lưỡi áp sát chân răng trên khi phát âm phụ âm trước, sau đó chỉ cần mở đường thở qua mũi để bật luồng hơi ra thành âm /n/ (không cần mở hé môi ra để phát âm nguyên âm).',
    '[
        {"word": "Button", "ipa": "/ˈbʌtn/", "meaning": "Cái nút áo (Phát âm /tn/ nghe như bắt-ừn)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/button--_gb_1.mp3"},
        {"word": "Sudden", "ipa": "/ˈsʌdn/", "meaning": "Đột ngột (Phát âm /dn/ nghe như sát-đừn)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/sudden--_gb_1.mp3"},
        {"word": "Listen", "ipa": "/ˈlɪsn/", "meaning": "Lắng nghe (Phát âm /sn/ nghe như lít-sừn)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/listen--_gb_1.mp3"},
        {"word": "Seven", "ipa": "/ˈsevn/", "meaning": "Số 7 (Phát âm /vn/ nghe như xét-vừn)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/seven--_gb_1.mp3"},
        {"word": "Action", "ipa": "/ˈækʃn/", "meaning": "Hành động (Phát âm /ʃn/ nghe như ác-sừn)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/action--_gb_1.mp3"}
    ]',
    11
),
(
    'CONSONANT_CLUSTERS',
    'Quy tắc Âm tiết phụ Syllabic L (/l̩/ - Đuôi "ồ / ờl" trong table, apple, bottle, middle)',
    'Khi kết thúc bằng đuôi -ble, -ple, -tle, -dle, -cle, âm /l/ đóng vai trò như nguyên âm tạo thành âm tiết nhẹ /l̩/ (nghe gần giống "bồ", "pồ", "tồ", "đồ").',
    '### 1. Hiện tượng Âm tiết hóa phụ âm L (Dark L):
Khi một từ kết thúc bằng một phụ âm + `le` hoặc `al`, `el`, `il` (như `-ble`, `-ple`, `-tle`, `-dle`, `-gle`, `-cal`, `-cil`), âm **/l/** đứng sau trở thành **Syllabic L**, tự nó tạo thành một âm tiết mà không cần nguyên âm rõ ràng.

### 2. Các dạng đuôi phổ biến:
- **-ble $\rightarrow$ /bl̩/** (nghe như *"bồ"* nhẹ): `table` /ˈteɪbl/, `bubble` /ˈbʌbl/, `able` /ˈeɪbl/.
- **-ple $\rightarrow$ /pl̩/** (nghe như *"pồ"* nhẹ): `apple` /ˈæpl/, `simple` /ˈsɪmpl/, `people` /ˈpiːpl/.
- **-tle $\rightarrow$ /tl̩/** (nghe như *"tồ"* nhẹ): `bottle` /ˈbɒtl/, `little` /ˈlɪtl/, `title` /ˈtaɪtl/.
- **-dle $\rightarrow$ /dl̩/** (nghe như *"đồ"* nhẹ): `middle` /ˈmɪdl/, `candle` /ˈkændl/, `noodle` /ˈnuːdl/.
- **-cle / -kle $\rightarrow$ /kl̩/** (nghe như *"cồ"* nhẹ): `uncle` /ˈʌŋkl/, `circle` /ˈsɜːkl/.
- **-gle $\rightarrow$ /ɡl̩/** (nghe như *"gồ"* nhẹ): `eagle` /ˈiːɡl/, `single` /ˈsɪŋɡl/.
- **-cal / -cil $\rightarrow$ /kl̩/, /sl̩/**: `musical` /ˈmjuːzɪkl/, `pencil` /ˈpensl/.

### 💡 Cách phát âm chuẩn:
Bật nhẹ phụ âm đứng trước (/b/, /p/, /t/, /d/) rồi nâng ngay đầu lưỡi chạm vào nướu răng hàm trên và giữ yên (Dark L). Không đọc tách bạch thành 2 từ riêng lẻ.',
    '[
        {"word": "Table", "ipa": "/ˈteɪbl/", "meaning": "Cái bàn (Đuôi -ble phát âm /bl/ nghe như bồ nhẹ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/table--_gb_1.mp3"},
        {"word": "Apple", "ipa": "/ˈæpl/", "meaning": "Quả táo (Đuôi -ple phát âm /pl/ nghe như pồ nhẹ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/apple--_gb_1.mp3"},
        {"word": "Bottle", "ipa": "/ˈbɒtl/", "meaning": "Chai nước (Đuôi -tle phát âm /tl/ nghe như tồ nhẹ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/bottle--_gb_1.mp3"},
        {"word": "Middle", "ipa": "/ˈmɪdl/", "meaning": "Ở giữa (Đuôi -dle phát âm /dl/ nghe như đồ nhẹ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/middle--_gb_1.mp3"},
        {"word": "Pencil", "ipa": "/ˈpensl/", "meaning": "Bút chì (Đuôi -cil phát âm /sl/ nghe như xồ nhẹ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/pencil--_gb_1.mp3"}
    ]',
    12
),
(
    'CONSONANT_CLUSTERS',
    'Quy tắc Âm tiết phụ Syllabic M (/m̩/ - Đuôi "ừm" trong rhythm, prism, realism)',
    'Khi kết thúc bằng đuôi -thm, -sm, phụ âm /m/ tự đóng vai trò âm tiết tạo thành đuôi "ừm" trầm sâu.',
    '### 1. Bản chất Syllabic M (/m̩/):
Khi âm /m/ đi sau các phụ âm xát như **/ð/** hoặc **/z/** ở đuôi từ (như `-thm`, `-sm`, `-ism`), âm /m/ tự ngân rung trong vòm mũi tạo thành âm **"ừm"** (hoặc "đ-ừm", "z-ừm").

### 2. Ví dụ tiêu biểu:
- **Rhythm** /ˈrɪðəm/ $\rightarrow$ đọc như *"rít-đừm"* (âm /ð/ kẹp lưỡi rồi khép môi ngân /m/).
- **Prism** /ˈprɪzəm/ $\rightarrow$ đọc như *"prí-zừm"*.
- **Realism** /ˈrɪəlɪzəm/ $\rightarrow$ đọc như *"ri-ơ-li-zừm"*.
- **Criticism** /ˈkrɪtɪsɪzəm/ $\rightarrow$ đọc như *"crí-ti-si-zừm"*.
- **Bottom** /ˈbɒtəm/ $\rightarrow$ đọc như *"bót-từm"*.',
    '[
        {"word": "Rhythm", "ipa": "/ˈrɪðəm/", "meaning": "Giai điệu (Đuôi -thm phát âm nghe như đừm)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/rhythm--_gb_1.mp3"},
        {"word": "Prism", "ipa": "/ˈprɪzəm/", "meaning": "Lăng kính (Đuôi -sm phát âm nghe như zừm)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/prism--_gb_1.mp3"},
        {"word": "Bottom", "ipa": "/ˈbɒtəm/", "meaning": "Phần đáy (Đuôi -tom phát âm nghe như từm)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/bottom--_gb_1.mp3"}
    ]',
    13
),
(
    'CONSONANT_CLUSTERS',
    'Quy tắc Biến âm Flap T (/t/ biến thành /d/ nhẹ chuẩn Anh - Mỹ)',
    'Khi âm /t/ hoặc /d/ đứng giữa 2 nguyên âm và không mang trọng âm, người Mỹ biến nó thành âm vỗ Flap T (nghe như chữ "đ" nhẹ).',
    '### 1. Điều kiện xảy ra Flap T (Tap T):
1. Âm **/t/** đứng **ở giữa hai nguyên âm** (hoặc nguyên âm + /l/, /r/).
2. Âm /t/ **KHÔNG** nằm trong âm tiết nhận trọng âm chính.

### 2. Sự thay đổi âm thanh:
Người bản ngữ không bật luồng hơi mạnh thành "thơ/tơ", mà chỉ dùng đầu lưỡi vỗ nhẹ vào vòm họng trên tạo thành âm **/ɾ/** (nghe gần giống âm "đ" lướt nhẹ trong tiếng Việt):
- `Water` /ˈwɔːtər/ $\rightarrow$ Đọc là **"wo-đơ"**
- `Better` /ˈbetər/ $\rightarrow$ Đọc là **"be-đơ"**
- `City` /ˈsɪti/ $\rightarrow$ Đọc là **"si-đi"**
- `Writer` /ˈraɪtər/ $\rightarrow$ Đọc là **"rai-đơ"**
- `Computer` /kəmˈpjuːtər/ $\rightarrow$ Đọc là **"cơm-piu-đơ"**

### 3. Áp dụng khi Nối âm giữa 2 từ (Connected Speech):
- `Get out` $\rightarrow$ Đọc là **"ge-đaut"**
- `Shut up` $\rightarrow$ Đọc là **"shắ-đắp"**
- `A lot of` $\rightarrow$ Đọc là **"ơ-lo-đơv"**',
    '[
        {"word": "Water", "ipa": "/ˈwɔːtər/ -> [ˈwɔːdər]", "meaning": "Nước (Flap T đọc thành wo-đơ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/water--_gb_1.mp3"},
        {"word": "Better", "ipa": "/ˈbetər/ -> [ˈbedər]", "meaning": "Tốt hơn (Flap T đọc thành be-đơ)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/better--_gb_1.mp3"},
        {"word": "City", "ipa": "/ˈsɪti/ -> [ˈsɪdi]", "meaning": "Thành phố (Flap T đọc thành si-đi)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/city--_gb_1.mp3"},
        {"word": "Get out", "ipa": "/ɡet aʊt/ -> [ɡe daʊt]", "meaning": "Ra ngoài (Nối Flap T đọc thành ge-đaut)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/get_out--_gb_1.mp3"}
    ]',
    14
),
(
    'CONSONANT_CLUSTERS',
    'Quy tắc Âm chặn họng Glottal Stop (/ʔ/ - Nuốt âm /t/ trong button, fitness, football)',
    'Khi âm /t/ đứng trước một phụ âm khác hoặc trước âm Syllabic N, người bản xứ thường ngắt hơi đột ngột trong cổ họng (Glottal Stop) thay vì bật hơi.',
    '### 1. Hiện tượng Glottal Stop (/ʔ/) là gì?
Là kỹ thuật khép chặt hai dây thanh âm lại trong tích tắc để **chặn hoàn toàn luồng hơi** trong cổ họng, sau đó thả lỏng ngay. (Tương tự cảm giác khi nói âm "ơ... ơ" ngắt quãng).

### 2. Các trường hợp xuất hiện Glottal Stop phổ biến:
- **Đứng trước phụ âm N (/tn/):**
  - `Button` /ˈbʌʔn̩/ $\rightarrow$ "bắt (ngắt hơi) ừn"
  - `Important` /ɪmˈpɔːrʔnt/ $\rightarrow$ "im-po-(ngắt hơi)-ừn"
  - `Curtain` /ˈkɜːrʔn/ $\rightarrow$ "cơ-(ngắt hơi)-ừn"
- **Đứng trước các phụ âm khác:**
  - `Football` /ˈfʊʔbɔːl/ $\rightarrow$ "fút-(chặn hơi)-bon" (không bật chữ t)
  - `Fitness` /ˈfɪʔnəs/ $\rightarrow$ "fít-(chặn hơi)-nợt"
  - `Batman` /ˈbæʔmæn/ $\rightarrow$ "bát-(chặn hơi)-man"
- **Đứng ở cuối câu (văn nói thông dụng):**
  - `Right now` $\rightarrow$ "Raiʔ now"
  - `I can''t do it` $\rightarrow$ "I canʔ do it"',
    '[
        {"word": "Button", "ipa": "/ˈbʌʔn/", "meaning": "Cái cúc áo (Chặn họng âm /t/)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/button--_gb_1.mp3"},
        {"word": "Football", "ipa": "/ˈfʊtbɔːl/ -> [ˈfʊʔbɔːl]", "meaning": "Bóng đá (Chặn hơi âm /t/ trước /b/)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/football--_gb_1.mp3"},
        {"word": "Fitness", "ipa": "/ˈfɪtnəs/ -> [ˈfɪʔnəs]", "meaning": "Thể hình (Chặn hơi âm /t/ trước /n/)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/fitness--_gb_1.mp3"}
    ]',
    15
),
(
    'CONSONANT_CLUSTERS',
    'Quy tắc ghép Cụm phụ âm đầu (Consonant Blends: /bl/, /pl/, /cl/, /br/, /str/...)',
    'Khắc phục lỗi chèn âm "ơ/ô" phổ biến của người Việt (như đọc blue thành "bồ-lu", play thành "pờ-lây", stop thành "xì-tốp").',
    '### 1. Lỗi chèn âm (Epenthesis) của người Việt:
Do tiếng Việt là ngôn ngữ đơn âm tiết và không có cụm phụ âm đi liền nhau, người Việt thường vô thức chèn thêm âm "ơ" hoặc "ô" vào giữa hai phụ âm:
- ❌ `Blue` đọc thành "bồ-lu"
- ❌ `Black` đọc thành "bồ-lắc"
- ❌ `Play` đọc thành "pờ-lây"
- ❌ `Stop` đọc thành "xì-tốp"

### 2. Kỹ thuật ghép âm (Blending Technique) chuẩn bản ngữ:
- **Chuẩn bị khẩu hình đồng thời**: Đặt khẩu hình của phụ âm thứ nhất (/b/) đồng thời đầu lưỡi đã sẵn sàng ở vị trí của phụ âm thứ hai (/l/).
- **Bật và trượt liền mạch**: Khi vừa bật âm /b/, thả ngay luồng hơi vào vị trí âm /l/ mà **KHÔNG** để dây thanh rung tạo thành một âm tiết riêng biệt.
- **Thời lượng**: Cụm /bl/, /pl/, /br/ chỉ diễn ra trong tích tắc (< 0.1 giây) như một âm duy nhất.

### 3. Các nhóm cụm phụ âm phổ biến:
- **L-Blends**: `/bl/` (blue, black), `/cl/` (clean, clock), `/fl/` (fly, flower), `/pl/` (play, plane).
- **R-Blends**: `/br/` (bread, brother), `/cr/` (crazy, cream), `/dr/` (drive, dream), `/tr/` (tree, train).
- **S-Blends**: `/st/` (stop, star), `/sp/` (speak, space), `/sk/` (sky, school), `/str/` (street, strong).',
    '[
        {"word": "Blue", "ipa": "/bluː/", "meaning": "Màu xanh (Ghép /bl/ liền mạch, không đọc là bồ-lu)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/blue--_gb_1.mp3"},
        {"word": "Black", "ipa": "/blæk/", "meaning": "Màu đen (Ghép /bl/ liền mạch)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/black--_gb_1.mp3"},
        {"word": "Play", "ipa": "/pleɪ/", "meaning": "Chơi (Ghép /pl/ liền mạch, không đọc pờ-lây)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/play--_gb_1.mp3"},
        {"word": "Street", "ipa": "/striːt/", "meaning": "Đường phố (Ghép 3 phụ âm /str/ mượt mà)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/street--_gb_1.mp3"}
    ]',
    16
);

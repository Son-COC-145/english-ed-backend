-- =========================================================================
-- V25: Hoàn thiện dữ liệu đầy đủ Module 0 & Module 1
-- 1. Cập nhật meaning_vi còn thiếu cho từ vựng ví dụ
-- 2. Chuyển đổi toàn bộ audio_url example words từ cdn.example.com sang Google CDN thật
-- 3. Bổ sung từ ví dụ đầy đủ để toàn bộ 44 âm IPA đều có ít nhất 5 từ kèm nghĩa & ảnh
-- 4. Cập nhật audio_male_url, audio_female_url và video_mouth_url cho 44 âm IPA
-- 5. Cập nhật audio_url cho các câu hỏi LISTENING của Module 0 (Placement Test)
-- =========================================================================

-- =========================================================================
-- PHẦN 1: NGHĨA TIẾNG VIỆT CÒN THIẾU TRONG ipa_example_words
-- =========================================================================
UPDATE ipa_example_words SET meaning_vi = 'tuổi tác, thời đại'     WHERE word = 'age';
UPDATE ipa_example_words SET meaning_vi = 'bồn tắm, sự tắm rửa'    WHERE word = 'bath';
UPDATE ipa_example_words SET meaning_vi = 'to lớn'                 WHERE word = 'big';
UPDATE ipa_example_words SET meaning_vi = 'những ngày'             WHERE word = 'days';
UPDATE ipa_example_words SET meaning_vi = 'nhận được, lấy'         WHERE word = 'get';
UPDATE ipa_example_words SET meaning_vi = 'thẩm phán, xét xử'      WHERE word = 'judge';
UPDATE ipa_example_words SET meaning_vi = 'dài'                    WHERE word = 'long';
UPDATE ipa_example_words SET meaning_vi = 'que diêm, trận đấu'     WHERE word = 'match';
UPDATE ipa_example_words SET meaning_vi = 'ca hát'                 WHERE word = 'sing';
UPDATE ipa_example_words SET meaning_vi = 'vườn thú, sở thú'       WHERE word = 'zoo';

-- =========================================================================
-- PHẦN 2: CHUYỂN ĐỔI AUDIO EXAMPLE WORDS CÒN DÙNG cdn.example.com SANG GOOGLE CDN
-- =========================================================================
UPDATE ipa_example_words
SET audio_url = 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/' || word || '--_gb_1.mp3'
WHERE audio_url LIKE '%example.com%';

-- =========================================================================
-- PHẦN 3: BỔ SUNG TỪ VÍ DỤ ĐỂ MỌI ÂM IPA ĐỀU ĐẠT TỐI THIỂU 5 TỪ
--         (Đầy đủ phonetic transcription, audio URL, nghĩa tiếng Việt và ảnh Unsplash)
-- =========================================================================
INSERT INTO ipa_example_words (phoneme_id, word, ipa_transcription, audio_url, meaning_vi, image_url) VALUES

-- /aʊ/ (hiện có 2: now, house -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'aʊ'), 'cloud', '/klaʊd/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cloud--_gb_1.mp3', 'đám mây', 'https://source.unsplash.com/200x200/?cloud,sky'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'aʊ'), 'mouth', '/maʊθ/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/mouth--_gb_1.mp3', 'cái miệng', 'https://source.unsplash.com/200x200/?mouth,lips'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'aʊ'), 'town',  '/taʊn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/town--_gb_1.mp3',  'thị trấn',  'https://source.unsplash.com/200x200/?town,city'),

-- /əʊ/ (hiện có 2: go, home -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'əʊ'), 'phone', '/fəʊn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/phone--_gb_1.mp3', 'điện thoại', 'https://source.unsplash.com/200x200/?phone,mobile'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'əʊ'), 'road',  '/rəʊd/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/road--_gb_1.mp3',  'con đường',  'https://source.unsplash.com/200x200/?road,highway'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'əʊ'), 'snow',  '/snəʊ/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/snow--_gb_1.mp3',  'tuyết',      'https://source.unsplash.com/200x200/?snow,winter'),

-- /ɪə/ (hiện có 2: ear, here -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪə'), 'year',  '/jɪər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/year--_gb_1.mp3',  'năm',        'https://source.unsplash.com/200x200/?year,calendar'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪə'), 'fear',  '/fɪər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/fear--_gb_1.mp3',  'sợ hãi',     'https://source.unsplash.com/200x200/?fear,dark'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪə'), 'near',  '/nɪər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/near--_gb_1.mp3',  'gần',        'https://source.unsplash.com/200x200/?near,close'),

-- /eə/ (hiện có 2: air, where -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'eə'), 'hair',  '/heər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/hair--_gb_1.mp3',  'tóc',        'https://source.unsplash.com/200x200/?hair,woman'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'eə'), 'bear',  '/beər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bear--_gb_1.mp3',  'con gấu',    'https://source.unsplash.com/200x200/?bear,animal'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'eə'), 'share', '/ʃeər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/share--_gb_1.mp3', 'chia sẻ',    'https://source.unsplash.com/200x200/?share,social'),

-- /ʊə/ (hiện có 2: tour, pure -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊə'), 'poor',  '/pʊər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/poor--_gb_1.mp3',  'nghèo',      'https://source.unsplash.com/200x200/?poor,sad'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊə'), 'sure',  '/ʃʊər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sure--_gb_1.mp3',  'chắc chắn',  'https://source.unsplash.com/200x200/?sure,confident'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊə'), 'cure',  '/kjʊər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cure--_gb_1.mp3',  'chữa khỏi',  'https://source.unsplash.com/200x200/?cure,medicine'),

-- /p/ (hiện có 2: pen, copy -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'p'),  'park',   '/pɑːrk/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/park--_gb_1.mp3',  'công viên',  'https://source.unsplash.com/200x200/?park,garden'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'p'),  'people', '/ˈpiːpl/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/people--_gb_1.mp3', 'mọi người', 'https://source.unsplash.com/200x200/?people,crowd'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'p'),  'place',  '/pleɪs/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/place--_gb_1.mp3',  'địa điểm',   'https://source.unsplash.com/200x200/?place,location'),

-- /b/ (hiện có 2: book, back -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'b'),  'ball',   '/bɔːl/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/ball--_gb_1.mp3',  'quả bóng',   'https://source.unsplash.com/200x200/?ball,sport'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'b'),  'baby',   '/ˈbeɪbi/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/baby--_gb_1.mp3',  'em bé',      'https://source.unsplash.com/200x200/?baby,child'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'b'),  'blue',   '/bluː/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/blue--_gb_1.mp3',   'màu xanh',   'https://source.unsplash.com/200x200/?blue,color'),

-- /t/ (hiện có 2: top, sit -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 't'),  'tea',    '/tiː/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/tea--_gb_1.mp3',   'tách trà',   'https://source.unsplash.com/200x200/?tea,cup'),
((SELECT id FROM ipa_phonemes WHERE symbol = 't'),  'tree',   '/triː/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/tree--_gb_1.mp3',  'cái cây',    'https://source.unsplash.com/200x200/?tree,nature'),
((SELECT id FROM ipa_phonemes WHERE symbol = 't'),  'town',   '/taʊn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/town--_gb_1.mp3',  'thị trấn',   'https://source.unsplash.com/200x200/?town,city'),

-- /d/ (hiện có 2: dog, red -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'd'),  'day',    '/deɪ/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/day--_gb_1.mp3',   'ngày',       'https://source.unsplash.com/200x200/?day,sunny'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'd'),  'door',   '/dɔːr/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/door--_gb_1.mp3',  'cái cửa',    'https://source.unsplash.com/200x200/?door,entrance'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'd'),  'drink',  '/drɪŋk/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/drink--_gb_1.mp3', 'uống nước',  'https://source.unsplash.com/200x200/?drink,water'),

-- /k/ (hiện có 2: cat, back -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'k'),  'key',    '/kiː/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/key--_gb_1.mp3',   'chìa khóa',  'https://source.unsplash.com/200x200/?key,lock'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'k'),  'clock',  '/klɒk/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/clock--_gb_1.mp3', 'đồng hồ',    'https://source.unsplash.com/200x200/?clock,time'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'k'),  'cook',   '/kʊk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cook--_gb_1.mp3',  'nấu ăn',     'https://source.unsplash.com/200x200/?cook,kitchen'),

-- /g/ (hiện có 2: get, big -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'g'),  'girl',   '/ɡɜːrl/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/girl--_gb_1.mp3',  'cô gái',     'https://source.unsplash.com/200x200/?girl,child'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'g'),  'gold',   '/ɡəʊld/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/gold--_gb_1.mp3',  'vàng bạc',   'https://source.unsplash.com/200x200/?gold,metal'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'g'),  'green',  '/ɡriːn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/green--_gb_1.mp3', 'màu xanh lá','https://source.unsplash.com/200x200/?green,nature'),

-- /f/ (hiện có 2: five, leaf -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'f'),  'fast',   '/fɑːst/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/fast--_gb_1.mp3',  'nhanh nhẹn', 'https://source.unsplash.com/200x200/?fast,runner'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'f'),  'face',   '/feɪs/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/face--_gb_1.mp3',  'khuôn mặt',  'https://source.unsplash.com/200x200/?face,portrait'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'f'),  'fox',    '/fɒks/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/fox--_gb_1.mp3',   'con cáo',    'https://source.unsplash.com/200x200/?fox,animal'),

-- /v/ (hiện có 2: van, live -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'v'),  'voice',  '/vɔɪs/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/voice--_gb_1.mp3', 'giọng nói',  'https://source.unsplash.com/200x200/?voice,microphone'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'v'),  'view',   '/vjuː/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/view--_gb_1.mp3',  'quang cảnh', 'https://source.unsplash.com/200x200/?view,landscape'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'v'),  'save',   '/seɪv/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/save--_gb_1.mp3',  'cứu, tiết kiệm','https://source.unsplash.com/200x200/?save,piggybank'),

-- /s/ (hiện có 2: sun, bus -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 's'),  'star',   '/stɑːr/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/star--_gb_1.mp3',  'ngôi sao',   'https://source.unsplash.com/200x200/?star,night'),
((SELECT id FROM ipa_phonemes WHERE symbol = 's'),  'sweet',  '/swiːt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sweet--_gb_1.mp3', 'ngọt ngào',  'https://source.unsplash.com/200x200/?sweet,candy'),
((SELECT id FROM ipa_phonemes WHERE symbol = 's'),  'sister', '/ˈsɪstər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sister--_gb_1.mp3', 'chị em gái', 'https://source.unsplash.com/200x200/?sister,family'),

-- /h/ (hiện có 2: hat, ahead -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'h'),  'hand',   '/hænd/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/hand--_gb_1.mp3',  'bàn tay',    'https://source.unsplash.com/200x200/?hand,touch'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'h'),  'head',   '/hed/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/head--_gb_1.mp3',   'cái đầu',    'https://source.unsplash.com/200x200/?head,thinking'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'h'),  'hope',   '/həʊp/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/hope--_gb_1.mp3',  'hy vọng',    'https://source.unsplash.com/200x200/?hope,sunrise'),

-- /m/ (hiện có 2: man, home -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'm'),  'milk',   '/mɪlk/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/milk--_gb_1.mp3',  'sữa tươi',   'https://source.unsplash.com/200x200/?milk,glass'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'm'),  'money',  '/ˈmʌni/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/money--_gb_1.mp3', 'tiền bạc',   'https://source.unsplash.com/200x200/?money,cash'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'm'),  'summer', '/ˈsʌmər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/summer--_gb_1.mp3', 'mùa hè',    'https://source.unsplash.com/200x200/?summer,beach'),

-- /n/ (hiện có 2: name, sun -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'n'),  'nose',   '/nəʊz/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/nose--_gb_1.mp3',  'chiếc mũi',  'https://source.unsplash.com/200x200/?nose,face'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'n'),  'nice',   '/naɪs/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/nice--_gb_1.mp3',  'tử tế, đẹp', 'https://source.unsplash.com/200x200/?nice,kindness'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'n'),  'nine',   '/naɪn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/nine--_gb_1.mp3',  'số chín',    'https://source.unsplash.com/200x200/?nine,number'),

-- /l/ (hiện có 2: leg, all -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'l'),  'light',  '/laɪt/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/light--_gb_1.mp3', 'ánh sáng',   'https://source.unsplash.com/200x200/?light,lamp'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'l'),  'lemon',  '/ˈlemən/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/lemon--_gb_1.mp3', 'quả chanh',  'https://source.unsplash.com/200x200/?lemon,fruit'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'l'),  'lake',   '/leɪk/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/lake--_gb_1.mp3',   'hồ nước',    'https://source.unsplash.com/200x200/?lake,water'),

-- /r/ (hiện có 2: red, very -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'r'),  'run',    '/rʌn/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/run--_gb_1.mp3',   'chạy bộ',    'https://source.unsplash.com/200x200/?run,sport'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'r'),  'room',   '/ruːm/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/room--_gb_1.mp3',  'căn phòng',  'https://source.unsplash.com/200x200/?room,interior'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'r'),  'rain',   '/reɪn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/rain--_gb_1.mp3',  'cơn mưa',    'https://source.unsplash.com/200x200/?rain,water'),

-- /w/ (hiện có 2: we, away -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'w'),  'water',  '/ˈwɔːtər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/water--_gb_1.mp3', 'nước uống', 'https://source.unsplash.com/200x200/?water,glass'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'w'),  'wind',   '/wɪnd/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/wind--_gb_1.mp3',  'cơn gió',    'https://source.unsplash.com/200x200/?wind,sky'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'w'),  'warm',   '/wɔːm/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/warm--_gb_1.mp3',  'ấm áp',      'https://source.unsplash.com/200x200/?warm,sun'),

-- /j/ (hiện có 2: yes, you -> bổ sung 3 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'j'),  'yellow', '/ˈjeləʊ/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/yellow--_gb_1.mp3', 'màu vàng', 'https://source.unsplash.com/200x200/?yellow,color'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'j'),  'young',  '/jʌŋ/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/young--_gb_1.mp3',  'trẻ tuổi',   'https://source.unsplash.com/200x200/?young,people'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'j'),  'yard',   '/jɑːrd/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/yard--_gb_1.mp3',   'sân vườn',   'https://source.unsplash.com/200x200/?yard,garden'),

-- /ʒ/ (hiện có 3 -> bổ sung 2 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'),  'treasure', '/ˈtreʒər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/treasure--_gb_1.mp3', 'kho báu', 'https://source.unsplash.com/200x200/?treasure,chest'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'),  'television', '/ˈtelɪvɪʒn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/television--_gb_1.mp3', 'ti vi', 'https://source.unsplash.com/200x200/?television,tv'),

-- /tʃ/ (hiện có 4 -> bổ sung 1 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'tʃ'), 'cheese', '/tʃiːz/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cheese--_gb_1.mp3', 'phô mai', 'https://source.unsplash.com/200x200/?cheese,food'),

-- /ð/ (hiện có 4 -> bổ sung 1 từ)
((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'),  'weather', '/ˈweðər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/weather--_gb_1.mp3', 'thời tiết', 'https://source.unsplash.com/200x200/?weather,cloud')

ON CONFLICT DO NOTHING;

-- =========================================================================
-- PHẦN 4: CẬP NHẬT AUDIO & VIDEO KHẨU HÌNH CHO 44 ÂM IPA
-- - audio_male_url, audio_female_url: trỏ đến backend TTS stream
-- - video_mouth_url: hướng dẫn khẩu hình phát âm chuẩn BBC Learning English
-- =========================================================================

-- Cập nhật video_mouth_url mặc định cho toàn bộ âm (nếu chưa có)
UPDATE ipa_phonemes
SET video_mouth_url = 'https://www.youtube.com/embed/videoseries?list=PLD6B222E02447DC07'
WHERE video_mouth_url IS NULL;

-- 12 Nguyên âm đơn (Monophthongs)
UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=sheep&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=sheep&type=WORD&voice=FEMALE'
WHERE symbol = 'iː';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=ship&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=ship&type=WORD&voice=FEMALE'
WHERE symbol = 'ɪ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=bed&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=bed&type=WORD&voice=FEMALE'
WHERE symbol = 'e';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=cat&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=cat&type=WORD&voice=FEMALE'
WHERE symbol = 'æ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=car&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=car&type=WORD&voice=FEMALE'
WHERE symbol = 'ɑː';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=hot&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=hot&type=WORD&voice=FEMALE'
WHERE symbol = 'ɒ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=call&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=call&type=WORD&voice=FEMALE'
WHERE symbol = 'ɔː';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=book&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=book&type=WORD&voice=FEMALE'
WHERE symbol = 'ʊ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=moon&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=moon&type=WORD&voice=FEMALE'
WHERE symbol = 'uː';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=cup&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=cup&type=WORD&voice=FEMALE'
WHERE symbol = 'ʌ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=bird&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=bird&type=WORD&voice=FEMALE'
WHERE symbol = 'ɜː';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=about&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=about&type=WORD&voice=FEMALE'
WHERE symbol = 'ə';

-- 8 Nguyên âm đôi (Diphthongs)
UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=day&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=day&type=WORD&voice=FEMALE'
WHERE symbol = 'eɪ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=fly&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=fly&type=WORD&voice=FEMALE'
WHERE symbol = 'aɪ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=boy&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=boy&type=WORD&voice=FEMALE'
WHERE symbol = 'ɔɪ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=now&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=now&type=WORD&voice=FEMALE'
WHERE symbol = 'aʊ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=go&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=go&type=WORD&voice=FEMALE'
WHERE symbol = 'əʊ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=ear&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=ear&type=WORD&voice=FEMALE'
WHERE symbol = 'ɪə';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=air&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=air&type=WORD&voice=FEMALE'
WHERE symbol = 'eə';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=tour&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=tour&type=WORD&voice=FEMALE'
WHERE symbol = 'ʊə';

-- 24 Phụ âm (Consonants)
UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=pen&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=pen&type=WORD&voice=FEMALE'
WHERE symbol = 'p';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=book&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=book&type=WORD&voice=FEMALE'
WHERE symbol = 'b';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=top&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=top&type=WORD&voice=FEMALE'
WHERE symbol = 't';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=dog&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=dog&type=WORD&voice=FEMALE'
WHERE symbol = 'd';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=cat&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=cat&type=WORD&voice=FEMALE'
WHERE symbol = 'k';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=get&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=get&type=WORD&voice=FEMALE'
WHERE symbol = 'g';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=chair&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=chair&type=WORD&voice=FEMALE'
WHERE symbol = 'tʃ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=judge&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=judge&type=WORD&voice=FEMALE'
WHERE symbol = 'dʒ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=five&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=five&type=WORD&voice=FEMALE'
WHERE symbol = 'f';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=van&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=van&type=WORD&voice=FEMALE'
WHERE symbol = 'v';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=think&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=think&type=WORD&voice=FEMALE'
WHERE symbol = 'θ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=this&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=this&type=WORD&voice=FEMALE'
WHERE symbol = 'ð';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=sun&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=sun&type=WORD&voice=FEMALE'
WHERE symbol = 's';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=zoo&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=zoo&type=WORD&voice=FEMALE'
WHERE symbol = 'z';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=shoe&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=shoe&type=WORD&voice=FEMALE'
WHERE symbol = 'ʃ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=vision&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=vision&type=WORD&voice=FEMALE'
WHERE symbol = 'ʒ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=hat&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=hat&type=WORD&voice=FEMALE'
WHERE symbol = 'h';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=man&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=man&type=WORD&voice=FEMALE'
WHERE symbol = 'm';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=name&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=name&type=WORD&voice=FEMALE'
WHERE symbol = 'n';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=sing&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=sing&type=WORD&voice=FEMALE'
WHERE symbol = 'ŋ';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=leg&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=leg&type=WORD&voice=FEMALE'
WHERE symbol = 'l';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=red&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=red&type=WORD&voice=FEMALE'
WHERE symbol = 'r';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=we&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=we&type=WORD&voice=FEMALE'
WHERE symbol = 'w';

UPDATE ipa_phonemes SET
    audio_male_url   = '/api/v1/ipa/phonemes/tts/stream?text=yes&type=WORD&voice=MALE',
    audio_female_url = '/api/v1/ipa/phonemes/tts/stream?text=yes&type=WORD&voice=FEMALE'
WHERE symbol = 'j';

-- =========================================================================
-- PHẦN 5: CẬP NHẬT AUDIO CHO CÁC CÂU HỎI LISTENING TRONG MODULE 0 (PLACEMENT TEST)
-- Dùng TTS stream endpoint — backend phát audio thoại rõ ràng tương ứng từng câu hỏi
-- =========================================================================

-- A1: "What is the boy's name?" -> Lời thoại: "Hi, my name is John. I am eight years old."
UPDATE questions
SET content_json = jsonb_set(
    content_json,
    '{audio_url}',
    '"/api/v1/ipa/phonemes/tts/stream?text=Hi%2C+my+name+is+John.+I+am+eight+years+old.&type=WORD&voice=FEMALE"'::jsonb
)
WHERE skill = 'LISTENING'
  AND cefr_level = 'A1'
  AND content_json->>'audio_url' LIKE '%example.com%'
  AND content_json->>'question' LIKE '%name%';

-- A2: "What time does the train leave?" -> Lời thoại: "The next train to London leaves at nine thirty."
UPDATE questions
SET content_json = jsonb_set(
    content_json,
    '{audio_url}',
    '"/api/v1/ipa/phonemes/tts/stream?text=Attention+please.+The+next+train+to+London+leaves+at+nine+thirty.&type=WORD&voice=FEMALE"'::jsonb
)
WHERE skill = 'LISTENING'
  AND cefr_level = 'A2'
  AND content_json->>'audio_url' LIKE '%example.com%';

-- B1: "Why is the woman calling?" -> Lời thoại: "Good afternoon. I am calling to cancel my reservation."
UPDATE questions
SET content_json = jsonb_set(
    content_json,
    '{audio_url}',
    '"/api/v1/ipa/phonemes/tts/stream?text=Good+afternoon.+I+am+calling+about+my+reservation.+I+would+like+to+cancel+my+booking+for+next+Friday%2C+please.&type=WORD&voice=FEMALE"'::jsonb
)
WHERE skill = 'LISTENING'
  AND cefr_level = 'B1'
  AND content_json->>'audio_url' LIKE '%example.com%';

-- B2: "What is the main topic?" -> Lời thoại: "Today we focus on how technology impacts society."
UPDATE questions
SET content_json = jsonb_set(
    content_json,
    '{audio_url}',
    '"/api/v1/ipa/phonemes/tts/stream?text=Today+we+will+focus+on+how+modern+technology+has+fundamentally+transformed+our+society+and+daily+lives.&type=WORD&voice=MALE"'::jsonb
)
WHERE skill = 'LISTENING'
  AND cefr_level = 'B2'
  AND content_json->>'audio_url' LIKE '%example.com%';

-- C1: "What is the speaker's attitude?" -> Lời thoại: "I remain cautiously optimistic about the new policy."
UPDATE questions
SET content_json = jsonb_set(
    content_json,
    '{audio_url}',
    '"/api/v1/ipa/phonemes/tts/stream?text=While+I+appreciate+the+government+efforts%2C+I+remain+cautiously+optimistic+about+the+long-term+impact+of+this+new+policy.&type=WORD&voice=MALE"'::jsonb
)
WHERE skill = 'LISTENING'
  AND cefr_level = 'C1'
  AND content_json->>'audio_url' LIKE '%example.com%';

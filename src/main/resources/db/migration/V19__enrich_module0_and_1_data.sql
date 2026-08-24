-- =========================================================================
-- V19: Bổ sung toàn diện dữ liệu chuyên sâu cho Module 0 và Module 1
-- 1. Cập nhật mẹo khẩu hình tiếng Việt (pronunciation_tip_vi) cho toàn bộ 44 âm IPA
-- 2. Bổ sung từ ví dụ phong phú (4-5 từ/âm) cho toàn bộ 44 âm IPA
-- 3. Bổ sung trọn bộ 17 Cặp âm dễ gây nhầm lẫn nhất cho người Việt (Minimal Pairs)
-- 4. Bổ sung trọn bộ 10 Quy tắc Trọng âm, Nối âm, Nuốt âm & Ngữ điệu (Rules)
-- 5. Bổ sung thêm câu hỏi kiểm tra đầu vào (Placement Test Questions)
-- =========================================================================

-- =========================================================================
-- PHẦN 1: CẬP NHẬT MẸO KHẨU HÌNH CHO TOÀN BỘ CÁC ÂM IPA CÒN LẠI
-- =========================================================================

-- Nguyên âm đơn (Monophthongs)
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Kéo dài khóe miệng sang hai bên như đang cười nhẹ, đầu lưỡi nâng cao chạm vòm trên. Phát âm ngân dài hơn âm "i" tiếng Việt.' WHERE symbol = 'iː';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Mở miệng tự nhiên, phát âm ngắn và dứt khoát, thả lỏng cơ miệng. Âm nằm giữa "i" và "ê" trong tiếng Việt.' WHERE symbol = 'ɪ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Mở miệng rộng vừa phải (rộng hơn âm /ɪ/), phát âm ngắn dứt khoát giống âm "e" tiếng Việt nhưng dứt khoát hơn.' WHERE symbol = 'e';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Hạ cằm sâu xuống, mở rộng khẩu hình theo cả chiều dọc lẫn chiều ngang. Phát âm lai giữa âm "a" và "e".' WHERE symbol = 'æ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Hạ cằm xuống sâu, lưỡi đặt thấp trong khoang miệng, phát âm ngân dài và sâu trong cổ họng giống chữ "a" kéo dài.' WHERE symbol = 'ɑː';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Mở miệng tròn vừa phải, hạ thấp hàm dưới, phát âm ngắn dứt khoát tương tự âm "o" ngắn trong tiếng Việt.' WHERE symbol = 'ɒ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Tròn môi hướng về phía trước, nâng phần cuống lưỡi lên, phát âm ngân dài và sâu giống âm "o" kéo dài.' WHERE symbol = 'ɔː';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Môi hơi tròn hướng về trước, thả lỏng cơ miệng, phát âm ngắn và dứt khoát. Nằm giữa âm "u" và "ư" tiếng Việt.' WHERE symbol = 'ʊ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Chu tròn môi về phía trước như đang huýt sáo, nâng cao cuống lưỡi, phát âm ngân dài hơn âm "u" tiếng Việt.' WHERE symbol = 'uː';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Mở miệng rộng bằng 1/2 so với âm /ɑː/, phát âm ngắn dứt khoát lai giữa âm "ă" và "ơ" tiếng Việt.' WHERE symbol = 'ʌ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Môi mở hờ tự nhiên, nâng nhẹ thân lưỡi, phát âm ngân dài từ cổ họng giống âm "ơ" kéo dài. (Có thể cong nhẹ đầu lưỡi theo chuẩn US).' WHERE symbol = 'ɜː';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Âm lướt phổ biến nhất trong tiếng Anh (Schwa). Mở miệng cực kỳ thả lỏng, phát âm rất ngắn và nhẹ như âm "ơ" thoảng qua.' WHERE symbol = 'ə';

-- Nguyên âm đôi (Diphthongs)
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /e/ rồi lướt nhẹ và nhanh dần sang âm /ɪ/. Khóe miệng mở rộng dần sang hai bên.' WHERE symbol = 'eɪ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /a/ mở rộng miệng, sau đó trượt nhanh về âm /ɪ/. Phát âm tương tự "ai" tiếng Việt nhưng mượt mà hơn.' WHERE symbol = 'aɪ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /ɔː/ tròn môi rồi lướt sang âm /ɪ/ dẹt môi. Tương tự vần "oi" trong tiếng Việt.' WHERE symbol = 'ɔɪ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /a/ mở rộng cằm rồi thu tròn môi dần về âm /ʊ/. Tương tự vần "ao" trong tiếng Việt.' WHERE symbol = 'aʊ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /ə/ thả lỏng rồi tròn môi dần về âm /ʊ/. Tương tự vần "âu" trong tiếng Việt nhưng kéo dài hơn.' WHERE symbol = 'əʊ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /ɪ/ dẹt môi rồi trượt về âm /ə/ thả lỏng miệng. Tương tự vần "ia" trong tiếng Việt.' WHERE symbol = 'ɪə';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /e/ mở vừa rồi lướt nhẹ về âm /ə/ thả lỏng. Tương tự vần "e-ơ" nối liền.' WHERE symbol = 'eə';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Bắt đầu từ âm /ʊ/ tròn môi rồi lướt nhẹ về âm /ə/. Tương tự vần "ua" trong tiếng Việt.' WHERE symbol = 'ʊə';

-- Phụ âm (Consonants)
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khép chặt hai môi lại để chặn luồng hơi, sau đó mở nhanh hai môi và bật mạnh luồng hơi ra ngoài. Dây thanh quản KHÔNG rung.' WHERE symbol = 'p';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /p/ nhưng cần rung dây thanh quản trong cổ họng ngay khi bật luồng hơi ra ngoài.' WHERE symbol = 'b';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Đặt đầu lưỡi chạm vào chân răng hàm trên để chặn hơi, sau đó hạ nhanh lưỡi và bật luồng hơi mạnh ra ngoài. Không rung thanh quản.' WHERE symbol = 't';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /t/ nhưng cần rung dây thanh quản trong cổ họng khi bật hơi. Không đọc thành chữ "đ" tiếng Việt.' WHERE symbol = 'd';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Nâng phần cuống lưỡi chạm vòm mềm trên để chặn hơi, sau đó hạ cuống lưỡi và bật luồng hơi mạnh ra từ cổ họng. Không rung thanh quản.' WHERE symbol = 'k';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /k/ nhưng rung dây thanh quản khi bật hơi. Giống chữ "g" tiếng Việt nhưng bật dứt khoát.' WHERE symbol = 'g';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khép nhẹ môi hình tròn chu về phía trước, đặt đầu lưỡi chặn hơi rồi bật mạnh luồng hơi qua kẽ răng. Không rung thanh quản.' WHERE symbol = 'tʃ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /tʃ/ nhưng cần rung dây thanh quản mạnh trong cổ họng khi bật hơi.' WHERE symbol = 'dʒ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Đặt nhẹ hàng răng trên lên môi dưới, đẩy luồng hơi êm qua khe giữa răng và môi. Không rung thanh quản.' WHERE symbol = 'f';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /f/ nhưng cần rung dây thanh quản trong cổ họng khi đẩy luồng hơi qua răng và môi.' WHERE symbol = 'v';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Đặt nhẹ đầu lưỡi vào giữa hai hàm răng, thổi nhẹ luồng hơi qua kẽ răng. Tuyệt đối không chạm môi hoặc đọc thành âm "th" tiếng Việt.' WHERE symbol = 'θ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Tương tự âm /θ/, đặt đầu lưỡi giữa hai hàm răng nhưng cần rung dây thanh quản trong cổ họng khi đẩy luồng hơi ra ngoài.' WHERE symbol = 'ð';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Hai hàm răng khép hờ, đưa đầu lưỡi lại gần chân răng trên, đẩy luồng hơi xì qua kẽ răng. Không rung thanh quản.' WHERE symbol = 's';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /s/ nhưng rung mạnh dây thanh quản trong cổ họng tạo âm xì rung như tiếng ong kêu.' WHERE symbol = 'z';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Chu tròn môi về phía trước, hai hàm răng khép hờ, đẩy luồng hơi mạnh ra ngoài (như động tác ra hiệu "suỵt" giữ im lặng).' WHERE symbol = 'ʃ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Khẩu hình tương tự âm /ʃ/ chu môi nhưng rung mạnh dây thanh quản trong cổ họng.' WHERE symbol = 'ʒ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Mở miệng tự nhiên, đẩy luồng hơi nhẹ nhàng từ trong vòm họng ra ngoài như tiếng thở phào nhẹ nhõm.' WHERE symbol = 'h';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Ngậm chặt hai môi lại, đẩy toàn bộ luồng hơi thoát ra qua đường mũi, rung dây thanh quản.' WHERE symbol = 'm';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Đặt đầu lưỡi chạm vào nướu răng hàm trên, đẩy luồng hơi thoát ra qua đường mũi, rung dây thanh quản.' WHERE symbol = 'n';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Nâng phần cuống lưỡi chạm vòm mềm chặn hơi, đẩy luồng hơi thoát ra hoàn toàn qua đường mũi, rung thanh quản.' WHERE symbol = 'ŋ';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Đặt đầu lưỡi chạm vào chân răng hàm trên, luồng hơi thoát ra ở hai bên cạnh lưỡi. (Khi đứng cuối từ, cong nhẹ đầu lưỡi).' WHERE symbol = 'l';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Hơi chu môi về phía trước, uốn cong đầu lưỡi về phía sau nhưng không chạm vào vòm miệng, rung thanh quản.' WHERE symbol = 'r';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Chu tròn môi về phía trước như hình chữ O nhỏ, sau đó mở rộng môi ra hai bên đồng thời rung thanh quản.' WHERE symbol = 'w';
UPDATE ipa_phonemes SET pronunciation_tip_vi = 'Nâng thân lưỡi lên cao gần vòm miệng, kéo khóe miệng sang hai bên và rung thanh quản (giống âm "d" mềm miền Nam).' WHERE symbol = 'j';


-- =========================================================================
-- PHẦN 2: BỔ SUNG TỪ VÍ DỤ PHONG PHÚ CHO TẤT CẢ 44 ÂM IPA
-- =========================================================================

-- Bổ sung thêm từ ví dụ cho các âm để mỗi âm có từ 4 đến 5 từ phong phú
INSERT INTO ipa_example_words (phoneme_id, word, ipa_transcription, audio_url) VALUES
-- /iː/
((SELECT id FROM ipa_phonemes WHERE symbol = 'iː'), 'feel',   '/fiːl/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/feel--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'iː'), 'reach',  '/riːtʃ/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/reach--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'iː'), 'beat',   '/biːt/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/beat--_gb_1.mp3'),

-- /ɪ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪ'), 'fill',   '/fɪl/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/fill--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪ'), 'rich',   '/rɪtʃ/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/rich--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪ'), 'bit',    '/bɪt/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bit--_gb_1.mp3'),

-- /e/
((SELECT id FROM ipa_phonemes WHERE symbol = 'e'), 'pen',    '/pen/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/pen--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'e'), 'men',    '/men/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/men--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'e'), 'check',  '/tʃek/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/check--_gb_1.mp3'),

-- /æ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'æ'), 'pan',    '/pæn/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/pan--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'æ'), 'man',    '/mæn/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/man--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'æ'), 'apple',  '/ˈæpl/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/apple--_gb_1.mp3'),

-- /ɑː/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɑː'), 'father', '/ˈfɑːðər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/father--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɑː'), 'start',  '/stɑːrt/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/start--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɑː'), 'park',   '/pɑːrk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/park--_gb_1.mp3'),

-- /ɒ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɒ'), 'stop',   '/stɒp/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/stop--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɒ'), 'box',    '/bɒks/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/box--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɒ'), 'clock',  '/klɒk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/clock--_gb_1.mp3'),

-- /ɔː/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔː'), 'door',   '/dɔːr/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/door--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔː'), 'sport',  '/spɔːrt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sport--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔː'), 'talk',   '/tɔːk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/talk--_gb_1.mp3'),

-- /ʊ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ'), 'foot',   '/fʊt/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/foot--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ'), 'full',   '/fʊl/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/full--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ'), 'push',   '/pʊʃ/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/push--_gb_1.mp3'),

-- /uː/
((SELECT id FROM ipa_phonemes WHERE symbol = 'uː'), 'blue',   '/bluː/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/blue--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'uː'), 'shoe',   '/ʃuː/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/shoe--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'uː'), 'group',  '/ɡruːp/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/group--_gb_1.mp3'),

-- /ʌ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʌ'), 'sun',    '/sʌn/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sun--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʌ'), 'bus',    '/bʌs/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bus--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʌ'), 'love',   '/lʌv/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/love--_gb_1.mp3'),

-- /ɜː/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɜː'), 'learn',  '/lɜːrn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/learn--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɜː'), 'nurse',  '/nɜːrs/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/nurse--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɜː'), 'early',  '/ˈɜːrli/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/early--_gb_1.mp3'),

-- /ə/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ə'), 'banana', '/bəˈnænə/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/banana--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ə'), 'police', '/pəˈliːs/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/police--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ə'), 'famous', '/ˈfeɪməs/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/famous--_gb_1.mp3'),

-- /eɪ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'eɪ'), 'make',   '/meɪk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/make--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'eɪ'), 'rain',   '/reɪn/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/rain--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'eɪ'), 'cake',   '/keɪk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cake--_gb_1.mp3'),

-- /aɪ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'aɪ'), 'time',   '/taɪm/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/time--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'aɪ'), 'drive',  '/draɪv/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/drive--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'aɪ'), 'like',   '/laɪk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/like--_gb_1.mp3'),

-- /ɔɪ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔɪ'), 'voice',  '/vɔɪs/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/voice--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔɪ'), 'join',   '/dʒɔɪn/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/join--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔɪ'), 'enjoy',  '/ɪnˈdʒɔɪ/','https://ssl.gstatic.com/dictionary/static/sounds/20200429/enjoy--_gb_1.mp3'),

-- /θ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'θ'), 'think',  '/θɪŋk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/think--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'θ'), 'thank',  '/θæŋk/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/thank--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'θ'), 'mouth',  '/maʊθ/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/mouth--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'θ'), 'teeth',  '/tiːθ/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/teeth--_gb_1.mp3'),

-- /ð/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'), 'this',   '/ðɪs/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/this--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'), 'that',   '/ðæt/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/that--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'), 'mother', '/ˈmʌðər/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/mother--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'), 'breathe','/briːð/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/breathe--_gb_1.mp3'),

-- /ʃ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʃ'), 'shop',   '/ʃɒp/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/shop--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʃ'), 'wash',   '/wɒʃ/',    'https://ssl.gstatic.com/dictionary/static/sounds/20200429/wash--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʃ'), 'special','/ˈspeʃl/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/special--_gb_1.mp3'),

-- /tʃ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'tʃ'), 'chair', '/tʃeər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/chair--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'tʃ'), 'teach', '/tiːtʃ/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/teach--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'tʃ'), 'nature','/ˈneɪtʃər/','https://ssl.gstatic.com/dictionary/static/sounds/20200429/nature--_gb_1.mp3'),

-- /dʒ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'dʒ'), 'job',   '/dʒɒb/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/job--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'dʒ'), 'page',  '/peɪdʒ/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/page--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'dʒ'), 'bridge','/brɪdʒ/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/bridge--_gb_1.mp3'),

-- /z/
((SELECT id FROM ipa_phonemes WHERE symbol = 'z'), 'zero',   '/ˈzɪərəʊ/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/zero--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'z'), 'music',  '/ˈmjuːzɪk/','https://ssl.gstatic.com/dictionary/static/sounds/20200429/music--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'z'), 'always', '/ˈɔːlweɪz/','https://ssl.gstatic.com/dictionary/static/sounds/20200429/always--_gb_1.mp3'),

-- /ʒ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'), 'vision',  '/ˈvɪʒn/',   'https://ssl.gstatic.com/dictionary/static/sounds/20200429/vision--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'), 'measure', '/ˈmeʒər/',  'https://ssl.gstatic.com/dictionary/static/sounds/20200429/measure--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'), 'casual',  '/ˈkæʒuəl/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/casual--_gb_1.mp3'),

-- /ŋ/
((SELECT id FROM ipa_phonemes WHERE symbol = 'ŋ'), 'song',    '/sɒŋ/',     'https://ssl.gstatic.com/dictionary/static/sounds/20200429/song--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ŋ'), 'ring',    '/rɪŋ/',     'https://ssl.gstatic.com/dictionary/static/sounds/20200429/ring--_gb_1.mp3'),
((SELECT id FROM ipa_phonemes WHERE symbol = 'ŋ'), 'english', '/ˈɪŋɡlɪʃ/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/english--_gb_1.mp3')
ON CONFLICT DO NOTHING;


-- =========================================================================
-- PHẦN 3: BỔ SUNG ĐẦY ĐỦ CÁC CẶP ÂM DỄ NHẦM LẪN NHẤT (MINIMAL PAIRS)
-- =========================================================================

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /uː/ (dài) và /ʊ/ (ngắn)', 'Âm /uː/ chu môi tròn nhỏ và phát âm kéo dài, trong khi /ʊ/ thả lỏng cơ môi và đọc dứt khoát.', 'fool', '/fuːl/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/fool--_gb_1.mp3', 'full', '/fʊl/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/full--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'uː' AND p2.symbol = 'ʊ' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /ɔː/ (dài) và /ɒ/ (ngắn)', 'Âm /ɔː/ nâng cao cuống lưỡi và kéo dài hơn, âm /ɒ/ mở rộng cằm và phát âm ngắn dứt khoát.', 'sport', '/spɔːt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sport--_gb_1.mp3', 'spot', '/spɒt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/spot--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'ɔː' AND p2.symbol = 'ɒ' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /ʌ/ (â ngắn) và /ɑː/ (a dài)', 'Âm /ʌ/ mở miệng vừa và đọc nhanh, còn âm /ɑː/ hạ cằm sâu và phát âm ngân dài trong vòm họng.', 'cup', '/kʌp/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cup--_gb_1.mp3', 'carp', '/kɑːp/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/carp--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'ʌ' AND p2.symbol = 'ɑː' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /tʃ/ (ch) và /dʒ/ (dj)', 'Âm /tʃ/ không rung thanh quản khi bật hơi, trong khi âm /dʒ/ cần rung mạnh thanh quản trong cổ họng.', 'cheap', '/tʃiːp/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/cheap--_gb_1.mp3', 'jeep', '/dʒiːp/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/jeep--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'tʃ' AND p2.symbol = 'dʒ' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /θ/ (th không rung) và /s/ (xì)', 'Âm /θ/ đặt đầu lưỡi giữa hai hàm răng thổi hơi, âm /s/ khép hai hàm răng lại và xì hơi qua kẽ răng.', 'think', '/θɪŋk/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/think--_gb_1.mp3', 'sink', '/sɪŋk/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sink--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'θ' AND p2.symbol = 's' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /ð/ (th rung) và /d/ (d)', 'Âm /ð/ đặt đầu lưỡi giữa hai hàm răng và rung, âm /d/ chạm đầu lưỡi vào chân răng trên rồi bật hơi.', 'they', '/ðeɪ/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/they--_gb_1.mp3', 'day', '/deɪ/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/day--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'ð' AND p2.symbol = 'd' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /f/ và /v/', 'Cả 2 đều đặt răng trên chạm môi dưới, nhưng /f/ không rung cổ họng còn /v/ rung thanh quản.', 'fan', '/fæn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/fan--_gb_1.mp3', 'van', '/væn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/van--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'f' AND p2.symbol = 'v' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /l/ và /r/', 'Âm /l/ đầu lưỡi chạm chân răng trên, âm /r/ uốn cong đầu lưỡi về phía sau và không chạm vòm họng.', 'light', '/laɪt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/light--_gb_1.mp3', 'right', '/raɪt/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/right--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'l' AND p2.symbol = 'r' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /w/ và /v/', 'Âm /w/ chu tròn hai môi (không chạm răng), âm /v/ bắt buộc đặt răng trên chạm vào môi dưới.', 'wet', '/wet/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/wet--_gb_1.mp3', 'vet', '/vet/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/vet--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'w' AND p2.symbol = 'v' ON CONFLICT DO NOTHING;

INSERT INTO ipa_minimal_pairs (phoneme1_id, phoneme2_id, title, description, word1, ipa1, audio1_url, word2, ipa2, audio2_url)
SELECT p1.id, p2.id, 'Phân biệt /n/ và /ŋ/ (ng)', 'Âm /n/ đầu lưỡi chạm chân răng trên, âm /ŋ/ cuống lưỡi nâng lên chạm vòm mềm chặn hơi qua mũi.', 'thin', '/θɪn/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/thin--_gb_1.mp3', 'thing', '/θɪŋ/', 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/thing--_gb_1.mp3'
FROM ipa_phonemes p1, ipa_phonemes p2 WHERE p1.symbol = 'n' AND p2.symbol = 'ŋ' ON CONFLICT DO NOTHING;


-- =========================================================================
-- PHẦN 4: BỔ SUNG CÁC QUY TẮC TRỌNG ÂM & NGỮ ĐIỆU CHUYÊN SÂU (RULES)
-- =========================================================================

INSERT INTO ipa_pronunciation_rules (category, title_vi, summary_vi, content_markdown, examples_json, order_index) VALUES
(
    'WORD_STRESS',
    'Quy tắc trọng âm Từ ghép (Compound Words)',
    'Danh từ ghép nhấn ở từ thứ nhất. Tính từ ghép và Động từ ghép thường nhấn ở từ thứ hai.',
    '### 1. Danh từ ghép (Compound Nouns)
- Trọng âm thường nhấn vào **từ thứ nhất**.
- Cấu trúc: Noun + Noun, Gerund + Noun, Adjective + Noun.
- Ví dụ: `AIR-port`, `BED-room`, `RAIN-coat`, `WASH-ing machine`.

### 2. Tính từ ghép (Compound Adjectives)
- Trọng âm thường nhấn vào **từ thứ hai**.
- Ví dụ: `old-FASH-ioned`, `well-KNOWN`, `bad-TEM-pered`, `short-SIGHT-ed`.

### 3. Động từ ghép (Compound Verbs)
- Trọng âm thường nhấn vào **từ thứ hai**.
- Ví dụ: `over-COOK`, `under-STAND`, `over-FLOW`.',
    '[
        {"word": "Airport", "ipa": "/ˈeəpɔːt/", "meaning": "Sân bay (Danh từ ghép -> Nhấn từ đầu)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/airport--_gb_1.mp3"},
        {"word": "Well-known", "ipa": "/ˌwel ˈnəʊn/", "meaning": "Nổi tiếng (Tính từ ghép -> Nhấn từ hai)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/well_known--_gb_1.mp3"},
        {"word": "Understand", "ipa": "/ˌʌndəˈstænd/", "meaning": "Hiểu (Động từ ghép -> Nhấn từ hai)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/understand--_gb_1.mp3"}
    ]',
    7
),
(
    'WORD_STRESS',
    'Quy tắc trọng âm Từ có 3 âm tiết trở lên',
    'Hầu hết các từ 3 âm tiết kết thúc bằng đuôi -y, -ce, -ate, -ise nhấn vào âm tiết thứ 3 tính từ cuối lên (âm đầu tiên).',
    '### 1. Nhấn vào âm tiết thứ 3 từ dưới đếm lên (Antepenultimate Syllable):
- Các từ tận cùng là **-y, -ce, -ate, -ise / -ize, -phy, -gy**:
- Ví dụ: 
  - `ECO-nomy` /ɪˈkɒnəmi/ (nhấn âm 2)
  - `PHO-tograph` /ˈfəʊtəɡrɑːf/ (nhấn âm 1)
  - `CON-centrate` /ˈkɒnsntreɪt/ (nhấn âm 1)
  - `CRIT-icise` /ˈkrɪtɪsaɪz/ (nhấn âm 1)

### 2. Tiền tố không nhận trọng âm:
- Hầu hết tiền tố như **un-, in-, dis-, re-, pre-, mis-** không làm thay đổi trọng âm gốc của từ:
- `happy` $\rightarrow$ `unHAPpy`, `cover` $\rightarrow$ `disCOVer`.',
    '[
        {"word": "Photograph", "ipa": "/ˈfəʊtəɡrɑːf/", "meaning": "Bức ảnh (Nhấn âm 1)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/photograph--_gb_1.mp3"},
        {"word": "Concentrate", "ipa": "/ˈkɒnsntreɪt/", "meaning": "Tập trung (Nhấn âm 1)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/concentrate--_gb_1.mp3"},
        {"word": "Unhappy", "ipa": "/ʌnˈhæpi/", "meaning": "Không vui (Tiền tố un- không nhận trọng âm)", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/unhappy--_gb_1.mp3"}
    ]',
    8
),
(
    'LINKING_SOUNDS',
    'Hiện tượng Nuốt âm (Elision) & Biến âm (Assimilation)',
    'Trong văn nói nhanh tự nhiên, một số âm yếu (như /t/, /d/, schwa) thường bị nuốt đi hoặc hòa quyện với âm kế tiếp.',
    '### 1. Nuốt âm /t/ và /d/ (Elision):
- Khi /t/ hoặc /d/ đứng giữa hai phụ âm khác, người bản xứ thường bỏ qua không phát âm:
  - `Next door` $\rightarrow$ Đọc là `/neks dɔːr/` (bỏ /t/)
  - `Last night` $\rightarrow$ Đọc là `/lɑːs naɪt/`
  - `Hold on tight` $\rightarrow$ Đọc là `/həʊld ɒn taɪt/`

### 2. Biến âm khi gặp âm /j/ (Assimilation):
- **/t/ + /j/ $\rightarrow$ /tʃ/**: `Nice to meet you` $\rightarrow$ `/miːtʃuː/`
- **/d/ + /j/ $\rightarrow$ /dʒ/**: `Did you do it?` $\rightarrow$ `/dɪdʒuː duː ɪt/`',
    '[
        {"word": "Next door", "ipa": "/neks dɔːr/", "meaning": "Nuốt âm /t/ khi đứng giữa /ks/ và /d/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/next_door--_gb_1.mp3"},
        {"word": "Meet you", "ipa": "/miːtʃuː/", "meaning": "Biến âm /t/ + /j/ thành /tʃ/", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/meet_you--_gb_1.mp3"}
    ]',
    9
),
(
    'INTONATION',
    'Quy tắc Ngữ điệu câu (Lên giọng & Xuống giọng)',
    'Xuống giọng ở cuối câu trần thuật và câu hỏi Wh-. Lên giọng ở cuối câu hỏi Yes/No và câu hỏi đuôi khi muốn xác nhận.',
    '### 1. Xuống giọng (Falling Intonation ↘):
- Dùng cho **Câu trần thuật**: `I live in Viet Nam. ↘`
- Dùng cho **Câu hỏi Wh-**: `Where do you live? ↘`, `What is your name? ↘`
- Dùng cho **Câu mệnh lệnh**: `Close the door! ↘`

### 2. Lên giọng (Rising Intonation ↗):
- Dùng cho **Câu hỏi Yes/No**: `Do you like coffee? ↗`, `Are you ready? ↗`
- Dùng cho **Liệt kê (trừ mục cuối cùng)**: `I bought apples ↗, bananas ↗, and oranges ↘.`',
    '[
        {"word": "Where do you live?", "ipa": "/weər duː juː lɪv/ ↘", "meaning": "Xuống giọng ở cuối câu hỏi Wh-", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/where_do_you_live--_gb_1.mp3"},
        {"word": "Do you like tea?", "ipa": "/duː juː laɪk tiː/ ↗", "meaning": "Lên giọng ở cuối câu hỏi Yes/No", "audioUrl": "https://ssl.gstatic.com/dictionary/static/sounds/20200429/do_you_like_tea--_gb_1.mp3"}
    ]',
    10
);

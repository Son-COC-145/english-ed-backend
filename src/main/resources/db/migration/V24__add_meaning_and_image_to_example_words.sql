-- =========================================================================
-- V24: Bổ sung nghĩa tiếng Việt (meaning_vi) và ảnh minh họa (image_url)
--      cho toàn bộ từ vựng ví dụ trong bảng ipa_example_words
-- =========================================================================

-- 1. Thêm 2 cột mới
ALTER TABLE ipa_example_words
    ADD COLUMN IF NOT EXISTS meaning_vi  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS image_url   VARCHAR(500);

-- =========================================================================
-- 2. Cập nhật nghĩa tiếng Việt cho tất cả từ ví dụ hiện có
--    (Dùng UPDATE theo word vì id không cố định giữa các môi trường)
-- =========================================================================

-- /iː/ — sheep, see (V8), feel, reach, beat (V19)
UPDATE ipa_example_words SET meaning_vi = 'con cừu'      WHERE word = 'sheep';
UPDATE ipa_example_words SET meaning_vi = 'nhìn thấy'    WHERE word = 'see' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'iː');
UPDATE ipa_example_words SET meaning_vi = 'cảm thấy'     WHERE word = 'feel';
UPDATE ipa_example_words SET meaning_vi = 'với tới, đạt được' WHERE word = 'reach';
UPDATE ipa_example_words SET meaning_vi = 'đánh bại'     WHERE word = 'beat';

-- /ɪ/ — ship, sit (V8), fill, rich, bit (V19)
UPDATE ipa_example_words SET meaning_vi = 'con tàu'      WHERE word = 'ship';
UPDATE ipa_example_words SET meaning_vi = 'ngồi'         WHERE word = 'sit';
UPDATE ipa_example_words SET meaning_vi = 'đổ đầy'       WHERE word = 'fill';
UPDATE ipa_example_words SET meaning_vi = 'giàu có'      WHERE word = 'rich';
UPDATE ipa_example_words SET meaning_vi = 'một chút'     WHERE word = 'bit';

-- /e/ — bed, red (V13), pen, men, check (V19)
UPDATE ipa_example_words SET meaning_vi = 'cái giường'   WHERE word = 'bed';
UPDATE ipa_example_words SET meaning_vi = 'màu đỏ'       WHERE word = 'red';
UPDATE ipa_example_words SET meaning_vi = 'cái bút'      WHERE word = 'pen';
UPDATE ipa_example_words SET meaning_vi = 'đàn ông'      WHERE word = 'men';
UPDATE ipa_example_words SET meaning_vi = 'kiểm tra'     WHERE word = 'check';

-- /æ/ — cat, bag (V13), pan, man, apple (V19)
UPDATE ipa_example_words SET meaning_vi = 'con mèo'      WHERE word = 'cat';
UPDATE ipa_example_words SET meaning_vi = 'cái túi'      WHERE word = 'bag';
UPDATE ipa_example_words SET meaning_vi = 'cái chảo'     WHERE word = 'pan';
UPDATE ipa_example_words SET meaning_vi = 'người đàn ông' WHERE word = 'man';
UPDATE ipa_example_words SET meaning_vi = 'quả táo'      WHERE word = 'apple';

-- /ɑː/ — car, heart (V13), father, start, park (V19)
UPDATE ipa_example_words SET meaning_vi = 'xe hơi'       WHERE word = 'car';
UPDATE ipa_example_words SET meaning_vi = 'trái tim'     WHERE word = 'heart';
UPDATE ipa_example_words SET meaning_vi = 'người cha'    WHERE word = 'father';
UPDATE ipa_example_words SET meaning_vi = 'bắt đầu'      WHERE word = 'start';
UPDATE ipa_example_words SET meaning_vi = 'công viên'    WHERE word = 'park';

-- /ɒ/ — hot, dog (V13), stop, box, clock (V19)
UPDATE ipa_example_words SET meaning_vi = 'nóng bức'     WHERE word = 'hot';
UPDATE ipa_example_words SET meaning_vi = 'con chó'      WHERE word = 'dog';
UPDATE ipa_example_words SET meaning_vi = 'dừng lại'     WHERE word = 'stop';
UPDATE ipa_example_words SET meaning_vi = 'cái hộp'      WHERE word = 'box';
UPDATE ipa_example_words SET meaning_vi = 'đồng hồ'      WHERE word = 'clock';

-- /ɔː/ — call, ball (V13), door, sport, talk (V19)
UPDATE ipa_example_words SET meaning_vi = 'gọi điện'     WHERE word = 'call';
UPDATE ipa_example_words SET meaning_vi = 'quả bóng'     WHERE word = 'ball';
UPDATE ipa_example_words SET meaning_vi = 'cái cửa'      WHERE word = 'door';
UPDATE ipa_example_words SET meaning_vi = 'thể thao'     WHERE word = 'sport';
UPDATE ipa_example_words SET meaning_vi = 'nói chuyện'   WHERE word = 'talk';

-- /ʊ/ — book, look (V13), foot, full, push (V19)
UPDATE ipa_example_words SET meaning_vi = 'cuốn sách'    WHERE word = 'book' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ');
UPDATE ipa_example_words SET meaning_vi = 'nhìn'         WHERE word = 'look';
UPDATE ipa_example_words SET meaning_vi = 'bàn chân'     WHERE word = 'foot';
UPDATE ipa_example_words SET meaning_vi = 'đầy'          WHERE word = 'full';
UPDATE ipa_example_words SET meaning_vi = 'đẩy'          WHERE word = 'push';

-- /uː/ — moon, food (V13), blue, shoe, group (V19)
UPDATE ipa_example_words SET meaning_vi = 'mặt trăng'    WHERE word = 'moon';
UPDATE ipa_example_words SET meaning_vi = 'thức ăn'      WHERE word = 'food';
UPDATE ipa_example_words SET meaning_vi = 'màu xanh dương' WHERE word = 'blue';
UPDATE ipa_example_words SET meaning_vi = 'đôi giày'     WHERE word = 'shoe';
UPDATE ipa_example_words SET meaning_vi = 'nhóm'         WHERE word = 'group';

-- /ʌ/ — cup, luck (V13), sun, bus, love (V19)
UPDATE ipa_example_words SET meaning_vi = 'cái cốc'      WHERE word = 'cup';
UPDATE ipa_example_words SET meaning_vi = 'may mắn'      WHERE word = 'luck';
UPDATE ipa_example_words SET meaning_vi = 'mặt trời'     WHERE word = 'sun';
UPDATE ipa_example_words SET meaning_vi = 'xe buýt'      WHERE word = 'bus';
UPDATE ipa_example_words SET meaning_vi = 'tình yêu'     WHERE word = 'love';

-- /ɜː/ — bird, world (V13), learn, nurse, early (V19)
UPDATE ipa_example_words SET meaning_vi = 'con chim'     WHERE word = 'bird';
UPDATE ipa_example_words SET meaning_vi = 'thế giới'     WHERE word = 'world';
UPDATE ipa_example_words SET meaning_vi = 'học hỏi'      WHERE word = 'learn';
UPDATE ipa_example_words SET meaning_vi = 'y tá'         WHERE word = 'nurse';
UPDATE ipa_example_words SET meaning_vi = 'sớm'          WHERE word = 'early';

-- /ə/ — about, sofa (V13), banana, police, famous (V19)
UPDATE ipa_example_words SET meaning_vi = 'về, khoảng'   WHERE word = 'about';
UPDATE ipa_example_words SET meaning_vi = 'ghế sô pha'   WHERE word = 'sofa';
UPDATE ipa_example_words SET meaning_vi = 'quả chuối'    WHERE word = 'banana';
UPDATE ipa_example_words SET meaning_vi = 'cảnh sát'     WHERE word = 'police';
UPDATE ipa_example_words SET meaning_vi = 'nổi tiếng'    WHERE word = 'famous';

-- /eɪ/ — day, say (V13), make, rain, cake (V19)
UPDATE ipa_example_words SET meaning_vi = 'ngày'         WHERE word = 'day';
UPDATE ipa_example_words SET meaning_vi = 'nói'          WHERE word = 'say';
UPDATE ipa_example_words SET meaning_vi = 'làm, tạo ra'  WHERE word = 'make';
UPDATE ipa_example_words SET meaning_vi = 'mưa'          WHERE word = 'rain';
UPDATE ipa_example_words SET meaning_vi = 'bánh kem'     WHERE word = 'cake';

-- /aɪ/ — fly, night (V13), time, drive, like (V19)
UPDATE ipa_example_words SET meaning_vi = 'bay'          WHERE word = 'fly';
UPDATE ipa_example_words SET meaning_vi = 'ban đêm'      WHERE word = 'night';
UPDATE ipa_example_words SET meaning_vi = 'thời gian'    WHERE word = 'time';
UPDATE ipa_example_words SET meaning_vi = 'lái xe'       WHERE word = 'drive';
UPDATE ipa_example_words SET meaning_vi = 'thích'        WHERE word = 'like';

-- /ɔɪ/ — boy, coin (V13), voice, join, enjoy (V19)
UPDATE ipa_example_words SET meaning_vi = 'cậu bé'       WHERE word = 'boy';
UPDATE ipa_example_words SET meaning_vi = 'đồng xu'      WHERE word = 'coin';
UPDATE ipa_example_words SET meaning_vi = 'giọng nói'    WHERE word = 'voice';
UPDATE ipa_example_words SET meaning_vi = 'tham gia'     WHERE word = 'join';
UPDATE ipa_example_words SET meaning_vi = 'thưởng thức'  WHERE word = 'enjoy';

-- /aʊ/ — now, house (V13)
UPDATE ipa_example_words SET meaning_vi = 'bây giờ'      WHERE word = 'now';
UPDATE ipa_example_words SET meaning_vi = 'ngôi nhà'     WHERE word = 'house';

-- /əʊ/ — go, home (V13)
UPDATE ipa_example_words SET meaning_vi = 'đi'           WHERE word = 'go';
UPDATE ipa_example_words SET meaning_vi = 'nhà, nhà riêng' WHERE word = 'home';

-- /ɪə/ — ear, here (V13)
UPDATE ipa_example_words SET meaning_vi = 'cái tai'      WHERE word = 'ear';
UPDATE ipa_example_words SET meaning_vi = 'ở đây'        WHERE word = 'here';

-- /eə/ — air, where (V13)
UPDATE ipa_example_words SET meaning_vi = 'không khí'    WHERE word = 'air';
UPDATE ipa_example_words SET meaning_vi = 'ở đâu'        WHERE word = 'where';

-- /ʊə/ — tour, pure (V13)
UPDATE ipa_example_words SET meaning_vi = 'chuyến du lịch' WHERE word = 'tour';
UPDATE ipa_example_words SET meaning_vi = 'thuần khiết'  WHERE word = 'pure';

-- /p/ — pen, copy (V8)
UPDATE ipa_example_words SET meaning_vi = 'cái bút'      WHERE word = 'pen' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'p');
UPDATE ipa_example_words SET meaning_vi = 'sao chép'     WHERE word = 'copy';

-- /b/ — book, back (V8)
UPDATE ipa_example_words SET meaning_vi = 'cuốn sách'    WHERE word = 'book' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'b');
UPDATE ipa_example_words SET meaning_vi = 'phía sau'     WHERE word = 'back';

-- /t/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'đầu, trên'    WHERE word = 'top';

-- /d/ (V13)
-- dog đã update ở /ɒ/ rồi, update riêng cho phoneme /d/
UPDATE ipa_example_words SET meaning_vi = 'màu đỏ'       WHERE word = 'red' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'd');

-- /k/ (V13)
-- cat đã update ở /æ/, back đã có, thêm riêng:

-- /tʃ/ — chair, teach, nature (V19)
UPDATE ipa_example_words SET meaning_vi = 'cái ghế'      WHERE word = 'chair';
UPDATE ipa_example_words SET meaning_vi = 'dạy học'      WHERE word = 'teach';
UPDATE ipa_example_words SET meaning_vi = 'thiên nhiên'  WHERE word = 'nature';

-- /dʒ/ — job, page, bridge (V19)
UPDATE ipa_example_words SET meaning_vi = 'công việc'    WHERE word = 'job';
UPDATE ipa_example_words SET meaning_vi = 'trang (sách)' WHERE word = 'page';
UPDATE ipa_example_words SET meaning_vi = 'cây cầu'      WHERE word = 'bridge';

-- /f/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'số năm'       WHERE word = 'five';
UPDATE ipa_example_words SET meaning_vi = 'chiếc lá'     WHERE word = 'leaf';

-- /v/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'xe tải nhỏ'   WHERE word = 'van';
UPDATE ipa_example_words SET meaning_vi = 'sống'         WHERE word = 'live';

-- /θ/ — think, thank, mouth, teeth (V19)
UPDATE ipa_example_words SET meaning_vi = 'nghĩ'         WHERE word = 'think';
UPDATE ipa_example_words SET meaning_vi = 'cảm ơn'       WHERE word = 'thank';
UPDATE ipa_example_words SET meaning_vi = 'cái miệng'    WHERE word = 'mouth';
UPDATE ipa_example_words SET meaning_vi = 'hàm răng'     WHERE word = 'teeth';

-- /ð/ — this, that, mother, breathe (V19)
UPDATE ipa_example_words SET meaning_vi = 'cái này'      WHERE word = 'this';
UPDATE ipa_example_words SET meaning_vi = 'cái đó'       WHERE word = 'that';
UPDATE ipa_example_words SET meaning_vi = 'người mẹ'     WHERE word = 'mother';
UPDATE ipa_example_words SET meaning_vi = 'thở'          WHERE word = 'breathe';

-- /s/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'mặt trời'     WHERE word = 'sun' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 's');
UPDATE ipa_example_words SET meaning_vi = 'xe buýt'      WHERE word = 'bus' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 's');

-- /z/ — zero, music, always (V19)
UPDATE ipa_example_words SET meaning_vi = 'số không'     WHERE word = 'zero';
UPDATE ipa_example_words SET meaning_vi = 'âm nhạc'      WHERE word = 'music';
UPDATE ipa_example_words SET meaning_vi = 'luôn luôn'    WHERE word = 'always';

-- /ʃ/ — shoe, fish (V8), shop, wash, special (V19)
UPDATE ipa_example_words SET meaning_vi = 'đôi giày'     WHERE word = 'shoe' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'ʃ');
UPDATE ipa_example_words SET meaning_vi = 'con cá'       WHERE word = 'fish';
UPDATE ipa_example_words SET meaning_vi = 'cửa hàng'     WHERE word = 'shop';
UPDATE ipa_example_words SET meaning_vi = 'giặt, rửa'    WHERE word = 'wash';
UPDATE ipa_example_words SET meaning_vi = 'đặc biệt'     WHERE word = 'special';

-- /ʒ/ — vision, measure, casual (V19)
UPDATE ipa_example_words SET meaning_vi = 'tầm nhìn'     WHERE word = 'vision';
UPDATE ipa_example_words SET meaning_vi = 'đo lường'     WHERE word = 'measure';
UPDATE ipa_example_words SET meaning_vi = 'thông thường, thoải mái' WHERE word = 'casual';

-- /h/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'cái mũ'       WHERE word = 'hat';
UPDATE ipa_example_words SET meaning_vi = 'phía trước, tiến lên' WHERE word = 'ahead';

-- /m/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'người đàn ông' WHERE word = 'man' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'm');
UPDATE ipa_example_words SET meaning_vi = 'ngôi nhà'     WHERE word = 'home' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'm');

-- /n/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'tên'          WHERE word = 'name';
UPDATE ipa_example_words SET meaning_vi = 'mặt trời'     WHERE word = 'sun' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'n');

-- /ŋ/ — song, ring, english (V19)
UPDATE ipa_example_words SET meaning_vi = 'bài hát'      WHERE word = 'song';
UPDATE ipa_example_words SET meaning_vi = 'chiếc nhẫn'   WHERE word = 'ring';
UPDATE ipa_example_words SET meaning_vi = 'tiếng Anh'    WHERE word = 'english';

-- /l/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'cái chân'     WHERE word = 'leg';
UPDATE ipa_example_words SET meaning_vi = 'tất cả'       WHERE word = 'all';

-- /r/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'màu đỏ'       WHERE word = 'red' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'r');
UPDATE ipa_example_words SET meaning_vi = 'rất, rất nhiều' WHERE word = 'very';

-- /w/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'chúng tôi'    WHERE word = 'we';
UPDATE ipa_example_words SET meaning_vi = 'xa, đi xa'    WHERE word = 'away';

-- /j/ (V13)
UPDATE ipa_example_words SET meaning_vi = 'vâng, có'     WHERE word = 'yes';
UPDATE ipa_example_words SET meaning_vi = 'bạn, anh/chị' WHERE word = 'you';

-- =========================================================================
-- 3. Cập nhật image_url dùng Unsplash (miễn phí, không cần key)
--    Format: https://source.unsplash.com/200x200/?{keyword}
--    Ghi chú: Có thể thay bằng URL ảnh thật sau khi upload lên Azure Blob
-- =========================================================================

UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?sheep'        WHERE word = 'sheep';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?ship'         WHERE word = 'ship';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?bed'          WHERE word = 'bed';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?cat'          WHERE word = 'cat';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?car'          WHERE word = 'car';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?hot'          WHERE word = 'hot';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?door'         WHERE word = 'door';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?book'         WHERE word = 'book' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ');
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?moon'         WHERE word = 'moon';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?sun'          WHERE word = 'sun';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?bird'         WHERE word = 'bird';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?banana'       WHERE word = 'banana';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?cake'         WHERE word = 'cake';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?fly'          WHERE word = 'fly';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?coin'         WHERE word = 'coin';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?house'        WHERE word = 'house';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?house'        WHERE word = 'home';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?ear'          WHERE word = 'ear';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?air,sky'      WHERE word = 'air';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?travel,tour'  WHERE word = 'tour';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?pen'          WHERE word = 'pen';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?book'         WHERE word = 'book' AND phoneme_id = (SELECT id FROM ipa_phonemes WHERE symbol = 'b');
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?chair'        WHERE word = 'chair';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?job,work'     WHERE word = 'job';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?bridge'       WHERE word = 'bridge';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?leaf,tree'    WHERE word = 'leaf';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?van,truck'    WHERE word = 'van';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?teeth,smile'  WHERE word = 'teeth';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?mother,family' WHERE word = 'mother';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?music'        WHERE word = 'music';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?shop,store'   WHERE word = 'shop';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?eyesight,eye' WHERE word = 'vision';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?hat'          WHERE word = 'hat';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?ring,jewelry' WHERE word = 'ring';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?leg,run'      WHERE word = 'leg';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?apple'        WHERE word = 'apple';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?clock'        WHERE word = 'clock';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?football,ball' WHERE word = 'ball';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?foot'         WHERE word = 'foot';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?rain'         WHERE word = 'rain';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?night,dark'   WHERE word = 'night';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?boy,child'    WHERE word = 'boy';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?dog'          WHERE word = 'dog';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?nurse,doctor' WHERE word = 'nurse';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?police'       WHERE word = 'police';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?love,heart'   WHERE word = 'love';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?bus'          WHERE word = 'bus';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?shoe,footwear' WHERE word = 'shoe';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?fish'         WHERE word = 'fish';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?nature,forest' WHERE word = 'nature';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?park,garden'  WHERE word = 'park';
UPDATE ipa_example_words SET image_url = 'https://source.unsplash.com/200x200/?sport,running' WHERE word = 'sport';

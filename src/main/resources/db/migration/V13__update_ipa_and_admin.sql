-- V13__update_ipa_and_admin.sql
-- 1. Cập nhật tài khoản admin: đổi email thành admin@gmail.com và cập nhật password
-- 2. Bổ sung đầy đủ 44 âm IPA (hiện tại V8 chỉ seed 5 âm)

-- ======================================================
-- PHẦN 1: Cập nhật tài khoản Admin
-- ======================================================

-- Đảm bảo có tài khoản admin@gmail.com với role ADMIN và password đúng
INSERT INTO users (email, password_hash, full_name, role, provider, locale, is_active, created_at, updated_at)
VALUES ('admin@gmail.com', '$2a$10$btDK0h5e0fJAAghAVRkgGOtqL/rkGh8laXG0N6GIdjdWV.WlUqrPq', 'System Admin', 'ADMIN', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO UPDATE 
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    updated_at = CURRENT_TIMESTAMP;

-- ======================================================
-- PHẦN 2: Bổ sung 39 âm IPA còn thiếu (V8 đã có 5 âm: iː, ɪ, p, b, ʃ)
-- ======================================================

INSERT INTO ipa_phonemes (symbol, phoneme_type, name_vi, audio_male_url, audio_female_url, cefr_intro_level, is_common_vn_error)
VALUES
  -- VOWEL_MONO (còn thiếu 10, đã có iː và ɪ)
  ('e',   'VOWEL_MONO', 'Âm e ngắn (bed, red)',         'https://cdn.example.com/phonemes/e_male.mp3',    'https://cdn.example.com/phonemes/e_female.mp3',    'A1', true),
  ('æ',   'VOWEL_MONO', 'Âm a mở (cat, bag)',           'https://cdn.example.com/phonemes/ae_male.mp3',   'https://cdn.example.com/phonemes/ae_female.mp3',   'A2', true),
  ('ɑː',  'VOWEL_MONO', 'Âm a dài (car, heart)',        'https://cdn.example.com/phonemes/aa_male.mp3',   'https://cdn.example.com/phonemes/aa_female.mp3',   'A1', false),
  ('ɒ',   'VOWEL_MONO', 'Âm o ngắn (hot, dog)',         'https://cdn.example.com/phonemes/o_male.mp3',    'https://cdn.example.com/phonemes/o_female.mp3',    'A1', false),
  ('ɔː',  'VOWEL_MONO', 'Âm o dài (call, ball)',        'https://cdn.example.com/phonemes/oo_male.mp3',   'https://cdn.example.com/phonemes/oo_female.mp3',   'A2', true),
  ('ʊ',   'VOWEL_MONO', 'Âm u ngắn (book, look)',       'https://cdn.example.com/phonemes/u_male.mp3',    'https://cdn.example.com/phonemes/u_female.mp3',    'A2', true),
  ('uː',  'VOWEL_MONO', 'Âm u dài (moon, food)',        'https://cdn.example.com/phonemes/uu_male.mp3',   'https://cdn.example.com/phonemes/uu_female.mp3',   'A1', false),
  ('ʌ',   'VOWEL_MONO', 'Âm â ngắn (cup, luck)',        'https://cdn.example.com/phonemes/a_male.mp3',    'https://cdn.example.com/phonemes/a_female.mp3',    'A2', true),
  ('ɜː',  'VOWEL_MONO', 'Âm ơ dài (bird, world)',       'https://cdn.example.com/phonemes/er_male.mp3',   'https://cdn.example.com/phonemes/er_female.mp3',   'B1', true),
  ('ə',   'VOWEL_MONO', 'Âm schwa (about, sofa)',       'https://cdn.example.com/phonemes/schwa_male.mp3','https://cdn.example.com/phonemes/schwa_female.mp3', 'B1', true),

  -- VOWEL_DIPH (8 âm đôi — tất cả đều mới)
  ('eɪ',  'VOWEL_DIPH', 'Âm đôi ei (day, say)',         'https://cdn.example.com/phonemes/ei_male.mp3',   'https://cdn.example.com/phonemes/ei_female.mp3',   'A2', false),
  ('aɪ',  'VOWEL_DIPH', 'Âm đôi ai (fly, my)',          'https://cdn.example.com/phonemes/ai_male.mp3',   'https://cdn.example.com/phonemes/ai_female.mp3',   'A1', false),
  ('ɔɪ',  'VOWEL_DIPH', 'Âm đôi oi (boy, coin)',        'https://cdn.example.com/phonemes/oi_male.mp3',   'https://cdn.example.com/phonemes/oi_female.mp3',   'A2', false),
  ('aʊ',  'VOWEL_DIPH', 'Âm đôi ao (now, house)',       'https://cdn.example.com/phonemes/au_male.mp3',   'https://cdn.example.com/phonemes/au_female.mp3',   'A2', false),
  ('əʊ',  'VOWEL_DIPH', 'Âm đôi ou (go, home)',         'https://cdn.example.com/phonemes/ou_male.mp3',   'https://cdn.example.com/phonemes/ou_female.mp3',   'A2', true),
  ('ɪə',  'VOWEL_DIPH', 'Âm đôi ia (ear, here)',        'https://cdn.example.com/phonemes/ia_male.mp3',   'https://cdn.example.com/phonemes/ia_female.mp3',   'B1', true),
  ('eə',  'VOWEL_DIPH', 'Âm đôi ea (air, where)',       'https://cdn.example.com/phonemes/ea_male.mp3',   'https://cdn.example.com/phonemes/ea_female.mp3',   'B1', true),
  ('ʊə',  'VOWEL_DIPH', 'Âm đôi ua (tour, pure)',       'https://cdn.example.com/phonemes/ua_male.mp3',   'https://cdn.example.com/phonemes/ua_female.mp3',   'B2', true),

  -- CONSONANT (còn thiếu 21, đã có p, b, ʃ)
  ('t',   'CONSONANT', 'Âm t (top, sit)',                'https://cdn.example.com/phonemes/t_male.mp3',    'https://cdn.example.com/phonemes/t_female.mp3',    'A1', false),
  ('d',   'CONSONANT', 'Âm d (dog, red)',                'https://cdn.example.com/phonemes/d_male.mp3',    'https://cdn.example.com/phonemes/d_female.mp3',    'A1', false),
  ('k',   'CONSONANT', 'Âm k (cat, back)',               'https://cdn.example.com/phonemes/k_male.mp3',    'https://cdn.example.com/phonemes/k_female.mp3',    'A1', false),
  ('g',   'CONSONANT', 'Âm g (get, big)',                'https://cdn.example.com/phonemes/g_male.mp3',    'https://cdn.example.com/phonemes/g_female.mp3',    'A1', false),
  ('tʃ',  'CONSONANT', 'Âm ch (chair, match)',           'https://cdn.example.com/phonemes/ch_male.mp3',   'https://cdn.example.com/phonemes/ch_female.mp3',   'A2', false),
  ('dʒ',  'CONSONANT', 'Âm dj (judge, age)',             'https://cdn.example.com/phonemes/dj_male.mp3',   'https://cdn.example.com/phonemes/dj_female.mp3',   'A2', false),
  ('f',   'CONSONANT', 'Âm f (five, leaf)',              'https://cdn.example.com/phonemes/f_male.mp3',    'https://cdn.example.com/phonemes/f_female.mp3',    'A1', false),
  ('v',   'CONSONANT', 'Âm v (van, live)',               'https://cdn.example.com/phonemes/v_male.mp3',    'https://cdn.example.com/phonemes/v_female.mp3',    'A1', true),
  ('θ',   'CONSONANT', 'Âm th không rung (think, bath)', 'https://cdn.example.com/phonemes/th_male.mp3',   'https://cdn.example.com/phonemes/th_female.mp3',   'A2', true),
  ('ð',   'CONSONANT', 'Âm th có rung (this, mother)',  'https://cdn.example.com/phonemes/dh_male.mp3',   'https://cdn.example.com/phonemes/dh_female.mp3',   'A2', true),
  ('s',   'CONSONANT', 'Âm s (sun, bus)',                'https://cdn.example.com/phonemes/s_male.mp3',    'https://cdn.example.com/phonemes/s_female.mp3',    'A1', false),
  ('z',   'CONSONANT', 'Âm z (zoo, days)',               'https://cdn.example.com/phonemes/z_male.mp3',    'https://cdn.example.com/phonemes/z_female.mp3',    'A2', true),
  ('ʒ',   'CONSONANT', 'Âm zh (measure, vision)',        'https://cdn.example.com/phonemes/zh_male.mp3',   'https://cdn.example.com/phonemes/zh_female.mp3',   'B1', true),
  ('h',   'CONSONANT', 'Âm h (hat, ahead)',              'https://cdn.example.com/phonemes/h_male.mp3',    'https://cdn.example.com/phonemes/h_female.mp3',    'A1', false),
  ('m',   'CONSONANT', 'Âm m (man, home)',               'https://cdn.example.com/phonemes/m_male.mp3',    'https://cdn.example.com/phonemes/m_female.mp3',    'A1', false),
  ('n',   'CONSONANT', 'Âm n (name, sun)',               'https://cdn.example.com/phonemes/n_male.mp3',    'https://cdn.example.com/phonemes/n_female.mp3',    'A1', false),
  ('ŋ',   'CONSONANT', 'Âm ng (sing, long)',             'https://cdn.example.com/phonemes/ng_male.mp3',   'https://cdn.example.com/phonemes/ng_female.mp3',   'A2', true),
  ('l',   'CONSONANT', 'Âm l (leg, all)',                'https://cdn.example.com/phonemes/l_male.mp3',    'https://cdn.example.com/phonemes/l_female.mp3',    'A1', false),
  ('r',   'CONSONANT', 'Âm r (red, very)',               'https://cdn.example.com/phonemes/r_male.mp3',    'https://cdn.example.com/phonemes/r_female.mp3',    'A2', true),
  ('w',   'CONSONANT', 'Âm w (we, away)',                'https://cdn.example.com/phonemes/w_male.mp3',    'https://cdn.example.com/phonemes/w_female.mp3',    'A1', false),
  ('j',   'CONSONANT', 'Âm y (yes, you)',                'https://cdn.example.com/phonemes/j_male.mp3',    'https://cdn.example.com/phonemes/j_female.mp3',    'A1', false);

-- ======================================================
-- Từ ví dụ cho 39 âm mới thêm (mỗi âm 2 từ)
-- ======================================================
INSERT INTO ipa_example_words (phoneme_id, word, ipa_transcription, audio_url) VALUES
  -- VOWEL_MONO mới
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'e'),  'bed',     '/bed/',       'https://cdn.example.com/words/bed.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'e'),  'red',     '/red/',       'https://cdn.example.com/words/red.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'æ'),  'cat',     '/kæt/',       'https://cdn.example.com/words/cat.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'æ'),  'bag',     '/bæɡ/',       'https://cdn.example.com/words/bag.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɑː'), 'car',     '/kɑːr/',      'https://cdn.example.com/words/car.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɑː'), 'heart',   '/hɑːrt/',     'https://cdn.example.com/words/heart.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɒ'),  'hot',     '/hɒt/',       'https://cdn.example.com/words/hot.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɒ'),  'dog',     '/dɒɡ/',       'https://cdn.example.com/words/dog.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔː'), 'call',    '/kɔːl/',      'https://cdn.example.com/words/call.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔː'), 'ball',    '/bɔːl/',      'https://cdn.example.com/words/ball.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ'),  'book',    '/bʊk/',       'https://cdn.example.com/words/book.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊ'),  'look',    '/lʊk/',       'https://cdn.example.com/words/look.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'uː'), 'moon',    '/muːn/',      'https://cdn.example.com/words/moon.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'uː'), 'food',    '/fuːd/',      'https://cdn.example.com/words/food.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʌ'),  'cup',     '/kʌp/',       'https://cdn.example.com/words/cup.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʌ'),  'luck',    '/lʌk/',       'https://cdn.example.com/words/luck.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɜː'), 'bird',    '/bɜːrd/',     'https://cdn.example.com/words/bird.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɜː'), 'world',   '/wɜːrld/',    'https://cdn.example.com/words/world.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ə'),  'about',   '/əˈbaʊt/',    'https://cdn.example.com/words/about.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ə'),  'sofa',    '/ˈsoʊfə/',    'https://cdn.example.com/words/sofa.mp3'),
  -- VOWEL_DIPH mới
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'eɪ'), 'day',     '/deɪ/',       'https://cdn.example.com/words/day.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'eɪ'), 'say',     '/seɪ/',       'https://cdn.example.com/words/say.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'aɪ'), 'fly',     '/flaɪ/',      'https://cdn.example.com/words/fly.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'aɪ'), 'night',   '/naɪt/',      'https://cdn.example.com/words/night.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔɪ'), 'boy',     '/bɔɪ/',       'https://cdn.example.com/words/boy.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɔɪ'), 'coin',    '/kɔɪn/',      'https://cdn.example.com/words/coin.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'aʊ'), 'now',     '/naʊ/',       'https://cdn.example.com/words/now.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'aʊ'), 'house',   '/haʊs/',      'https://cdn.example.com/words/house.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'əʊ'), 'go',      '/ɡəʊ/',       'https://cdn.example.com/words/go.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'əʊ'), 'home',    '/həʊm/',      'https://cdn.example.com/words/home.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪə'), 'ear',     '/ɪər/',       'https://cdn.example.com/words/ear.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ɪə'), 'here',    '/hɪər/',      'https://cdn.example.com/words/here.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'eə'), 'air',     '/eər/',       'https://cdn.example.com/words/air.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'eə'), 'where',   '/weər/',      'https://cdn.example.com/words/where.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊə'), 'tour',    '/tʊər/',      'https://cdn.example.com/words/tour.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʊə'), 'pure',    '/pjʊər/',     'https://cdn.example.com/words/pure.mp3'),
  -- CONSONANT mới
  ((SELECT id FROM ipa_phonemes WHERE symbol = 't'),  'top',     '/tɒp/',       'https://cdn.example.com/words/top.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 't'),  'sit',     '/sɪt/',       'https://cdn.example.com/words/sit2.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'd'),  'dog',     '/dɒɡ/',       'https://cdn.example.com/words/dog2.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'd'),  'red',     '/red/',       'https://cdn.example.com/words/red2.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'k'),  'cat',     '/kæt/',       'https://cdn.example.com/words/cat2.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'k'),  'back',    '/bæk/',       'https://cdn.example.com/words/back.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'g'),  'get',     '/ɡet/',       'https://cdn.example.com/words/get.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'g'),  'big',     '/bɪɡ/',       'https://cdn.example.com/words/big.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'tʃ'), 'chair',   '/tʃeər/',     'https://cdn.example.com/words/chair.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'tʃ'), 'match',   '/mætʃ/',      'https://cdn.example.com/words/match.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'dʒ'), 'judge',   '/dʒʌdʒ/',     'https://cdn.example.com/words/judge.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'dʒ'), 'age',     '/eɪdʒ/',      'https://cdn.example.com/words/age.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'f'),  'five',    '/faɪv/',      'https://cdn.example.com/words/five.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'f'),  'leaf',    '/liːf/',      'https://cdn.example.com/words/leaf.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'v'),  'van',     '/væn/',       'https://cdn.example.com/words/van.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'v'),  'live',    '/lɪv/',       'https://cdn.example.com/words/live.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'θ'),  'think',   '/θɪŋk/',      'https://cdn.example.com/words/think.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'θ'),  'bath',    '/bɑːθ/',      'https://cdn.example.com/words/bath.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'),  'this',    '/ðɪs/',       'https://cdn.example.com/words/this.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ð'),  'mother',  '/ˈmʌðər/',    'https://cdn.example.com/words/mother.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 's'),  'sun',     '/sʌn/',       'https://cdn.example.com/words/sun.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 's'),  'bus',     '/bʌs/',       'https://cdn.example.com/words/bus.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'z'),  'zoo',     '/zuː/',       'https://cdn.example.com/words/zoo.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'z'),  'days',    '/deɪz/',      'https://cdn.example.com/words/days.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'),  'measure', '/ˈmeʒər/',    'https://cdn.example.com/words/measure.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ʒ'),  'vision',  '/ˈvɪʒən/',    'https://cdn.example.com/words/vision.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'h'),  'hat',     '/hæt/',       'https://cdn.example.com/words/hat.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'h'),  'ahead',   '/əˈhed/',     'https://cdn.example.com/words/ahead.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'm'),  'man',     '/mæn/',       'https://cdn.example.com/words/man.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'm'),  'home',    '/həʊm/',      'https://cdn.example.com/words/home2.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'n'),  'name',    '/neɪm/',      'https://cdn.example.com/words/name.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'n'),  'sun',     '/sʌn/',       'https://cdn.example.com/words/sun2.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ŋ'),  'sing',    '/sɪŋ/',       'https://cdn.example.com/words/sing.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'ŋ'),  'long',    '/lɒŋ/',       'https://cdn.example.com/words/long.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'l'),  'leg',     '/leɡ/',       'https://cdn.example.com/words/leg.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'l'),  'all',     '/ɔːl/',       'https://cdn.example.com/words/all.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'r'),  'red',     '/red/',       'https://cdn.example.com/words/red3.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'r'),  'very',    '/ˈveri/',     'https://cdn.example.com/words/very.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'w'),  'we',      '/wiː/',       'https://cdn.example.com/words/we.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'w'),  'away',    '/əˈweɪ/',     'https://cdn.example.com/words/away.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'j'),  'yes',     '/jes/',       'https://cdn.example.com/words/yes.mp3'),
  ((SELECT id FROM ipa_phonemes WHERE symbol = 'j'),  'you',     '/juː/',       'https://cdn.example.com/words/you.mp3');

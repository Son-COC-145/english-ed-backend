-- ==============================================================================
-- V32__seed_system_data.sql
-- Thêm dữ liệu mẫu toàn diện cho hệ thống:
-- 1. Users (Admin, Teachers, Students)
-- 2. Topics (Chủ đề từ vựng phong phú)
-- 3. Vocabulary (Bộ từ vựng chuẩn kèm JSON Typed: Ví dụ, Collocation, Hội thoại)
-- 4. Student Stats & Gamification (XP, Streak, Lịch sử Minigame)
-- 5. Student Vocabulary Progress & SRS Due Review (Từ mới, đang học, đến hạn ôn)
-- 6. Courses / Classes & Enrollments (Khóa học, Giảng viên, Học viên)
-- 7. Syllabus & Materials (Giáo trình theo tuần, Tài liệu học tập)
-- 8. Assignments & Submissions (Bài tập & Chấm điểm)
-- 9. Speaking Scenarios (Kịch bản luyện nói AI - Module 3)
-- 10. Notifications
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. USERS: Thêm / cập nhật tài khoản chuẩn hệ thống
-- Password:
--   admin@gmail.com   -> Admin123456@  ($2a$10$btDK0h5e0fJAAghAVRkgGOtqL/rkGh8laXG0N6GIdjdWV.WlUqrPq)
--   teacher & student -> Password@123  ($2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.)
-- ------------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, role, provider, locale, is_active, created_at, updated_at)
VALUES 
  ('admin@gmail.com',         '$2a$10$btDK0h5e0fJAAghAVRkgGOtqL/rkGh8laXG0N6GIdjdWV.WlUqrPq', 'System Admin',     'ADMIN',   'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('teacher@example.com',     '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Thầy John Smith',  'TEACHER', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('sarah.teacher@gmail.com', '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Cô Sarah Jenkins', 'TEACHER', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('student@example.com',     '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Nguyễn Văn An',    'STUDENT', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('student2@gmail.com',      '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Trần Thị Bình',    'STUDENT', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('student3@gmail.com',      '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Lê Hoàng Cường',   'STUDENT', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO UPDATE 
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    full_name = EXCLUDED.full_name,
    is_active = true,
    updated_at = CURRENT_TIMESTAMP;

-- ------------------------------------------------------------------------------
-- 2. TOPICS: Các chủ đề từ vựng theo khung CEFR và Danh mục
-- ------------------------------------------------------------------------------
INSERT INTO topics (id, name_en, name_vi, iconUrl, is_active, cefr_level, category)
VALUES
  (1,  'Daily Life & Routines',      'Cuộc sống thường ngày',        'https://images.unsplash.com/photo-1506784983877-45594efa4cbe', true, 'A1', 'DAILY_CONVERSATION'),
  (2,  'Family & Relationships',     'Gia đình & Mối quan hệ',       'https://images.unsplash.com/photo-1511895426328-dc8714191300', true, 'A1', 'DAILY_CONVERSATION'),
  (3,  'Food & Dining',              'Ẩm thực & Nhà hàng',           'https://images.unsplash.com/photo-1555396273-367ea4eb4db5', true, 'A2', 'DAILY_CONVERSATION'),
  (4,  'Travel & Exploration',       'Du lịch & Khám phá',           'https://images.unsplash.com/photo-1488646953014-85cb44e25828', true, 'A2', 'TRAVEL'),
  (5,  'Hotels & Accommodation',     'Khách sạn & Nơi lưu trú',      'https://images.unsplash.com/photo-1566073771259-6a8506099945', true, 'A2', 'TRAVEL'),
  (6,  'Office & Workplace',         'Văn phòng & Nơi làm việc',     'https://images.unsplash.com/photo-1497366216548-37526070297c', true, 'B1', 'WORK'),
  (7,  'Business Meetings',          'Họp hành & Đàm phán',          'https://images.unsplash.com/photo-1542744173-8e7e53415bb0', true, 'B1', 'WORK'),
  (8,  'Environment & Climate',      'Môi trường & Biến đổi khí hậu','https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05', true, 'B2', 'EXAM_IELTS'),
  (9,  'Technology & AI',            'Công nghệ & Trí tuệ nhân tạo', 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e', true, 'B2', 'EXAM_IELTS'),
  (10, 'Higher Education & Research','Đại học & Nghiên cứu khoa học','https://images.unsplash.com/photo-1523050854058-8df90110c9f1', true, 'C1', 'STUDY_ABROAD')
ON CONFLICT (id) DO UPDATE
SET name_en = EXCLUDED.name_en,
    name_vi = EXCLUDED.name_vi,
    iconUrl = EXCLUDED.iconUrl,
    is_active = EXCLUDED.is_active,
    cefr_level = EXCLUDED.cefr_level,
    category = EXCLUDED.category;

SELECT setval(pg_get_serial_sequence('topics', 'id'), (SELECT MAX(id) FROM topics));

-- ------------------------------------------------------------------------------
-- 3. VOCABULARY: Bộ từ vựng chất lượng cao kèm JSON Typed
-- ------------------------------------------------------------------------------
INSERT INTO vocabulary (
    id, topic_id, word, ipa_transcription, cefr_level, definition_vi,
    image_url, audio_us_url, audio_uk_url, nuance_note,
    example_sentences_json, collocation_json, dialogue_json,
    status, created_by, published_at, created_at
) VALUES
-- Topic 1: Daily Life (A1)
(1, 1, 'Routine', '/ruːˈtiːn/', 'A1', 'Thói quen hằng ngày, nề nếp sinh hoạt',
 'https://images.unsplash.com/photo-1499750310107-5fef28a66643',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/routine--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/routine--_gb_1.mp3',
 'Thường dùng chỉ các chuỗi hành động lặp đi lặp lại vào giờ cố định mỗi ngày.',
 '[{"sentence": "My morning routine includes coffee and reading.", "translationVi": "Thói quen buổi sáng của tôi bao gồm uống cà phê và đọc sách."}]'::jsonb,
 '[{"phrase": "daily routine", "meaningVi": "thói quen hàng ngày"}, {"phrase": "morning routine", "meaningVi": "nề nếp buổi sáng"}]'::jsonb,
 '[{"speaker": "A", "sentence": "Do you have a fixed morning routine?", "translationVi": "Cậu có thói quen buổi sáng cố định không?"}, {"speaker": "B", "sentence": "Yes, I always jog at 6 AM.", "translationVi": "Có, mình luôn chạy bộ lúc 6 giờ sáng."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 1, 'Schedule', '/ˈskedʒ.uːl/', 'A1', 'Lịch trình, thời gian biểu',
 'https://images.unsplash.com/photo-1506784365847-bbad939e9335',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/schedule--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/schedule--_gb_1.mp3',
 'Phát âm Mỹ là /ˈskedʒ.uːl/, phát âm Anh-Anh là /ˈʃedʒ.uːl/.',
 '[{"sentence": "I have a very busy schedule today.", "translationVi": "Hôm nay tôi có một lịch trình rất bận rộn."}]'::jsonb,
 '[{"phrase": "tight schedule", "meaningVi": "lịch trình dày đặc"}, {"phrase": "ahead of schedule", "meaningVi": "sớm hơn dự kiến"}]'::jsonb,
 '[{"speaker": "A", "sentence": "Can we meet tomorrow?", "translationVi": "Ngày mai mình gặp nhau được không?"}, {"speaker": "B", "sentence": "Let me check my schedule first.", "translationVi": "Để mình kiểm tra lại lịch đã nhé."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 1, 'Commute', '/kəˈmjuːt/', 'A2', 'Đi lại hàng ngày giữa nhà và nơi làm việc',
 'https://images.unsplash.com/photo-1517649763962-0c623266ddc0',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/commute--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/commute--_gb_1.mp3',
 'Có thể làm cả danh từ (chuyến đi lại) hoặc động từ (di chuyển đi làm).',
 '[{"sentence": "It takes an hour to commute to work.", "translationVi": "Mất một tiếng đồng hồ để đi từ nhà đến chỗ làm."}]'::jsonb,
 '[{"phrase": "daily commute", "meaningVi": "quãng đường đi làm hàng ngày"}, {"phrase": "morning commute", "meaningVi": "giờ đi làm buổi sáng"}]'::jsonb,
 '[{"speaker": "A", "sentence": "How is your daily commute?", "translationVi": "Quãng đường đi làm của cậu thế nào?"}, {"speaker": "B", "sentence": "It is quite tiring by bus.", "translationVi": "Đi xe buýt khá là mệt mỏi."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 2: Family & Relationships (A1)
(4, 2, 'Sibling', '/ˈsɪb.lɪŋ/', 'A1', 'Anh chị em ruột',
 'https://images.unsplash.com/photo-1471286174890-9c112ffca56a',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sibling--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sibling--_gb_1.mp3',
 'Từ trang trọng bao gồm cả anh, em trai và chị, em gái.',
 '[{"sentence": "Do you have any siblings?", "translationVi": "Bạn có anh chị em nào không?"}]'::jsonb,
 '[{"phrase": "sibling rivalry", "meaningVi": "sự ganh đua giữa anh chị em"}]'::jsonb,
 '[{"speaker": "A", "sentence": "Do you have siblings?", "translationVi": "Cậu có anh chị em gì không?"}, {"speaker": "B", "sentence": "I have two younger sisters.", "translationVi": "Mình có hai cô em gái."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 3: Food & Dining (A2)
(5, 3, 'Appetizer', '/ˈæp.ə.taɪ.zɚ/', 'A2', 'Món khai vị',
 'https://images.unsplash.com/photo-1541529086526-db283c563270',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/appetizer--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/appetizer--_gb_1.mp3',
 'Tiếng Anh-Mỹ là appetizer, Anh-Anh hay dùng starter.',
 '[{"sentence": "We ordered spring rolls as an appetizer.", "translationVi": "Chúng tôi gọi món nem rán làm món khai vị."}]'::jsonb,
 '[{"phrase": "order appetizers", "meaningVi": "gọi món khai vị"}]'::jsonb,
 '[{"speaker": "Waiter", "sentence": "Would you like an appetizer to start?", "translationVi": "Quý khách có muốn dùng món khai vị trước không?"}, {"speaker": "Customer", "sentence": "Yes, garlic bread please.", "translationVi": "Vâng, cho tôi bánh mì bơ tỏi nhé."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(6, 3, 'Beverage', '/ˈbev.ɚ.ɪdʒ/', 'A2', 'Đồ uống, thức uống',
 'https://images.unsplash.com/photo-1551024709-8f23befc6f87',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/beverage--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/beverage--_gb_1.mp3',
 'Từ lịch sự/trang trọng hơn của từ drink, thường thấy trong menu nhà hàng.',
 '[{"sentence": "Hot beverages are served on request.", "translationVi": "Đồ uống nóng được phục vụ theo yêu cầu."}]'::jsonb,
 '[{"phrase": "alcoholic beverages", "meaningVi": "đồ uống có cồn"}, {"phrase": "refreshing beverage", "meaningVi": "thức uống giải khát"}]'::jsonb,
 '[{"speaker": "A", "sentence": "What beverage would you recommend?", "translationVi": "Bạn gợi ý đồ uống gì ngon?"}, {"speaker": "B", "sentence": "Our iced peach tea is the best.", "translationVi": "Trà đào đá của quán là ngon nhất đấy ạ."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 4: Travel & Exploration (A2)
(7, 4, 'Itinerary', '/aɪˈtɪn.ər.er.i/', 'A2', 'Lịch trình, lộ trình chuyến đi',
 'https://images.unsplash.com/photo-1488646953014-85cb44e25828',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/itinerary--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/itinerary--_gb_1.mp3',
 'Bản kế hoạch chi tiết về thời gian và các địa điểm sẽ ghé thăm.',
 '[{"sentence": "We followed a strict itinerary during the tour.", "translationVi": "Chúng tôi tuân thủ một lịch trình nghiêm ngặt suốt chuyến tham quan."}]'::jsonb,
 '[{"phrase": "travel itinerary", "meaningVi": "lịch trình chuyến du lịch"}, {"phrase": "planned itinerary", "meaningVi": "lộ trình đã lên kế hoạch"}]'::jsonb,
 '[{"speaker": "A", "sentence": "Have you finalized our trip itinerary?", "translationVi": "Cậu đã chốt lịch trình chuyến đi chưa?"}, {"speaker": "B", "sentence": "Almost, just need to pick hotel dates.", "translationVi": "Gần xong rồi, chỉ cần chọn ngày ở khách sạn nữa thôi."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(8, 4, 'Luggage', '/ˈlʌɡ.ɪdʒ/', 'A2', 'Hành lý mang theo',
 'https://images.unsplash.com/photo-1581553680321-4fffae59fccd',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/luggage--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/luggage--_gb_1.mp3',
 'Là danh từ không đếm được (uncountable). Tiếng Mỹ hay dùng baggage.',
 '[{"sentence": "Never leave your luggage unattended at the airport.", "translationVi": "Đừng bao giờ để hành lý của bạn không có người trông coi ở sân bay."}]'::jsonb,
 '[{"phrase": "hand luggage", "meaningVi": "hành lý xách tay"}, {"phrase": "lost luggage", "meaningVi": "hành lý thất lạc"}]'::jsonb,
 '[{"speaker": "Officer", "sentence": "Is this your only piece of luggage?", "translationVi": "Đây có phải kiện hành lý duy nhất của quý khách không?"}, {"speaker": "Passenger", "sentence": "Yes, just this carry-on bag.", "translationVi": "Vâng, chỉ có chiếc túi xách tay này thôi."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 5: Hotels & Accommodation (A2)
(9, 5, 'Reservation', '/ˌrez.ɚˈveɪ.ʃən/', 'A2', 'Sự đặt trước (phòng, bàn, vé)',
 'https://images.unsplash.com/photo-1566073771259-6a8506099945',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/reservation--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/reservation--_gb_1.mp3',
 'Thường đi với động từ make: make a reservation.',
 '[{"sentence": "I would like to make a reservation for two nights.", "translationVi": "Tôi muốn đặt phòng cho hai đêm."}]'::jsonb,
 '[{"phrase": "make a reservation", "meaningVi": "đặt chỗ trước"}, {"phrase": "confirm reservation", "meaningVi": "xác nhận đặt chỗ"}]'::jsonb,
 '[{"speaker": "Receptionist", "sentence": "Good evening, do you have a reservation?", "translationVi": "Chào buổi tối, quý khách đã đặt phòng trước chưa ạ?"}, {"speaker": "Guest", "sentence": "Yes, under the name of Nguyen.", "translationVi": "Có, dưới tên Nguyễn."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 6: Office & Workplace (B1)
(10, 6, 'Deadline', '/ˈded.laɪn/', 'B1', 'Hạn chót, thời hạn hoàn thành',
 'https://images.unsplash.com/photo-1506784983877-45594efa4cbe',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/deadline--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/deadline--_gb_1.mp3',
 'Rất hay dùng với: meet a deadline (kịp hạn), miss a deadline (trễ hạn).',
 '[{"sentence": "We are working overtime to meet the project deadline.", "translationVi": "Chúng tôi đang làm thêm giờ để kịp thời hạn dự án."}]'::jsonb,
 '[{"phrase": "meet a deadline", "meaningVi": "kịp hạn chót"}, {"phrase": "tight deadline", "meaningVi": "thời hạn gấp rút"}]'::jsonb,
 '[{"speaker": "Boss", "sentence": "Can you submit the report before the deadline?", "translationVi": "Cậu có thể nộp báo cáo trước hạn chót không?"}, {"speaker": "Staff", "sentence": "Certainly, it will be ready by 3 PM.", "translationVi": "Chắc chắn rồi, 3 giờ chiều sẽ sẵn sàng ạ."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(11, 6, 'Collaborate', '/kəˈlæb.ə.reɪt/', 'B1', 'Hợp tác, cộng tác làm việc',
 'https://images.unsplash.com/photo-1522071820081-009f0129c71c',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/collaborate--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/collaborate--_gb_1.mp3',
 'Đi kèm giới từ with (ai) hoặc on (công việc gì).',
 '[{"sentence": "Our team collaborates closely with the marketing department.", "translationVi": "Đội ngũ của chúng tôi cộng tác chặt chẽ với phòng tiếp thị."}]'::jsonb,
 '[{"phrase": "collaborate closely", "meaningVi": "hợp tác chặt chẽ"}, {"phrase": "collaborate on a project", "meaningVi": "hợp tác trong một dự án"}]'::jsonb,
 '[{"speaker": "A", "sentence": "Shall we collaborate on this presentation?", "translationVi": "Chúng mình cùng làm chung bài thuyết trình này nhé?"}, {"speaker": "B", "sentence": "Great idea, let us split the slides.", "translationVi": "Ý hay đấy, chia slide ra làm nhé."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 7: Business Meetings (B1)
(12, 7, 'Negotiate', '/nəˈɡoʊ.ʃi.eɪt/', 'B1', 'Đàm phán, thương lượng',
 'https://images.unsplash.com/photo-1542744173-8e7e53415bb0',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/negotiate--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/negotiate--_gb_1.mp3',
 'Thường đi với: negotiate terms (thương lượng điều khoản), negotiate a contract.',
 '[{"sentence": "The CEO negotiated a favorable contract with suppliers.", "translationVi": "Giám đốc điều hành đã đàm phán được một hợp đồng có lợi với các nhà cung cấp."}]'::jsonb,
 '[{"phrase": "negotiate a contract", "meaningVi": "thương lượng hợp đồng"}, {"phrase": "negotiate terms", "meaningVi": "đàm phán điều khoản"}]'::jsonb,
 '[{"speaker": "Manager", "sentence": "Are we ready to negotiate price tomorrow?", "translationVi": "Ngày mai chúng ta đã sẵn sàng thương lượng về giá chưa?"}, {"speaker": "Analyst", "sentence": "Yes, all market data is prepared.", "translationVi": "Vâng, toàn bộ số liệu thị trường đã sẵn sàng."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 8: Environment & Climate (B2)
(13, 8, 'Sustainable', '/səˈsteɪ.nə.bəl/', 'B2', 'Bền vững, thân thiện với môi trường',
 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sustainable--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/sustainable--_gb_1.mp3',
 'Từ vựng trọng tâm trong bài thi IELTS Speaking/Writing Task 2.',
 '[{"sentence": "Solar power is a clean and sustainable energy source.", "translationVi": "Năng lượng mặt trời là nguồn năng lượng sạch và bền vững."}]'::jsonb,
 '[{"phrase": "sustainable development", "meaningVi": "phát triển bền vững"}, {"phrase": "sustainable energy", "meaningVi": "năng lượng bền vững"}]'::jsonb,
 '[{"speaker": "A", "sentence": "Why should companies adopt sustainable practices?", "translationVi": "Tại sao các công ty nên áp dụng các biện pháp bền vững?"}, {"speaker": "B", "sentence": "It preserves resources and attracts conscious consumers.", "translationVi": "Nó giúp bảo tồn tài nguyên và thu hút người tiêu dùng có ý thức."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 9: Technology & AI (B2)
(14, 9, 'Algorithm', '/ˈæl.ɡə.rɪð.əm/', 'B2', 'Thuật toán',
 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/algorithm--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/algorithm--_gb_1.mp3',
 'Trình tự các quy tắc tính toán giải quyết một bài toán cụ thể.',
 '[{"sentence": "The recommendation algorithm suggests relevant songs.", "translationVi": "Thuật toán gợi ý sẽ đề xuất những bài hát phù hợp."}]'::jsonb,
 '[{"phrase": "machine learning algorithm", "meaningVi": "thuật toán học máy"}, {"phrase": "search algorithm", "meaningVi": "thuật toán tìm kiếm"}]'::jsonb,
 '[{"speaker": "Dev", "sentence": "Which algorithm did you apply for sorting?", "translationVi": "Cậu dùng thuật toán nào để sắp xếp vậy?"}, {"speaker": "Engineer", "sentence": "I used QuickSort for optimal performance.", "translationVi": "Mình dùng QuickSort để tối ưu hiệu năng."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Topic 10: Higher Education & Research (C1)
(15, 10, 'Dissertation', '/ˌdɪs.ɚˈteɪ.ʃən/', 'C1', 'Luận văn tốt nghiệp, luận án tiến sĩ',
 'https://images.unsplash.com/photo-1523050854058-8df90110c9f1',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/dissertation--_us_1.mp3',
 'https://ssl.gstatic.com/dictionary/static/sounds/20200429/dissertation--_gb_1.mp3',
 'Văn bản học thuật chuyên sâu yêu cầu nghiên cứu gốc độc lập.',
 '[{"sentence": "She submitted her doctoral dissertation after four years of research.", "translationVi": "Cô ấy đã nộp luận án tiến sĩ sau 4 năm nghiên cứu."}]'::jsonb,
 '[{"phrase": "write a dissertation", "meaningVi": "viết luận văn"}, {"phrase": "defend a dissertation", "meaningVi": "bảo vệ luận văn"}]'::jsonb,
 '[{"speaker": "Advisor", "sentence": "How is the progress on your dissertation?", "translationVi": "Tiến độ luận án của em đến đâu rồi?"}, {"speaker": "PhD Student", "sentence": "I have completed the data analysis chapter.", "translationVi": "Em đã hoàn thành chương phân tích dữ liệu rồi ạ."}]'::jsonb,
 'PUBLISHED', (SELECT id FROM users WHERE email = 'admin@gmail.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET word = EXCLUDED.word,
    ipa_transcription = EXCLUDED.ipa_transcription,
    cefr_level = EXCLUDED.cefr_level,
    definition_vi = EXCLUDED.definition_vi,
    image_url = EXCLUDED.image_url,
    audio_us_url = EXCLUDED.audio_us_url,
    audio_uk_url = EXCLUDED.audio_uk_url,
    nuance_note = EXCLUDED.nuance_note,
    example_sentences_json = EXCLUDED.example_sentences_json,
    collocation_json = EXCLUDED.collocation_json,
    dialogue_json = EXCLUDED.dialogue_json,
    status = EXCLUDED.status;

SELECT setval(pg_get_serial_sequence('vocabulary', 'id'), (SELECT MAX(id) FROM vocabulary));

-- ------------------------------------------------------------------------------
-- 4. STUDENT STATS: Khởi tạo thông số Gamification cho học viên
-- ------------------------------------------------------------------------------
INSERT INTO student_stats (
    student_id, total_xp, current_streak, longest_streak, streak_freeze_count,
    last_activity_date, total_study_minutes, updated_at
)
SELECT 
    u.id, 450, 5, 14, 2, CURRENT_DATE, 240, CURRENT_TIMESTAMP
FROM users u
WHERE u.role = 'STUDENT'
ON CONFLICT (student_id) DO UPDATE
SET total_xp = EXCLUDED.total_xp,
    current_streak = EXCLUDED.current_streak,
    longest_streak = EXCLUDED.longest_streak,
    streak_freeze_count = EXCLUDED.streak_freeze_count,
    last_activity_date = CURRENT_DATE,
    total_study_minutes = EXCLUDED.total_study_minutes,
    updated_at = CURRENT_TIMESTAMP;

-- ------------------------------------------------------------------------------
-- 5. STUDENT VOCABULARY PROGRESS: Lịch ôn tập Spaced Repetition (SRS)
-- Đảm bảo có từ ĐẾN HẠN ÔN HÔM NAY (next_review_at <= now) để test ngay API!
-- ------------------------------------------------------------------------------
INSERT INTO student_vocabulary_progress (
    student_id, vocabulary_id, status, next_review_at, correct_count, incorrect_count,
    last_practiced_at, easiness_factor, interval_days, repetitions
)
SELECT 
    s.id, v.id,
    CASE 
        WHEN v.id IN (1, 2, 7) THEN 'REVIEWING'::character varying
        WHEN v.id IN (3, 5, 8) THEN 'LEARNING'::character varying
        WHEN v.id IN (10, 11)  THEN 'MASTERED'::character varying
        ELSE 'NEW'::character varying
    END,
    CASE 
        -- Các từ 1, 2, 3, 7 đến hạn ôn ngay hôm nay (next_review_at nằm trong quá khứ)
        WHEN v.id IN (1, 2, 3, 7) THEN CURRENT_TIMESTAMP - INTERVAL '2 hours'
        -- Các từ khác đặt lịch ôn trong tương lai
        WHEN v.id IN (5, 8)       THEN CURRENT_TIMESTAMP + INTERVAL '2 days'
        WHEN v.id IN (10, 11)     THEN CURRENT_TIMESTAMP + INTERVAL '14 days'
        ELSE NULL
    END,
    CASE WHEN v.id IN (10, 11) THEN 5 WHEN v.id IN (1, 2, 7) THEN 2 ELSE 0 END,
    CASE WHEN v.id IN (3, 5) THEN 1 ELSE 0 END,
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CASE WHEN v.id IN (10, 11) THEN 2.6 ELSE 2.5 END,
    CASE WHEN v.id IN (10, 11) THEN 14 WHEN v.id IN (1, 2, 7) THEN 1 ELSE 0 END,
    CASE WHEN v.id IN (10, 11) THEN 3 WHEN v.id IN (1, 2, 7) THEN 1 ELSE 0 END
FROM users s
CROSS JOIN vocabulary v
WHERE s.email = 'student@example.com' AND v.id <= 12
ON CONFLICT (student_id, vocabulary_id) DO UPDATE
SET status = EXCLUDED.status,
    next_review_at = EXCLUDED.next_review_at,
    correct_count = EXCLUDED.correct_count,
    incorrect_count = EXCLUDED.incorrect_count,
    last_practiced_at = EXCLUDED.last_practiced_at,
    easiness_factor = EXCLUDED.easiness_factor,
    interval_days = EXCLUDED.interval_days,
    repetitions = EXCLUDED.repetitions;

-- ------------------------------------------------------------------------------
-- 6. MINIGAME RESULTS: Lịch sử chơi game tương tác
-- ------------------------------------------------------------------------------
INSERT INTO minigame_results (student_id, game_type, topic_id, score, xp_earned, duration_seconds, played_at)
SELECT 
    s.id, 'LISTEN_CHOOSE', 1, 100, 15, 3, CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM users s WHERE s.email = 'student@example.com'
UNION ALL
SELECT 
    s.id, 'WORD_SCRAMBLE', 4, 100, 20, 2, CURRENT_TIMESTAMP - INTERVAL '18 hours'
FROM users s WHERE s.email = 'student@example.com'
UNION ALL
SELECT 
    s.id, 'MATCHING_FLASH', 3, 100, 10, 5, CURRENT_TIMESTAMP - INTERVAL '5 hours'
FROM users s WHERE s.email = 'student@example.com';

-- ------------------------------------------------------------------------------
-- 7. CLASSES / COURSES: Khóa học của giáo viên
-- ------------------------------------------------------------------------------
INSERT INTO classes (id, name, teacher_id, description, cefr_target, is_active, start_date, end_date, created_at, updated_at)
VALUES
  (1, 'Tiếng Anh Giao Tiếp Toàn Diện (A1-A2)', 
   (SELECT id FROM users WHERE email = 'teacher@example.com'),
   'Khóa học tập trung phản xạ giao tiếp đời sống, du lịch và thói quen hàng ngày.',
   'A2', true, CURRENT_DATE - INTERVAL '14 days', CURRENT_DATE + INTERVAL '45 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'IELTS Intensive - Bứt Phá Band 6.5+', 
   (SELECT id FROM users WHERE email = 'sarah.teacher@gmail.com'),
   'Luyện chuyên sâu từ vựng học thuật, đề thi IELTS Speaking & Writing.',
   'B2', true, CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE + INTERVAL '60 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'English for Business & Workplace', 
   (SELECT id FROM users WHERE email = 'teacher@example.com'),
   'Tiếng Anh thương mại, viết email, thuyết trình và đàm phán hợp đồng.',
   'B1', true, CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    teacher_id = EXCLUDED.teacher_id,
    description = EXCLUDED.description,
    cefr_target = EXCLUDED.cefr_target,
    is_active = EXCLUDED.is_active,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('classes', 'id'), (SELECT MAX(id) FROM classes));

-- ------------------------------------------------------------------------------
-- 8. CLASS_STUDENTS: Gán học viên vào khóa học
-- ------------------------------------------------------------------------------
INSERT INTO class_students (class_id, student_id, joined_at, status, updated_at)
SELECT 
    1, u.id, CURRENT_TIMESTAMP - INTERVAL '14 days', 'ACTIVE', CURRENT_TIMESTAMP
FROM users u WHERE u.email IN ('student@example.com', 'student2@gmail.com')
ON CONFLICT (class_id, student_id) DO NOTHING;

INSERT INTO class_students (class_id, student_id, joined_at, status, updated_at)
SELECT 
    2, u.id, CURRENT_TIMESTAMP - INTERVAL '7 days', 'ACTIVE', CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'student@example.com'
ON CONFLICT (class_id, student_id) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 9. TEACHING MATERIALS: Tài liệu học tập cho lớp học
-- ------------------------------------------------------------------------------
INSERT INTO teaching_materials (id, teacher_id, class_id, title, file_type, file_url, file_size_kb, is_live_presenting, created_at, updated_at)
VALUES
  (1, (SELECT id FROM users WHERE email = 'teacher@example.com'), 1, 
   'Slide Bài giảng: Daily Life & Vocabulary', 'PDF', 'https://cdn.example.com/materials/daily_life_slides.pdf', 2450, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, (SELECT id FROM users WHERE email = 'teacher@example.com'), 1, 
   'Audio Luyện Nghe: At the Restaurant', 'MP3', 'https://cdn.example.com/materials/restaurant_dialogue.mp3', 5120, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, (SELECT id FROM users WHERE email = 'sarah.teacher@gmail.com'), 2, 
   'IELTS Speaking Part 2 Model Answers', 'PDF', 'https://cdn.example.com/materials/ielts_part2_samples.pdf', 3120, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    file_type = EXCLUDED.file_type,
    file_url = EXCLUDED.file_url,
    file_size_kb = EXCLUDED.file_size_kb,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('teaching_materials', 'id'), (SELECT MAX(id) FROM teaching_materials));

-- ------------------------------------------------------------------------------
-- 10. SYLLABUS_ITEMS & TOPICS: Giáo trình bài giảng theo tuần
-- ------------------------------------------------------------------------------
INSERT INTO syllabus_items (id, class_id, week_number, title, description, scheduled_date, material_id, sort_order, updated_at)
VALUES
  (1, 1, 1, 'Tuần 1: Khởi động & Thói quen hàng ngày', 
   'Giới thiệu bản thân, lịch trình sinh hoạt và các hoạt động thường nhật.',
   CURRENT_DATE - INTERVAL '14 days', 1, 1, CURRENT_TIMESTAMP),
  (2, 1, 2, 'Tuần 2: Ẩm thực & Gọi món nhà hàng', 
   'Mẫu câu giao tiếp gọi món, thực đơn và văn hóa ăn uống.',
   CURRENT_DATE - INTERVAL '7 days', 2, 2, CURRENT_TIMESTAMP),
  (3, 1, 3, 'Tuần 3: Lên kế hoạch Du lịch & Đặt phòng', 
   'Từ vựng sân bay, hành lý, lộ trình di chuyển và khách sạn.',
   CURRENT_DATE, NULL, 3, CURRENT_TIMESTAMP),
  (4, 2, 1, 'Tuần 1: Môi trường & Năng lượng tái tạo', 
   'Phát triển ý tưởng Speaking Part 3 về biến đổi khí hậu.',
   CURRENT_DATE - INTERVAL '7 days', 3, 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    scheduled_date = EXCLUDED.scheduled_date,
    material_id = EXCLUDED.material_id,
    sort_order = EXCLUDED.sort_order,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('syllabus_items', 'id'), (SELECT MAX(id) FROM syllabus_items));

-- Liên kết Syllabus Item với Topics (để Daily Mission tự động phân bổ từ mới)
INSERT INTO syllabus_item_topics (syllabus_item_id, topic_id)
VALUES 
  (1, 1), (1, 2),
  (2, 3),
  (3, 4), (3, 5),
  (4, 8)
ON CONFLICT (syllabus_item_id, topic_id) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 11. ASSIGNMENTS & SUBMISSIONS: Bài tập về nhà & Chấm điểm
-- ------------------------------------------------------------------------------
INSERT INTO assignments (id, class_id, teacher_id, title, description, module_type, ref_id, deadline_at, created_at, updated_at)
VALUES
  (1, 1, (SELECT id FROM users WHERE email = 'teacher@example.com'),
   'Bài tập Flashcard: Ôn tập 10 từ vựng Tuần 1',
   'Học viên hoàn thành chu trình flashcard các từ vựng chủ đề Daily Life.',
   'VOCABULARY', 1, CURRENT_TIMESTAMP + INTERVAL '3 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, (SELECT id FROM users WHERE email = 'teacher@example.com'),
   'Luyện phát âm: 5 cặp âm tối thiểu (Minimal Pairs)',
   'Ghi âm bài đọc các cặp từ dễ nhầm lẫn trong Module 0.',
   'PRONUNCIATION', 1, CURRENT_TIMESTAMP + INTERVAL '5 days', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    deadline_at = EXCLUDED.deadline_at,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(pg_get_serial_sequence('assignments', 'id'), (SELECT MAX(id) FROM assignments));

INSERT INTO assignment_submissions (assignment_id, student_id, score, status, submitted_at, teacher_comment_text, commented_at, updated_at)
SELECT 
    1, s.id, 9.50, 'GRADED', CURRENT_TIMESTAMP - INTERVAL '1 day',
    'Em phát âm rất chuẩn và làm bài đúng hạn. Tiếp tục phát huy nhé!',
    CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP
FROM users s WHERE s.email = 'student@example.com'
ON CONFLICT (assignment_id, student_id) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 12. SPEAKING SCENARIOS: Kịch bản luyện nói AI (Module 3)
-- ------------------------------------------------------------------------------
INSERT INTO speaking_scenarios (
    id, title_vi, title_en, context_description, ai_role_name, ai_role_avatar_url,
    ai_system_prompt, goal_description, hint_phrases_json, cefr_level, topic_id, is_active
) VALUES
(1, 'Gọi đồ uống tại quán Cà phê', 'Ordering Coffee at a Cafe',
 'Bạn đang ghé thăm một quán cà phê tại New York và cần gọi đồ uống cùng bánh ngọt cho buổi sáng.',
 'Barista Alex', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb',
 'You are Alex, a friendly barista at a cozy New York coffee shop. Greet the customer warmly, ask what they would like to drink and eat, and confirm their order with price.',
 'Gọi thành công 1 đồ uống nóng và 1 món bánh ngọt, hỏi giá tiền và cảm ơn.',
 '["I would like a hot latte, please.", "Can I have a chocolate muffin?", "How much does that come to?"]'::jsonb,
 'A1', 3, true),

(2, 'Nhận phòng tại Khách sạn', 'Checking in at a Hotel',
 'Bạn vừa đáp chuyến bay đến London và đến quầy lễ tân khách sạn để nhận phòng đã đặt trước.',
 'Receptionist Emma', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2',
 'You are Emma, the polite receptionist at Grand Hotel London. Welcome the guest, ask for their passport and reservation name, and explain hotel breakfast hours and WiFi.',
 'Cung cấp tên đặt phòng, hỏi về giờ ăn sáng và xin mật khẩu WiFi.',
 '["Good afternoon, I have a reservation under the name Nguyen.", "What time is breakfast served?", "Could you tell me the WiFi password?"]'::jsonb,
 'A2', 5, true),

(3, 'Phỏng vấn xin việc - Giới thiệu bản thân', 'Job Interview - Self Introduction',
 'Bạn đang tham gia buổi phỏng vấn vị trí chuyên viên phần mềm tại một tập đoàn đa quốc gia.',
 'HR Manager David', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d',
 'You are David, a professional HR Director. Ask the candidate to introduce themselves, describe their greatest strength and explain why they want to join the company.',
 'Giới thiệu kinh nghiệm học tập, thế mạnh chuyên môn và thể hiện sự nhiệt huyết với vị trí ứng tuyển.',
 '["I graduated with a degree in Computer Science.", "My greatest strength is problem-solving.", "I am eager to contribute to your team."] '::jsonb,
 'B1', 6, true),

(4, 'Thảo luận giải pháp Môi trường', 'Debating Environmental Solutions',
 'Bạn tham gia hội thảo quốc tế về giải pháp hạn chế rác thải nhựa và năng lượng tái tạo.',
 'Dr. Green (Environmentalist)', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e',
 'You are Dr. Green, an environmental scientist. Engage in an intellectual discussion about carbon footprint, renewable energy, and government regulations.',
 'Trình bày quan điểm về việc thay thế nhiên liệu hóa thạch và đề xuất chính sách giảm đồ nhựa dùng 1 lần.',
 '["In my view, transitioning to solar energy is essential.", "Governments should impose stricter bans on single-use plastics.", "Sustainable practices benefit both ecology and economy."] '::jsonb,
 'B2', 8, true)
ON CONFLICT (id) DO UPDATE
SET title_vi = EXCLUDED.title_vi,
    title_en = EXCLUDED.title_en,
    context_description = EXCLUDED.context_description,
    ai_role_name = EXCLUDED.ai_role_name,
    ai_role_avatar_url = EXCLUDED.ai_role_avatar_url,
    ai_system_prompt = EXCLUDED.ai_system_prompt,
    goal_description = EXCLUDED.goal_description,
    hint_phrases_json = EXCLUDED.hint_phrases_json,
    cefr_level = EXCLUDED.cefr_level,
    topic_id = EXCLUDED.topic_id,
    is_active = EXCLUDED.is_active;

SELECT setval(pg_get_serial_sequence('speaking_scenarios', 'id'), (SELECT MAX(id) FROM speaking_scenarios));

-- ------------------------------------------------------------------------------
-- 13. NOTIFICATIONS: Thông báo chào mừng & nhắc nhở học tập
-- ------------------------------------------------------------------------------
INSERT INTO notifications (user_id, title, body, type, is_read, data, created_at)
SELECT 
    s.id,
    'Chào mừng bạn đến với English App!',
    'Hãy bắt đầu ngày mới bằng việc hoàn thành nhiệm vụ 5 từ vựng mới và ôn tập các từ đến hạn nhé!',
    'SYSTEM', false, '{"screen": "DAILY_MISSION"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM users s WHERE s.email = 'student@example.com'
UNION ALL
SELECT 
    s.id,
    '🔥 Chuỗi Streak 5 ngày!',
    'Chúc mừng bạn đã duy trì chuỗi học tập 5 ngày liên tiếp. Học ngay để giữ streak nào!',
    'STREAK_REMINDER', false, '{"screen": "HOME"}'::jsonb, CURRENT_TIMESTAMP - INTERVAL '3 hours'
FROM users s WHERE s.email = 'student@example.com';

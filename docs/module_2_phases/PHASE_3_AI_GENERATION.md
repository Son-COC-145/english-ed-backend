# GIAI ĐOẠN 3: TỰ ĐỘNG SINH TỪ VỰNG BẰNG AI (ADMIN WORKFLOW)

## 1. Mục tiêu (Objective)
Cho phép Admin chỉ cần nhập `Word`, `Topic`, `CEFR` -> Hệ thống tự động gọi Text AI, Image AI, và Audio TTS để sinh toàn bộ Flashcard data chuẩn JSON lưu vào DB.

## 2. Chi tiết thực hiện (Implementation Steps)

### Bước 1: Xây dựng Text Generation Service (AiTextService.java)
- Sử dụng Spring WebClient (hoặc `RestTemplate` / `Spring AI`) gọi API OpenAI hoặc Gemini.
- **Prompt Architecture:**
  *"Generate JSON for the English word '{word}' in topic '{topic}', CEFR '{cefr}'. JSON must exactly follow this schema: { "ipaTranscription": "...", "definitionVi": "...", "nuanceNote": "...", "exampleSentencesJson": [ {"en": "...", "vi": "..."} ], "collocationJson": [ {"collocation": "...", "vi": "..."} ], "dialogueJson": [ {"speaker": "A", "en": "...", "vi": "..."} ] }"*
- **Xử lý:** Parse JSON trả về bằng `ObjectMapper` của Jackson sang các Object Java và gán vào Entity `Vocabulary`.

### Bước 2: Xây dựng Image & TTS Services
- **`ImageGenerationService.java`**: 
  - Gọi API Pollinations.ai (VD: `https://image.pollinations.ai/prompt/flat vector icon of {word} {topic}`) tải ảnh về dạng byte array.
  - Dùng thư viện Cloudinary (đã cấu hình trong pom.xml) upload ảnh này lên mây, lấy về public `imageUrl`.
- **`TtsGenerationService.java`**:
  - Gọi Google Cloud TTS (Text-to-Speech) để sinh 2 file MP3: giọng `en-US` và `en-GB`.
  - Tương tự, upload byte array MP3 lên Cloudinary, lấy về `audioUsUrl` và `audioUkUrl`.

### Bước 3: Admin Controller & Điều phối (AdminVocabularyController.java)
- Viết API `POST /api/v1/admin/vocabularies/generate`
- **Logic đa luồng (Concurrency - Tùy chọn nâng cao):** 
  - Vì gọi API Text, Image, Audio đều tốn thời gian (I/O blocking), ta nên sử dụng `CompletableFuture` của Java để gọi 3 Services cùng lúc thay vì chạy tuần tự. (Tăng tốc độ sinh data từ 10s xuống 3s).
- Lưu toàn bộ kết quả vào Entity `Vocabulary` với trạng thái `status = DRAFT`.
- Trả về API (Response) nội dung vừa tạo để Admin nhìn thấy trên màn hình.

### Bước 4: Chức năng Publish
- Viết API `PUT /api/v1/admin/vocabularies/{id}/publish`. 
- Đổi status từ `DRAFT` thành `PUBLISHED` (Lúc này App của User mới bắt đầu thấy từ vựng này).

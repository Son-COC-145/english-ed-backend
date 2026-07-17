INSERT INTO questions (cefr_level, skill, question_type, content_json, correct_answer, timeout_seconds, is_active, created_at) VALUES
-- A1 (8 questions)
('A1', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "I have a ___ and a sister.", "options": ["brother", "book", "car", "dog"]}'::jsonb, 'brother', 30, true, CURRENT_TIMESTAMP),
('A1', 'GRAMMAR', 'MULTIPLE_CHOICE', '{"question": "She ___ from Spain.", "options": ["am", "is", "are", "be"]}'::jsonb, 'is', 30, true, CURRENT_TIMESTAMP),
('A1', 'READING', 'READING_COMPREHENSION', '{"text": "My name is Tom. I am 25 years old. I live in London.", "question": "Where does Tom live?", "options": ["Paris", "New York", "London", "Tokyo"]}'::jsonb, 'London', 90, true, CURRENT_TIMESTAMP),
('A1', 'LISTENING', 'LISTENING', '{"audio_url": "https://example.com/audio/a1_1.mp3", "question": "What is the boy''s name?", "options": ["John", "Mike", "Tom", "David"]}'::jsonb, 'John', 60, true, CURRENT_TIMESTAMP),
('A1', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "I eat an ___ every morning.", "options": ["apple", "water", "bread", "milk"]}'::jsonb, 'apple', 30, true, CURRENT_TIMESTAMP),
('A1', 'GRAMMAR', 'FILL_BLANK', '{"question": "___ you like pizza?"}'::jsonb, 'Do', 30, true, CURRENT_TIMESTAMP),
('A1', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "book", "ipa": "/bʊk/"}'::jsonb, 'book', 10, true, CURRENT_TIMESTAMP),
('A1', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "cat", "ipa": "/kæt/"}'::jsonb, 'cat', 10, true, CURRENT_TIMESTAMP),

-- A2 (8 questions)
('A2', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "I went to the ___ to buy some medicine.", "options": ["supermarket", "pharmacy", "library", "bank"]}'::jsonb, 'pharmacy', 30, true, CURRENT_TIMESTAMP),
('A2', 'GRAMMAR', 'MULTIPLE_CHOICE', '{"question": "I ___ to the cinema yesterday.", "options": ["go", "goes", "went", "going"]}'::jsonb, 'went', 30, true, CURRENT_TIMESTAMP),
('A2', 'READING', 'READING_COMPREHENSION', '{"text": "Yesterday, Sarah went shopping. She bought a new dress and a pair of shoes. The dress was red and the shoes were black.", "question": "What color was the dress?", "options": ["Black", "Red", "Blue", "Green"]}'::jsonb, 'Red', 90, true, CURRENT_TIMESTAMP),
('A2', 'LISTENING', 'LISTENING', '{"audio_url": "https://example.com/audio/a2_1.mp3", "question": "What time does the train leave?", "options": ["9:00", "9:30", "10:00", "10:30"]}'::jsonb, '9:30', 60, true, CURRENT_TIMESTAMP),
('A2', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "My father''s brother is my ___.", "options": ["uncle", "aunt", "cousin", "grandfather"]}'::jsonb, 'uncle', 30, true, CURRENT_TIMESTAMP),
('A2', 'GRAMMAR', 'FILL_BLANK', '{"question": "She is ___ than her sister."}'::jsonb, 'taller', 30, true, CURRENT_TIMESTAMP),
('A2', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "thought", "ipa": "/θɔːt/"}'::jsonb, 'thought', 10, true, CURRENT_TIMESTAMP),
('A2', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "world", "ipa": "/wɜːrld/"}'::jsonb, 'world', 10, true, CURRENT_TIMESTAMP),

-- B1 (8 questions)
('B1', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "Could you ___ me a favor?", "options": ["make", "do", "take", "give"]}'::jsonb, 'do', 30, true, CURRENT_TIMESTAMP),
('B1', 'GRAMMAR', 'MULTIPLE_CHOICE', '{"question": "If it rains tomorrow, we ___ at home.", "options": ["stay", "will stay", "would stay", "stayed"]}'::jsonb, 'will stay', 30, true, CURRENT_TIMESTAMP),
('B1', 'READING', 'READING_COMPREHENSION', '{"text": "The Eiffel Tower is a wrought-iron lattice tower on the Champ de Mars in Paris, France. It is named after the engineer Gustave Eiffel, whose company designed and built the tower.", "question": "Who is the tower named after?", "options": ["A French king", "The city of Paris", "An engineer", "A scientist"]}'::jsonb, 'An engineer', 90, true, CURRENT_TIMESTAMP),
('B1', 'LISTENING', 'LISTENING', '{"audio_url": "https://example.com/audio/b1_1.mp3", "question": "Why is the woman calling?", "options": ["To book a flight", "To cancel a reservation", "To complain about service", "To ask for information"]}'::jsonb, 'To cancel a reservation', 60, true, CURRENT_TIMESTAMP),
('B1', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "He was very ___ when he failed his driving test.", "options": ["disappointed", "excited", "thrilled", "amused"]}'::jsonb, 'disappointed', 30, true, CURRENT_TIMESTAMP),
('B1', 'GRAMMAR', 'FILL_BLANK', '{"question": "I have ___ living here for 5 years."}'::jsonb, 'been', 30, true, CURRENT_TIMESTAMP),
('B1', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "schedule", "ipa": "/ˈʃedʒuːl/"}'::jsonb, 'schedule', 10, true, CURRENT_TIMESTAMP),
('B1', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "museum", "ipa": "/mjuˈziːəm/"}'::jsonb, 'museum', 10, true, CURRENT_TIMESTAMP),

-- B2 (8 questions)
('B2', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "The company decided to ___ its operations in Asia.", "options": ["expand", "shrink", "decrease", "reduce"]}'::jsonb, 'expand', 30, true, CURRENT_TIMESTAMP),
('B2', 'GRAMMAR', 'MULTIPLE_CHOICE', '{"question": "By this time next year, I ___ my university degree.", "options": ["will finish", "will have finished", "have finished", "finish"]}'::jsonb, 'will have finished', 30, true, CURRENT_TIMESTAMP),
('B2', 'READING', 'READING_COMPREHENSION', '{"text": "Climate change refers to long-term shifts in temperatures and weather patterns. These shifts may be natural, such as through variations in the solar cycle. But since the 1800s, human activities have been the main driver of climate change.", "question": "According to the text, what is the main cause of climate change since the 1800s?", "options": ["Solar cycle variations", "Human activities", "Natural shifts", "Volcanic eruptions"]}'::jsonb, 'Human activities', 90, true, CURRENT_TIMESTAMP),
('B2', 'LISTENING', 'LISTENING', '{"audio_url": "https://example.com/audio/b2_1.mp3", "question": "What is the main topic of the lecture?", "options": ["The history of art", "The impact of technology on society", "The basics of quantum physics", "The life cycle of stars"]}'::jsonb, 'The impact of technology on society', 60, true, CURRENT_TIMESTAMP),
('B2', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "She has a very ___ schedule this week.", "options": ["tight", "loose", "empty", "free"]}'::jsonb, 'tight', 30, true, CURRENT_TIMESTAMP),
('B2', 'GRAMMAR', 'FILL_BLANK', '{"question": "Hardly ___ I arrived when the phone rang."}'::jsonb, 'had', 30, true, CURRENT_TIMESTAMP),
('B2', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "entrepreneur", "ipa": "/ˌɑːntrəprəˈnɜːr/"}'::jsonb, 'entrepreneur', 10, true, CURRENT_TIMESTAMP),
('B2', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "choir", "ipa": "/ˈkwaɪər/"}'::jsonb, 'choir', 10, true, CURRENT_TIMESTAMP),

-- C1 (8 questions)
('C1', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "His argument was so ___ that everyone agreed with him.", "options": ["persuasive", "flawed", "ambiguous", "vague"]}'::jsonb, 'persuasive', 30, true, CURRENT_TIMESTAMP),
('C1', 'GRAMMAR', 'MULTIPLE_CHOICE', '{"question": "___ you to change your mind, please let me know.", "options": ["Were", "If", "Should", "Had"]}'::jsonb, 'Were', 30, true, CURRENT_TIMESTAMP),
('C1', 'READING', 'READING_COMPREHENSION', '{"text": "The advent of artificial intelligence has sparked a plethora of debates regarding its ethical implications. While some hail it as the panacea for all modern woes, others caution against the unforeseen ramifications of unchecked technological advancement.", "question": "What does the word ''panacea'' mean in this context?", "options": ["A complicated problem", "A cure-all solution", "A controversial topic", "An inevitable disaster"]}'::jsonb, 'A cure-all solution', 90, true, CURRENT_TIMESTAMP),
('C1', 'LISTENING', 'LISTENING', '{"audio_url": "https://example.com/audio/c1_1.mp3", "question": "What is the speaker''s attitude towards the new policy?", "options": ["Highly supportive", "Cautiously optimistic", "Strongly opposed", "Indifferent"]}'::jsonb, 'Cautiously optimistic', 60, true, CURRENT_TIMESTAMP),
('C1', 'VOCABULARY', 'MULTIPLE_CHOICE', '{"question": "The politician tried to ___ the blame onto his opponent.", "options": ["shift", "take", "make", "do"]}'::jsonb, 'shift', 30, true, CURRENT_TIMESTAMP),
('C1', 'GRAMMAR', 'FILL_BLANK', '{"question": "Not only ___ he late, but he also forgot his notes."}'::jsonb, 'was', 30, true, CURRENT_TIMESTAMP),
('C1', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "anemone", "ipa": "/əˈneməni/"}'::jsonb, 'anemone', 10, true, CURRENT_TIMESTAMP),
('C1', 'PRONUNCIATION', 'PRONUNCIATION', '{"word": "synecdoche", "ipa": "/sɪˈnekdəki/"}'::jsonb, 'synecdoche', 10, true, CURRENT_TIMESTAMP);

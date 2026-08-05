UPDATE questions
SET content_json = jsonb_set(content_json, '{transcript}', '"Hello, my name is John."')
WHERE cefr_level = 'A1' AND skill = 'LISTENING';

UPDATE questions
SET content_json = jsonb_set(content_json, '{transcript}', '"Attention passengers, the train to London will depart at nine thirty."')
WHERE cefr_level = 'A2' AND skill = 'LISTENING';

UPDATE questions
SET content_json = jsonb_set(content_json, '{transcript}', '"Hello, I would like to cancel my flight reservation."')
WHERE cefr_level = 'B1' AND skill = 'LISTENING';

UPDATE questions
SET content_json = jsonb_set(content_json, '{transcript}', '"Today, we will discuss the impact of technology on society."')
WHERE cefr_level = 'B2' AND skill = 'LISTENING';

UPDATE questions
SET content_json = jsonb_set(content_json, '{transcript}', '"While some are opposed to the new policy, I am cautiously optimistic about its potential."')
WHERE cefr_level = 'C1' AND skill = 'LISTENING';

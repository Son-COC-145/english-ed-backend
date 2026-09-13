-- Student upload bytes already exist before STT starts; TTS readiness applies only to AI.
UPDATE speaking_turns
SET audio_status = CASE WHEN audio_data IS NULL THEN 'NOT_APPLICABLE' ELSE 'READY' END
WHERE speaker = 'STUDENT';

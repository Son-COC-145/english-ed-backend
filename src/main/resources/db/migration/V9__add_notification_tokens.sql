CREATE TABLE IF NOT EXISTS user_device_tokens (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(1000) NOT NULL,
    device_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_device_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_token UNIQUE (user_id, token)
);

CREATE INDEX idx_user_device_tokens_user_id ON user_device_tokens(user_id);
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    order_info VARCHAR(500),
    vnp_txn_ref VARCHAR(50) UNIQUE,
    vnp_transaction_no VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payment_transactions_sub FOREIGN KEY (subscription_id) REFERENCES user_subscriptions (id)
);

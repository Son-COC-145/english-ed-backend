ALTER TABLE assignment_submissions ADD COLUMN grading_revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notification_outbox ADD COLUMN claim_token VARCHAR(36);

CREATE TABLE notification_push_deliveries (
    id BIGSERIAL PRIMARY KEY,
    outbox_event_id BIGINT NOT NULL REFERENCES notification_outbox(id),
    device_token TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    last_error TEXT,
    sent_at TIMESTAMP,
    UNIQUE(outbox_event_id, device_token)
);

CREATE TABLE stored_files (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    object_key TEXT NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    byte_size BIGINT NOT NULL,
    url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claim_token VARCHAR(36),
    locked_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, object_key, resource_type)
);
CREATE INDEX idx_stored_files_cleanup ON stored_files(status, available_at);

-- Check URL and JSON references across all modules without hard-coding business tables.
CREATE FUNCTION storage_url_is_referenced(file_url TEXT) RETURNS BOOLEAN
LANGUAGE plpgsql AS $$
DECLARE column_info RECORD; found BOOLEAN;
BEGIN
    IF file_url IS NULL THEN RETURN FALSE; END IF;
    FOR column_info IN
        SELECT table_schema, table_name, column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name <> 'stored_files'
          AND ((column_name LIKE '%url' AND data_type IN ('text','character varying'))
               OR data_type IN ('json','jsonb'))
    LOOP
        IF column_info.data_type IN ('json','jsonb') THEN
            EXECUTE format('SELECT EXISTS(SELECT 1 FROM %I.%I WHERE position($1 in %I::text)>0)',
                column_info.table_schema,column_info.table_name,column_info.column_name) INTO found USING file_url;
        ELSE
            EXECUTE format('SELECT EXISTS(SELECT 1 FROM %I.%I WHERE %I=$1)',
                column_info.table_schema,column_info.table_name,column_info.column_name) INTO found USING file_url;
        END IF;
        IF found THEN RETURN TRUE; END IF;
    END LOOP;
    RETURN FALSE;
END;
$$;

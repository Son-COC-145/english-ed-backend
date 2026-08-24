CREATE TABLE syllabus_item_topics (
    syllabus_item_id BIGINT NOT NULL,
    topic_id SMALLINT NOT NULL,
    PRIMARY KEY (syllabus_item_id, topic_id),
    CONSTRAINT fk_syllabus_item FOREIGN KEY (syllabus_item_id) REFERENCES syllabus_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

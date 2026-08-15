DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens RENAME COLUMN token TO token_hash;
ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
ALTER TABLE refresh_tokens ADD COLUMN family_id UUID NOT NULL;
ALTER TABLE refresh_tokens ADD COLUMN parent_token_id UUID;
ALTER TABLE refresh_tokens ADD COLUMN rotated_at TIMESTAMP(6);
ALTER TABLE refresh_tokens ADD COLUMN reuse_detected BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_parent
    FOREIGN KEY (parent_token_id) REFERENCES refresh_tokens (id);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id, revoked, expiry_date);

ALTER TABLE blog_posts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE blog_posts ADD COLUMN scheduled_publish_at TIMESTAMP(6);
ALTER TABLE blog_posts ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(excerpt, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(content, '')), 'C')
    ) STORED;

CREATE INDEX idx_blog_posts_search_vector ON blog_posts USING GIN (search_vector);
CREATE INDEX idx_blog_posts_scheduled
    ON blog_posts (scheduled_publish_at)
    WHERE status = 'DRAFT' AND hidden = FALSE AND scheduled_publish_at IS NOT NULL;

CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(60) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE post_tags (
    post_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tags_post
        FOREIGN KEY (post_id) REFERENCES blog_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE INDEX idx_post_tags_tag ON post_tags (tag_id, post_id);

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    available_at TIMESTAMP(6) NOT NULL,
    locked_at TIMESTAMP(6),
    processed_at TIMESTAMP(6),
    last_error VARCHAR(1000),
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_notification_outbox_ready
    ON notification_outbox (status, available_at, created_at);

CREATE TABLE auth_rate_limits (
    scope VARCHAR(80) NOT NULL,
    client_key VARCHAR(64) NOT NULL,
    window_start TIMESTAMP(6) NOT NULL,
    request_count INTEGER NOT NULL,
    PRIMARY KEY (scope, client_key, window_start)
);

CREATE INDEX idx_auth_rate_limits_window ON auth_rate_limits (window_start);

CREATE TABLE blog_posts (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL UNIQUE,
    excerpt VARCHAR(500),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    author_id UUID NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    hidden_at TIMESTAMP(6),
    hidden_by_id UUID,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6),
    CONSTRAINT fk_blog_posts_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_blog_posts_hidden_by FOREIGN KEY (hidden_by_id) REFERENCES users (id)
);

CREATE TABLE post_comments (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    hidden_at TIMESTAMP(6),
    hidden_by_id UUID,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_post_comments_post
        FOREIGN KEY (post_id) REFERENCES blog_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_comments_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_post_comments_hidden_by FOREIGN KEY (hidden_by_id) REFERENCES users (id)
);

CREATE TABLE post_likes (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_post_likes_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES blog_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_blog_posts_public
    ON blog_posts (status, hidden, published_at DESC);
CREATE INDEX idx_blog_posts_author ON blog_posts (author_id, created_at DESC);
CREATE INDEX idx_post_comments_post ON post_comments (post_id, hidden, created_at);
CREATE INDEX idx_post_likes_post ON post_likes (post_id);

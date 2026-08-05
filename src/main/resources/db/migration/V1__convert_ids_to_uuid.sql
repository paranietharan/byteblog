CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    email_verified BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    email_verified_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    used_at TIMESTAMP(6),
    token_type VARCHAR(50),
    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'id'
          AND data_type <> 'uuid'
    ) THEN
        ALTER TABLE users ADD COLUMN uuid_id UUID DEFAULT gen_random_uuid();
        ALTER TABLE email_verification_tokens ADD COLUMN uuid_id UUID DEFAULT gen_random_uuid();
        ALTER TABLE email_verification_tokens ADD COLUMN user_uuid UUID;
        ALTER TABLE refresh_tokens ADD COLUMN uuid_id UUID DEFAULT gen_random_uuid();
        ALTER TABLE refresh_tokens ADD COLUMN user_uuid UUID;

        UPDATE email_verification_tokens token
        SET user_uuid = users.uuid_id
        FROM users
        WHERE token.user_id = users.id;

        UPDATE refresh_tokens token
        SET user_uuid = users.uuid_id
        FROM users
        WHERE token.user_id = users.id;

        ALTER TABLE users ALTER COLUMN uuid_id SET NOT NULL;
        ALTER TABLE email_verification_tokens ALTER COLUMN uuid_id SET NOT NULL;
        ALTER TABLE email_verification_tokens ALTER COLUMN user_uuid SET NOT NULL;
        ALTER TABLE refresh_tokens ALTER COLUMN uuid_id SET NOT NULL;
        ALTER TABLE refresh_tokens ALTER COLUMN user_uuid SET NOT NULL;

        ALTER TABLE email_verification_tokens DROP CONSTRAINT IF EXISTS fk_email_verification_tokens_user;
        ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user;

        EXECUTE (
            SELECT string_agg(
                format('ALTER TABLE %I DROP CONSTRAINT %I', table_name, constraint_name),
                '; '
            )
            FROM information_schema.table_constraints
            WHERE table_schema = current_schema()
              AND table_name IN ('email_verification_tokens', 'refresh_tokens')
              AND constraint_type = 'FOREIGN KEY'
        );

        EXECUTE (
            SELECT string_agg(
                format('ALTER TABLE %I DROP CONSTRAINT %I', table_name, constraint_name),
                '; '
            )
            FROM information_schema.table_constraints
            WHERE table_schema = current_schema()
              AND table_name IN ('users', 'email_verification_tokens', 'refresh_tokens')
              AND constraint_type = 'PRIMARY KEY'
        );

        ALTER TABLE email_verification_tokens DROP COLUMN user_id;
        ALTER TABLE email_verification_tokens DROP COLUMN id;
        ALTER TABLE refresh_tokens DROP COLUMN user_id;
        ALTER TABLE refresh_tokens DROP COLUMN id;
        ALTER TABLE users DROP COLUMN id;

        ALTER TABLE users RENAME COLUMN uuid_id TO id;
        ALTER TABLE email_verification_tokens RENAME COLUMN uuid_id TO id;
        ALTER TABLE email_verification_tokens RENAME COLUMN user_uuid TO user_id;
        ALTER TABLE refresh_tokens RENAME COLUMN uuid_id TO id;
        ALTER TABLE refresh_tokens RENAME COLUMN user_uuid TO user_id;

        ALTER TABLE users ADD CONSTRAINT users_pkey PRIMARY KEY (id);
        ALTER TABLE email_verification_tokens
            ADD CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id);
        ALTER TABLE refresh_tokens ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);
        ALTER TABLE email_verification_tokens
            ADD CONSTRAINT fk_email_verification_tokens_user
            FOREIGN KEY (user_id) REFERENCES users (id);
        ALTER TABLE refresh_tokens
            ADD CONSTRAINT fk_refresh_tokens_user
            FOREIGN KEY (user_id) REFERENCES users (id);
    END IF;
END
$$;

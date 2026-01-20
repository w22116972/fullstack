-- Shared database schema
-- Users and Articles tables with proper foreign key relationship
-- Schema is managed by JPA (ddl-auto=update), this file provides indexes

-- Users table (created by auth-service JPA)
-- Admin account created on startup via ADMIN_EMAIL/ADMIN_PASSWORD env vars

-- Articles table indexes (after JPA creates the tables)
-- These will be created if tables exist, ignored otherwise
DO $$
BEGIN
    -- Create indexes if tables exist
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users') THEN
        CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'articles') THEN
        CREATE INDEX IF NOT EXISTS idx_articles_author ON articles(author_id);
        CREATE INDEX IF NOT EXISTS idx_articles_title ON articles(title);
    END IF;
END $$;

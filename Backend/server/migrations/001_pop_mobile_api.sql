CREATE TABLE IF NOT EXISTS pop_users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pop_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    access_token_hash CHAR(64) NOT NULL UNIQUE,
    refresh_token_hash CHAR(64) NOT NULL UNIQUE,
    access_expires_at TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS pop_sessions_user_idx ON pop_sessions(user_id);

CREATE TABLE IF NOT EXISTS pop_assessment_results (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    answers JSONB NOT NULL,
    scores JSONB NOT NULL,
    note TEXT NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS pop_assessment_user_date_idx
    ON pop_assessment_results(user_id, completed_at DESC);

CREATE TABLE IF NOT EXISTS pop_personal_plans (
    user_id UUID PRIMARY KEY REFERENCES pop_users(id) ON DELETE CASCADE,
    plan JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pop_focus_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    client_session_id TEXT NOT NULL,
    task_name TEXT NOT NULL,
    target_minutes INTEGER NOT NULL CHECK (target_minutes BETWEEN 1 AND 600),
    actual_minutes INTEGER NOT NULL CHECK (actual_minutes BETWEEN 0 AND 600),
    interruption_count INTEGER NOT NULL CHECK (interruption_count >= 0),
    interruption_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    self_rating INTEGER NOT NULL DEFAULT 0 CHECK (self_rating BETWEEN 0 AND 5),
    status TEXT NOT NULL CHECK (status IN ('NOT_STARTED', 'RUNNING', 'PAUSED', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, client_session_id)
);
CREATE INDEX IF NOT EXISTS pop_focus_user_date_idx
    ON pop_focus_sessions(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS pop_journals (
    id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    estimated_short_video_minutes INTEGER NOT NULL CHECK (estimated_short_video_minutes >= 0),
    focus_minutes INTEGER NOT NULL CHECK (focus_minutes >= 0),
    interruption_count INTEGER NOT NULL CHECK (interruption_count >= 0),
    distractors JSONB NOT NULL DEFAULT '[]'::jsonb,
    most_productive_session TEXT NOT NULL DEFAULT '',
    tomorrow_adjustment TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, entry_date),
    UNIQUE (id)
);
CREATE INDEX IF NOT EXISTS pop_journals_user_date_idx
    ON pop_journals(user_id, entry_date DESC);

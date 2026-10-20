CREATE TABLE IF NOT EXISTS pop_schools (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    name_key TEXT NOT NULL UNIQUE,
    created_by UUID REFERENCES pop_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pop_classes (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL REFERENCES pop_schools(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    name_key TEXT NOT NULL,
    teacher_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, name_key)
);
CREATE INDEX IF NOT EXISTS pop_classes_teacher_idx ON pop_classes(teacher_id);

CREATE TABLE IF NOT EXISTS pop_class_members (
    class_id UUID NOT NULL REFERENCES pop_classes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ,
    decided_by UUID REFERENCES pop_users(id) ON DELETE SET NULL,
    PRIMARY KEY (class_id, user_id)
);
-- Mỗi học sinh chỉ có tối đa một yêu cầu đang chờ hoặc một lớp đã được duyệt tại một thời điểm.
CREATE UNIQUE INDEX IF NOT EXISTS pop_class_members_one_active_idx
    ON pop_class_members(user_id) WHERE status IN ('PENDING', 'APPROVED');
CREATE INDEX IF NOT EXISTS pop_class_members_class_status_idx
    ON pop_class_members(class_id, status);

CREATE TABLE IF NOT EXISTS pop_notifications (
    id UUID PRIMARY KEY,
    class_id UUID NOT NULL REFERENCES pop_classes(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES pop_users(id) ON DELETE SET NULL,
    kind TEXT NOT NULL DEFAULT 'ANNOUNCEMENT' CHECK (kind IN ('ANNOUNCEMENT', 'SYSTEM')),
    title TEXT NOT NULL CHECK (char_length(title) BETWEEN 1 AND 80),
    body TEXT NOT NULL CHECK (char_length(body) BETWEEN 1 AND 500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS pop_notifications_class_idx
    ON pop_notifications(class_id, created_at DESC);

CREATE TABLE IF NOT EXISTS pop_notification_recipients (
    notification_id UUID NOT NULL REFERENCES pop_notifications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES pop_users(id) ON DELETE CASCADE,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    PRIMARY KEY (notification_id, user_id)
);
CREATE INDEX IF NOT EXISTS pop_notification_recipients_user_idx
    ON pop_notification_recipients(user_id);

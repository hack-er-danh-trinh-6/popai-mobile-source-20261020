import { randomUUID } from "node:crypto";
import { Router } from "express";
import rateLimit from "express-rate-limit";
import bcrypt from "bcryptjs";
import { pool, query } from "../db.js";
import { normalizeEmail } from "../domain.js";
import {
  cleanName, isUuid, issueGateToken, nameKey, parseDays, safeEqualText,
  validateNotification, validateTimeZone, verifyGateToken
} from "../classroomDomain.js";
import { createSession, requireAuth, requireTeacher, toUserDto } from "../security.js";

const router = Router();
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const gateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { message: "Nhập sai mật khẩu quá nhiều lần. Vui lòng chờ rồi thử lại." }
});
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { message: "Quá nhiều lần thử. Vui lòng chờ rồi thử lại." }
});
const sendLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  limit: 30,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  keyGenerator: (req) => req.user.id,
  message: { message: "Bạn đã gửi quá nhiều thông báo trong một giờ. Vui lòng thử lại sau." }
});

// ---------------------------------------------------------------------------
// Cổng mật khẩu admin
// ---------------------------------------------------------------------------
const gateOptions = () => ({ secret: process.env.TEACHER_GATE_SECRET });

function requireGate(req, res, next) {
  const adminPassword = process.env.TEACHER_ADMIN_PASSWORD;
  if (!adminPassword) {
    return res.status(503).json({ message: "Máy chủ chưa cấu hình mật khẩu admin (TEACHER_ADMIN_PASSWORD)." });
  }
  if (!verifyGateToken(req.get("x-gate-token"), adminPassword, gateOptions())) {
    return res.status(403).json({ message: "Phiên nhập mật khẩu admin đã hết hạn. Vui lòng nhập lại." });
  }
  return next();
}

router.post("/gate", gateLimiter, (req, res) => {
  const adminPassword = process.env.TEACHER_ADMIN_PASSWORD;
  if (!adminPassword) {
    return res.status(503).json({ message: "Máy chủ chưa cấu hình mật khẩu admin (TEACHER_ADMIN_PASSWORD)." });
  }
  const supplied = req.body?.password;
  if (typeof supplied !== "string" || supplied.length > 200 || !safeEqualText(supplied, adminPassword)) {
    return res.status(401).json({ message: "Mật khẩu admin không đúng." });
  }
  const { token, expiresAt } = issueGateToken(adminPassword, gateOptions());
  return res.json({ gateToken: token, expiresAt: new Date(expiresAt).toISOString() });
});

// ---------------------------------------------------------------------------
// Lớp học
// ---------------------------------------------------------------------------
const classDto = (row) => ({
  id: row.id,
  name: row.name,
  schoolId: row.school_id,
  schoolName: row.school_name,
  pendingCount: Number(row.pending_count ?? 0),
  studentCount: Number(row.student_count ?? 0)
});

/** Tìm hoặc tạo trường, rồi tạo lớp cho giáo viên. Trả { conflict } nếu lớp đã có chủ. */
async function declareClass(client, teacherId, schoolName, className) {
  const school = await client.query(
    `INSERT INTO pop_schools (id, name, name_key, created_by)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (name_key) DO UPDATE SET name_key = EXCLUDED.name_key
     RETURNING id, name`,
    [randomUUID(), schoolName, nameKey(schoolName), teacherId]
  );
  const schoolRow = school.rows[0];
  const existing = await client.query(
    "SELECT id, teacher_id FROM pop_classes WHERE school_id = $1 AND name_key = $2",
    [schoolRow.id, nameKey(className)]
  );
  if (existing.rowCount > 0) {
    return { conflict: existing.rows[0].teacher_id === teacherId ? "OWN" : "TAKEN" };
  }
  const inserted = await client.query(
    `INSERT INTO pop_classes (id, school_id, name, name_key, teacher_id)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING id, name, school_id`,
    [randomUUID(), schoolRow.id, className, nameKey(className), teacherId]
  );
  return {
    classRow: { ...inserted.rows[0], school_name: schoolRow.name, pending_count: 0, student_count: 0 }
  };
}

const CLASS_TAKEN_MESSAGE = "Lớp này đã có giáo viên khác quản lý.";

router.post("/auth/register", authLimiter, requireGate, async (req, res, next) => {
  const client = await pool.connect();
  try {
    const email = normalizeEmail(req.body?.email);
    const password = req.body?.password;
    const fullName = cleanName(req.body?.fullName, 100);
    const schoolName = cleanName(req.body?.school, 150);
    const className = cleanName(req.body?.className, 60);
    if (!emailPattern.test(email) || email.length > 254) {
      return res.status(400).json({ message: "Email không hợp lệ." });
    }
    if (typeof password !== "string" || password.length < 8 || password.length > 128) {
      return res.status(400).json({ message: "Mật khẩu phải có từ 8 đến 128 ký tự." });
    }
    if (!fullName) return res.status(400).json({ message: "Vui lòng nhập họ tên (tối đa 100 ký tự)." });
    if (!schoolName) return res.status(400).json({ message: "Vui lòng nhập tên trường (tối đa 150 ký tự)." });
    if (!className) return res.status(400).json({ message: "Vui lòng nhập tên lớp (tối đa 60 ký tự)." });

    const passwordHash = await bcrypt.hash(password, 12);
    const userId = randomUUID();
    await client.query("BEGIN");
    const inserted = await client.query(
      `INSERT INTO pop_users (id, email, full_name, password_hash, role)
       VALUES ($1, $2, $3, $4, 'TEACHER')
       RETURNING id, email, full_name, role`,
      [userId, email, fullName, passwordHash]
    );
    const declared = await declareClass(client, userId, schoolName, className);
    if (declared.conflict) {
      await client.query("ROLLBACK");
      return res.status(409).json({ message: CLASS_TAKEN_MESSAGE });
    }
    await client.query("COMMIT");
    const user = inserted.rows[0];
    const tokens = await createSession(user.id);
    return res.status(201).json({ ...tokens, user: toUserDto(user), classes: [classDto(declared.classRow)] });
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    if (error?.code === "23505") {
      const isEmail = String(error.constraint ?? "").includes("email");
      return res.status(409).json({ message: isEmail ? "Email này đã được đăng ký." : CLASS_TAKEN_MESSAGE });
    }
    return next(error);
  } finally {
    client.release();
  }
});

router.post("/auth/login", authLimiter, requireGate, async (req, res, next) => {
  try {
    const email = normalizeEmail(req.body?.email);
    const password = req.body?.password;
    const invalid = { message: "Email hoặc mật khẩu không đúng." };
    if (!emailPattern.test(email) || typeof password !== "string" || password.length > 128) {
      return res.status(401).json(invalid);
    }
    const found = await query(
      "SELECT id, email, full_name, password_hash, role FROM pop_users WHERE email = $1",
      [email]
    );
    if (found.rowCount !== 1 || !(await bcrypt.compare(password, found.rows[0].password_hash))) {
      return res.status(401).json(invalid);
    }
    const user = found.rows[0];
    if (user.role !== "TEACHER") {
      return res.status(403).json({ message: "Đây không phải tài khoản giáo viên. Học sinh vui lòng dùng ứng dụng POP AI." });
    }
    const tokens = await createSession(user.id);
    return res.json({ ...tokens, user: toUserDto(user) });
  } catch (error) {
    return next(error);
  }
});

const teacherOnly = [requireAuth, requireTeacher];

router.get("/auth/me", ...teacherOnly, (req, res) => res.json(toUserDto(req.user)));

router.post("/auth/logout", ...teacherOnly, async (req, res, next) => {
  try {
    await query("DELETE FROM pop_sessions WHERE access_token_hash = $1", [req.accessTokenHash]);
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

router.get("/classes", ...teacherOnly, async (req, res, next) => {
  try {
    const result = await query(
      `SELECT c.id, c.name, s.id AS school_id, s.name AS school_name,
              COUNT(m.user_id) FILTER (WHERE m.status = 'PENDING')::int AS pending_count,
              COUNT(m.user_id) FILTER (WHERE m.status = 'APPROVED')::int AS student_count
         FROM pop_classes c
         JOIN pop_schools s ON s.id = c.school_id
         LEFT JOIN pop_class_members m ON m.class_id = c.id
        WHERE c.teacher_id = $1
        GROUP BY c.id, s.id
        ORDER BY s.name, c.name`,
      [req.user.id]
    );
    return res.json(result.rows.map(classDto));
  } catch (error) {
    return next(error);
  }
});

router.post("/classes", ...teacherOnly, async (req, res, next) => {
  const client = await pool.connect();
  try {
    const schoolName = cleanName(req.body?.school, 150);
    const className = cleanName(req.body?.className, 60);
    if (!schoolName) return res.status(400).json({ message: "Vui lòng nhập tên trường (tối đa 150 ký tự)." });
    if (!className) return res.status(400).json({ message: "Vui lòng nhập tên lớp (tối đa 60 ký tự)." });
    await client.query("BEGIN");
    const declared = await declareClass(client, req.user.id, schoolName, className);
    if (declared.conflict) {
      await client.query("ROLLBACK");
      return res.status(409).json({
        message: declared.conflict === "OWN" ? "Bạn đã khai báo lớp này rồi." : CLASS_TAKEN_MESSAGE
      });
    }
    await client.query("COMMIT");
    return res.status(201).json(classDto(declared.classRow));
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    if (error?.code === "23505") return res.status(409).json({ message: CLASS_TAKEN_MESSAGE });
    return next(error);
  } finally {
    client.release();
  }
});

/** Trả lớp thuộc giáo viên đang đăng nhập; tự trả 404 nếu không phải lớp của họ. */
async function loadOwnedClass(req, res) {
  const classId = req.params.classId;
  if (!isUuid(classId)) {
    res.status(404).json({ message: "Không tìm thấy lớp." });
    return null;
  }
  const result = await query(
    `SELECT c.id, c.name, s.name AS school_name
       FROM pop_classes c JOIN pop_schools s ON s.id = c.school_id
      WHERE c.id = $1 AND c.teacher_id = $2`,
    [classId, req.user.id]
  );
  if (result.rowCount !== 1) {
    res.status(404).json({ message: "Không tìm thấy lớp." });
    return null;
  }
  return result.rows[0];
}

async function createNotification(client, { classId, senderId, kind, title, body, userIds }) {
  const id = randomUUID();
  await client.query(
    `INSERT INTO pop_notifications (id, class_id, sender_id, kind, title, body)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [id, classId, senderId, kind, title, body]
  );
  await client.query(
    `INSERT INTO pop_notification_recipients (notification_id, user_id)
     SELECT $1, unnest($2::uuid[])`,
    [id, userIds]
  );
  return id;
}

// ---------------------------------------------------------------------------
// Duyệt học sinh
// ---------------------------------------------------------------------------
router.get("/classes/:classId/requests", ...teacherOnly, async (req, res, next) => {
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    const result = await query(
      `SELECT u.id, u.full_name, u.email, m.requested_at
         FROM pop_class_members m JOIN pop_users u ON u.id = m.user_id
        WHERE m.class_id = $1 AND m.status = 'PENDING'
        ORDER BY m.requested_at`,
      [klass.id]
    );
    return res.json(result.rows.map((row) => ({
      userId: row.id,
      fullName: row.full_name,
      email: row.email,
      requestedAt: row.requested_at.toISOString()
    })));
  } catch (error) {
    return next(error);
  }
});

const DECISIONS = {
  approve: {
    status: "APPROVED",
    title: "Bạn đã được duyệt vào lớp",
    body: (klass) => `Giáo viên đã duyệt bạn vào lớp ${klass.name} (${klass.school_name}).`
  },
  reject: {
    status: "REJECTED",
    title: "Yêu cầu vào lớp chưa được duyệt",
    body: (klass) => `Giáo viên chưa duyệt yêu cầu vào lớp ${klass.name} (${klass.school_name}). Bạn có thể chọn lại lớp khác.`
  }
};

async function decide(req, res, next, decisionKey, onlyUserId) {
  const decision = DECISIONS[decisionKey];
  const client = await pool.connect();
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    if (onlyUserId !== null && !isUuid(onlyUserId)) {
      return res.status(404).json({ message: "Không tìm thấy yêu cầu." });
    }
    await client.query("BEGIN");
    const updated = await client.query(
      `UPDATE pop_class_members
          SET status = $3, decided_at = NOW(), decided_by = $4
        WHERE class_id = $1 AND status = 'PENDING' AND ($2::uuid IS NULL OR user_id = $2::uuid)
        RETURNING user_id`,
      [klass.id, onlyUserId, decision.status, req.user.id]
    );
    if (updated.rowCount === 0) {
      await client.query("ROLLBACK");
      return onlyUserId === null
        ? res.json({ updated: 0 })
        : res.status(404).json({ message: "Không còn yêu cầu đang chờ của học sinh này." });
    }
    await createNotification(client, {
      classId: klass.id,
      senderId: req.user.id,
      kind: "SYSTEM",
      title: decision.title,
      body: decision.body(klass),
      userIds: updated.rows.map((row) => row.user_id)
    });
    await client.query("COMMIT");
    return res.json({ updated: updated.rowCount });
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    return next(error);
  } finally {
    client.release();
  }
}

router.post("/classes/:classId/requests/approve-all", ...teacherOnly,
  (req, res, next) => decide(req, res, next, "approve", null));
router.post("/classes/:classId/requests/:userId/approve", ...teacherOnly,
  (req, res, next) => decide(req, res, next, "approve", req.params.userId));
router.post("/classes/:classId/requests/:userId/reject", ...teacherOnly,
  (req, res, next) => decide(req, res, next, "reject", req.params.userId));

// ---------------------------------------------------------------------------
// Học sinh trong lớp và dữ liệu thói quen tập trung
// Chỉ chia sẻ phiên tập trung từ lúc học sinh được duyệt vào lớp; không chia sẻ nhật ký, đánh giá, trò chuyện AI.
// ---------------------------------------------------------------------------
router.get("/classes/:classId/students", ...teacherOnly, async (req, res, next) => {
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    const days = parseDays(req.query.days);
    if (days === null) return res.status(400).json({ message: "days phải nằm trong khoảng từ 1 đến 90." });
    const timeZone = validateTimeZone(req.query.timeZone);
    if (timeZone === null) return res.status(400).json({ message: "timeZone không hợp lệ." });
    const result = await query(
      `SELECT u.id, u.full_name, u.email, m.decided_at AS joined_at,
              COALESCE(SUM(f.actual_minutes) FILTER (WHERE f.status = 'COMPLETED'), 0)::int AS focus_minutes,
              COUNT(f.id) FILTER (WHERE f.status = 'COMPLETED')::int AS completed_sessions,
              COUNT(f.id)::int AS total_sessions,
              COALESCE(SUM(f.interruption_count), 0)::int AS interruptions,
              COALESCE(AVG(f.self_rating) FILTER (WHERE f.self_rating > 0), 0)::numeric AS avg_rating,
              COUNT(DISTINCT (f.created_at AT TIME ZONE $3)::date)::int AS active_days,
              MAX(f.created_at) AS last_activity
         FROM pop_class_members m
         JOIN pop_users u ON u.id = m.user_id
         LEFT JOIN pop_focus_sessions f
                ON f.user_id = u.id
               AND f.created_at >= NOW() - make_interval(days => $2::int)
               AND f.created_at >= m.decided_at
        WHERE m.class_id = $1 AND m.status = 'APPROVED'
        GROUP BY u.id, m.decided_at
        ORDER BY u.full_name`,
      [klass.id, days, timeZone]
    );
    const students = result.rows.map((row) => ({
      userId: row.id,
      fullName: row.full_name,
      email: row.email,
      joinedAt: row.joined_at.toISOString(),
      focusMinutes: row.focus_minutes,
      completedSessions: row.completed_sessions,
      totalSessions: row.total_sessions,
      interruptions: row.interruptions,
      avgRating: Number(Number(row.avg_rating).toFixed(1)),
      activeDays: row.active_days,
      lastActivity: row.last_activity ? row.last_activity.toISOString() : null
    }));
    const totals = students.reduce((sum, s) => ({
      focusMinutes: sum.focusMinutes + s.focusMinutes,
      completedSessions: sum.completedSessions + s.completedSessions,
      interruptions: sum.interruptions + s.interruptions,
      activeStudents: sum.activeStudents + (s.totalSessions > 0 ? 1 : 0)
    }), { focusMinutes: 0, completedSessions: 0, interruptions: 0, activeStudents: 0 });
    return res.json({ days, class: { id: klass.id, name: klass.name, schoolName: klass.school_name }, totals, students });
  } catch (error) {
    return next(error);
  }
});

router.get("/classes/:classId/students/:userId/focus", ...teacherOnly, async (req, res, next) => {
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    if (!isUuid(req.params.userId)) return res.status(404).json({ message: "Không tìm thấy học sinh trong lớp." });
    const days = parseDays(req.query.days, 30);
    if (days === null) return res.status(400).json({ message: "days phải nằm trong khoảng từ 1 đến 90." });
    const timeZone = validateTimeZone(req.query.timeZone);
    if (timeZone === null) return res.status(400).json({ message: "timeZone không hợp lệ." });
    const member = await query(
      `SELECT u.id, u.full_name, u.email, m.decided_at
         FROM pop_class_members m JOIN pop_users u ON u.id = m.user_id
        WHERE m.class_id = $1 AND m.user_id = $2 AND m.status = 'APPROVED'`,
      [klass.id, req.params.userId]
    );
    if (member.rowCount !== 1) return res.status(404).json({ message: "Không tìm thấy học sinh trong lớp." });
    const student = member.rows[0];
    const [sessions, daily] = await Promise.all([
      query(
        `SELECT id, task_name, target_minutes, actual_minutes, interruption_count, self_rating, status, created_at
           FROM pop_focus_sessions
          WHERE user_id = $1 AND created_at >= GREATEST(NOW() - make_interval(days => $2::int), $3::timestamptz)
          ORDER BY created_at DESC LIMIT 200`,
        [student.id, days, student.decided_at]
      ),
      query(
        `SELECT (created_at AT TIME ZONE $4)::date::text AS day,
                COALESCE(SUM(actual_minutes) FILTER (WHERE status = 'COMPLETED'), 0)::int AS focus_minutes,
                COUNT(*) FILTER (WHERE status = 'COMPLETED')::int AS completed_sessions,
                COALESCE(SUM(interruption_count), 0)::int AS interruptions
           FROM pop_focus_sessions
          WHERE user_id = $1 AND created_at >= GREATEST(NOW() - make_interval(days => $2::int), $3::timestamptz)
          GROUP BY 1 ORDER BY 1`,
        [student.id, days, student.decided_at, timeZone]
      )
    ]);
    return res.json({
      student: { userId: student.id, fullName: student.full_name, email: student.email, joinedAt: student.decided_at.toISOString() },
      days,
      daily: daily.rows.map((row) => ({
        date: row.day,
        focusMinutes: row.focus_minutes,
        completedSessions: row.completed_sessions,
        interruptions: row.interruptions
      })),
      sessions: sessions.rows.map((row) => ({
        id: row.id,
        taskName: row.task_name,
        targetMinutes: row.target_minutes,
        actualMinutes: row.actual_minutes,
        interruptionCount: row.interruption_count,
        selfRating: row.self_rating,
        status: row.status,
        createdAt: row.created_at.toISOString()
      }))
    });
  } catch (error) {
    return next(error);
  }
});

router.delete("/classes/:classId/students/:userId", ...teacherOnly, async (req, res, next) => {
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    if (!isUuid(req.params.userId)) return res.status(404).json({ message: "Không tìm thấy học sinh trong lớp." });
    const removed = await query(
      "DELETE FROM pop_class_members WHERE class_id = $1 AND user_id = $2 AND status = 'APPROVED'",
      [klass.id, req.params.userId]
    );
    if (removed.rowCount === 0) return res.status(404).json({ message: "Không tìm thấy học sinh trong lớp." });
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

// ---------------------------------------------------------------------------
// Thông báo gửi tới điện thoại học sinh
// ---------------------------------------------------------------------------
router.post("/classes/:classId/notifications", ...teacherOnly, sendLimiter, async (req, res, next) => {
  const client = await pool.connect();
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    const parsed = validateNotification(req.body);
    if (parsed.error) return res.status(400).json({ message: parsed.error });

    await client.query("BEGIN");
    const approved = await client.query(
      "SELECT user_id FROM pop_class_members WHERE class_id = $1 AND status = 'APPROVED'",
      [klass.id]
    );
    let recipients = approved.rows.map((row) => row.user_id);
    if (parsed.studentIds) {
      const allowed = new Set(recipients);
      if (!parsed.studentIds.every((id) => allowed.has(id))) {
        await client.query("ROLLBACK");
        return res.status(400).json({ message: "Có học sinh không thuộc lớp này." });
      }
      recipients = parsed.studentIds;
    }
    if (recipients.length === 0) {
      await client.query("ROLLBACK");
      return res.status(400).json({ message: "Lớp chưa có học sinh nào được duyệt để nhận thông báo." });
    }
    const id = await createNotification(client, {
      classId: klass.id,
      senderId: req.user.id,
      kind: "ANNOUNCEMENT",
      title: parsed.title,
      body: parsed.body,
      userIds: recipients
    });
    await client.query("COMMIT");
    return res.status(201).json({ id, recipientCount: recipients.length });
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    return next(error);
  } finally {
    client.release();
  }
});

router.get("/classes/:classId/notifications", ...teacherOnly, async (req, res, next) => {
  try {
    const klass = await loadOwnedClass(req, res);
    if (!klass) return;
    const result = await query(
      `SELECT n.id, n.title, n.body, n.created_at,
              COUNT(r.user_id)::int AS recipient_count,
              COUNT(r.read_at)::int AS read_count
         FROM pop_notifications n
         LEFT JOIN pop_notification_recipients r ON r.notification_id = n.id
        WHERE n.class_id = $1 AND n.kind = 'ANNOUNCEMENT'
        GROUP BY n.id
        ORDER BY n.created_at DESC
        LIMIT 50`,
      [klass.id]
    );
    return res.json(result.rows.map((row) => ({
      id: row.id,
      title: row.title,
      body: row.body,
      createdAt: row.created_at.toISOString(),
      recipientCount: row.recipient_count,
      readCount: row.read_count
    })));
  } catch (error) {
    return next(error);
  }
});

export default router;

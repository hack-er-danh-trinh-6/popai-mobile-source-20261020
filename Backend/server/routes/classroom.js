import { Router } from "express";
import rateLimit from "express-rate-limit";
import { query } from "../db.js";
import { isUuid, nameKey } from "../classroomDomain.js";
import { requireAuth } from "../security.js";

// Router này được gắn vào /api/mobile/v1 trước popRoutes, nên mỗi route tự yêu cầu đăng nhập.
const router = Router();

const joinLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 15,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  keyGenerator: (req) => req.user.id,
  message: { message: "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau." }
});

function requireStudent(req, res, next) {
  if (req.user.role !== "STUDENT") {
    return res.status(403).json({ message: "Chức năng này dành cho tài khoản học sinh." });
  }
  return next();
}

const studentOnly = [requireAuth, requireStudent];

router.get("/schools", ...studentOnly, async (req, res, next) => {
  try {
    const keyword = nameKey(typeof req.query.q === "string" ? req.query.q.slice(0, 100) : "");
    const result = await query(
      `SELECT s.id, s.name, COUNT(c.id)::int AS class_count
         FROM pop_schools s JOIN pop_classes c ON c.school_id = s.id
        WHERE $1 = '' OR position($1 in s.name_key) > 0
        GROUP BY s.id
        ORDER BY s.name
        LIMIT 50`,
      [keyword]
    );
    return res.json(result.rows.map((row) => ({ id: row.id, name: row.name, classCount: row.class_count })));
  } catch (error) {
    return next(error);
  }
});

router.get("/schools/:schoolId/classes", ...studentOnly, async (req, res, next) => {
  try {
    if (!isUuid(req.params.schoolId)) return res.json([]);
    const result = await query(
      `SELECT c.id, c.name, t.full_name AS teacher_name
         FROM pop_classes c JOIN pop_users t ON t.id = c.teacher_id
        WHERE c.school_id = $1
        ORDER BY c.name`,
      [req.params.schoolId]
    );
    return res.json(result.rows.map((row) => ({ id: row.id, name: row.name, teacherName: row.teacher_name })));
  } catch (error) {
    return next(error);
  }
});

const membershipDto = (row) => ({
  status: row.status,
  classId: row.class_id,
  className: row.class_name,
  schoolName: row.school_name,
  teacherName: row.teacher_name,
  requestedAt: row.requested_at.toISOString(),
  decidedAt: row.decided_at ? row.decided_at.toISOString() : null
});

async function loadMembership(userId) {
  // Ưu tiên yêu cầu đang chờ/đã duyệt; nếu không có thì trả kết quả mới nhất (ví dụ bị từ chối).
  const result = await query(
    `SELECT m.status, m.class_id, c.name AS class_name, s.name AS school_name,
            t.full_name AS teacher_name, m.requested_at, m.decided_at
       FROM pop_class_members m
       JOIN pop_classes c ON c.id = m.class_id
       JOIN pop_schools s ON s.id = c.school_id
       JOIN pop_users t ON t.id = c.teacher_id
      WHERE m.user_id = $1
      ORDER BY (m.status IN ('PENDING', 'APPROVED')) DESC, COALESCE(m.decided_at, m.requested_at) DESC
      LIMIT 1`,
    [userId]
  );
  return result.rowCount === 1 ? membershipDto(result.rows[0]) : { status: "NONE" };
}

router.get("/class/membership", ...studentOnly, async (req, res, next) => {
  try {
    return res.json(await loadMembership(req.user.id));
  } catch (error) {
    return next(error);
  }
});

router.post("/class/join-request", ...studentOnly, joinLimiter, async (req, res, next) => {
  try {
    const classId = req.body?.classId;
    if (!isUuid(classId)) return res.status(400).json({ message: "Lớp không hợp lệ." });
    const found = await query("SELECT 1 FROM pop_classes WHERE id = $1", [classId]);
    if (found.rowCount === 0) return res.status(404).json({ message: "Không tìm thấy lớp." });
    const inserted = await query(
      `INSERT INTO pop_class_members (class_id, user_id, status)
       VALUES ($1, $2, 'PENDING')
       ON CONFLICT (class_id, user_id) DO UPDATE
         SET status = 'PENDING', requested_at = NOW(), decided_at = NULL, decided_by = NULL
         WHERE pop_class_members.status = 'REJECTED'
       RETURNING class_id`,
      [classId, req.user.id]
    );
    if (inserted.rowCount === 0) {
      return res.status(409).json({ message: "Bạn đã gửi yêu cầu hoặc đã ở trong lớp này." });
    }
    return res.status(201).json(await loadMembership(req.user.id));
  } catch (error) {
    if (error?.code === "23505") {
      return res.status(409).json({ message: "Bạn đang có yêu cầu hoặc đã ở trong một lớp khác. Hãy rời lớp đó trước." });
    }
    return next(error);
  }
});

// Huỷ yêu cầu đang chờ, rời lớp đã tham gia, hoặc xoá thông báo bị từ chối.
router.delete("/class/membership", ...studentOnly, async (req, res, next) => {
  try {
    await query("DELETE FROM pop_class_members WHERE user_id = $1", [req.user.id]);
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

// ---------------------------------------------------------------------------
// Hộp thư thông báo của học sinh
// ---------------------------------------------------------------------------
const notificationDto = (row) => ({
  id: row.id,
  title: row.title,
  body: row.body,
  className: row.class_name,
  senderName: row.sender_name,
  kind: row.kind,
  createdAt: row.created_at.toISOString(),
  read: row.read_at !== null
});

const NOTIFICATION_SELECT = `
  SELECT n.id, n.title, n.body, n.kind, n.created_at, c.name AS class_name,
         sender.full_name AS sender_name, r.read_at
    FROM pop_notification_recipients r
    JOIN pop_notifications n ON n.id = r.notification_id
    JOIN pop_classes c ON c.id = n.class_id
    LEFT JOIN pop_users sender ON sender.id = n.sender_id`;

router.get("/notifications", ...studentOnly, async (req, res, next) => {
  try {
    const [items, unread] = await Promise.all([
      query(
        `${NOTIFICATION_SELECT}
          WHERE r.user_id = $1
          ORDER BY n.created_at DESC
          LIMIT 50`,
        [req.user.id]
      ),
      query(
        "SELECT COUNT(*)::int AS unread FROM pop_notification_recipients WHERE user_id = $1 AND read_at IS NULL",
        [req.user.id]
      )
    ]);
    return res.json({ unreadCount: unread.rows[0].unread, items: items.rows.map(notificationDto) });
  } catch (error) {
    return next(error);
  }
});

// App gọi định kỳ: trả các thông báo chưa từng được giao tới thiết bị (và đánh dấu đã giao) để hiện thông báo điện thoại.
router.post("/notifications/deliver", ...studentOnly, async (req, res, next) => {
  try {
    const result = await query(
      `WITH claimed AS (
         UPDATE pop_notification_recipients
            SET delivered_at = NOW()
          WHERE user_id = $1 AND delivered_at IS NULL AND read_at IS NULL
            AND notification_id IN (
              SELECT notification_id FROM pop_notification_recipients r2
                JOIN pop_notifications n2 ON n2.id = r2.notification_id
               WHERE r2.user_id = $1 AND r2.delivered_at IS NULL AND r2.read_at IS NULL
               ORDER BY n2.created_at DESC LIMIT 10
            )
        RETURNING notification_id
       )
       SELECT n.id, n.title, n.body, n.kind, n.created_at, c.name AS class_name,
              sender.full_name AS sender_name, NULL::timestamptz AS read_at
         FROM claimed
         JOIN pop_notifications n ON n.id = claimed.notification_id
         JOIN pop_classes c ON c.id = n.class_id
         LEFT JOIN pop_users sender ON sender.id = n.sender_id
        ORDER BY n.created_at`,
      [req.user.id]
    );
    return res.json(result.rows.map(notificationDto));
  } catch (error) {
    return next(error);
  }
});

router.post("/notifications/read", ...studentOnly, async (req, res, next) => {
  try {
    const ids = req.body?.ids;
    if (req.body?.all === true) {
      await query(
        "UPDATE pop_notification_recipients SET read_at = NOW() WHERE user_id = $1 AND read_at IS NULL",
        [req.user.id]
      );
      return res.status(204).end();
    }
    if (!Array.isArray(ids) || ids.length === 0 || ids.length > 100 || !ids.every(isUuid)) {
      return res.status(400).json({ message: "Danh sách thông báo không hợp lệ." });
    }
    await query(
      `UPDATE pop_notification_recipients SET read_at = NOW()
        WHERE user_id = $1 AND read_at IS NULL AND notification_id = ANY($2::uuid[])`,
      [req.user.id, ids]
    );
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

export default router;

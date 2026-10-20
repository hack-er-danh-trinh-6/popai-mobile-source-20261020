import { randomUUID } from "node:crypto";
import { Router } from "express";
import rateLimit from "express-rate-limit";
import { query } from "../db.js";
import { formatJournalDate, parseJournalDate, scoreAssessment } from "../domain.js";
import { buildReflectionContext, requestAdvice } from "../ai.js";
import { loadActivityContext, loadJournalForDate, resolveTimeZone } from "../aiContext.js";
import { requireAuth } from "../security.js";

const router = Router();
router.use(requireAuth);
const aiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  // requireAuth đã chạy trước: giới hạn theo tài khoản thay vì IP (nhiều người dùng có thể chung một IP mạng).
  keyGenerator: (req) => req.user.id,
  message: { message: "Bạn đã gửi nhiều yêu cầu POP-AI. Vui lòng thử lại sau." }
});

const validStringArray = (value, maxItems = 20, maxLength = 200) =>
  Array.isArray(value) && value.length <= maxItems &&
  value.every((entry) => typeof entry === "string" && entry.trim().length <= maxLength);

router.post("/assessment/submit", async (req, res, next) => {
  try {
    let scores;
    try {
      scores = scoreAssessment(req.body?.answers);
    } catch (error) {
      return res.status(400).json({ message: error.message });
    }
    const id = randomUUID();
    const answers = req.body.answers.map(({ questionId, score }) => ({ questionId, score }));
    const note = "Chỉ để tự theo dõi, không phải chẩn đoán";
    const inserted = await query(
      `INSERT INTO pop_assessment_results (id, user_id, answers, scores, note)
       VALUES ($1, $2, $3::jsonb, $4::jsonb, $5)
       RETURNING id, completed_at, scores, note`,
      [id, req.user.id, JSON.stringify(answers), JSON.stringify(scores), note]
    );
    const row = inserted.rows[0];
    return res.status(201).json({
      id: row.id,
      completedAt: row.completed_at.toISOString(),
      scores: row.scores,
      note: row.note
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/assessment/history", async (req, res, next) => {
  try {
    const result = await query(
      `SELECT id, completed_at, scores, note
         FROM pop_assessment_results
        WHERE user_id = $1
        ORDER BY completed_at DESC
        LIMIT 100`,
      [req.user.id]
    );
    return res.json(result.rows.map((row) => ({
      id: row.id,
      completedAt: row.completed_at.toISOString(),
      scores: row.scores,
      note: row.note
    })));
  } catch (error) {
    return next(error);
  }
});

router.get("/plan", async (req, res, next) => {
  try {
    const result = await query("SELECT plan FROM pop_personal_plans WHERE user_id = $1", [req.user.id]);
    if (!result.rowCount) return res.status(404).json({ message: "Bạn chưa lưu kế hoạch cá nhân." });
    return res.json(result.rows[0].plan);
  } catch (error) {
    return next(error);
  }
});

router.post("/plan", async (req, res, next) => {
  try {
    const plan = req.body ?? {};
    const allowedDurations = [10, 15, 25, 30, 45];
    if (
      !Number.isInteger(plan.dailyShortVideoLimitMinutes) || plan.dailyShortVideoLimitMinutes < 0 || plan.dailyShortVideoLimitMinutes > 1440 ||
      !validStringArray(plan.trackedPlatforms, 10, 60) ||
      typeof plan.focusProtectionHours !== "string" || plan.focusProtectionHours.length > 100 ||
      !validStringArray(plan.studyTasks, 30, 200) ||
      !allowedDurations.includes(plan.sessionDurationMinutes) ||
      ![7, 30].includes(plan.targetDays)
    ) return res.status(400).json({ message: "Kế hoạch cá nhân không hợp lệ." });

    const normalized = {
      dailyShortVideoLimitMinutes: plan.dailyShortVideoLimitMinutes,
      trackedPlatforms: plan.trackedPlatforms.map((value) => value.trim()),
      focusProtectionHours: plan.focusProtectionHours.trim(),
      studyTasks: plan.studyTasks.map((value) => value.trim()).filter(Boolean),
      sessionDurationMinutes: plan.sessionDurationMinutes,
      targetDays: plan.targetDays
    };
    await query(
      `INSERT INTO pop_personal_plans (user_id, plan, updated_at)
       VALUES ($1, $2::jsonb, NOW())
       ON CONFLICT (user_id) DO UPDATE SET plan = EXCLUDED.plan, updated_at = NOW()`,
      [req.user.id, JSON.stringify(normalized)]
    );
    return res.json(normalized);
  } catch (error) {
    return next(error);
  }
});

router.post("/focus/session", async (req, res, next) => {
  try {
    const session = req.body ?? {};
    const clientSessionId = typeof session.clientSessionId === "string" && session.clientSessionId.length <= 120
      ? session.clientSessionId
      : randomUUID();
    const statuses = ["NOT_STARTED", "RUNNING", "PAUSED", "COMPLETED"];
    if (
      typeof session.taskName !== "string" || session.taskName.trim().length > 200 ||
      !Number.isInteger(session.targetMinutes) || session.targetMinutes < 1 || session.targetMinutes > 600 ||
      !Number.isInteger(session.actualMinutes) || session.actualMinutes < 0 || session.actualMinutes > 600 ||
      !Number.isInteger(session.interruptionCount) || session.interruptionCount < 0 || session.interruptionCount > 10000 ||
      !validStringArray(session.interruptionReasons, 100, 200) ||
      !Number.isInteger(session.selfRating) || session.selfRating < 0 || session.selfRating > 5 ||
      !statuses.includes(session.status)
    ) return res.status(400).json({ message: "Phiên tập trung không hợp lệ." });

    await query(
      `INSERT INTO pop_focus_sessions
        (id, user_id, client_session_id, task_name, target_minutes, actual_minutes,
         interruption_count, interruption_reasons, self_rating, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8::jsonb, $9, $10)
       ON CONFLICT (user_id, client_session_id) DO UPDATE SET
         task_name = EXCLUDED.task_name,
         target_minutes = EXCLUDED.target_minutes,
         actual_minutes = EXCLUDED.actual_minutes,
         interruption_count = EXCLUDED.interruption_count,
         interruption_reasons = EXCLUDED.interruption_reasons,
         self_rating = EXCLUDED.self_rating,
         status = EXCLUDED.status`,
      [randomUUID(), req.user.id, clientSessionId, session.taskName.trim(), session.targetMinutes,
        session.actualMinutes, session.interruptionCount, JSON.stringify(session.interruptionReasons),
        session.selfRating, session.status]
    );
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

router.post("/journal", async (req, res, next) => {
  try {
    const journal = req.body ?? {};
    const entryDate = parseJournalDate(journal.date);
    if (!entryDate) return res.status(400).json({ message: "Ngày nhật ký không hợp lệ." });
    if (
      !Number.isInteger(journal.estimatedShortVideoMinutes) || journal.estimatedShortVideoMinutes < 0 || journal.estimatedShortVideoMinutes > 1440 ||
      !Number.isInteger(journal.focusMinutes) || journal.focusMinutes < 0 || journal.focusMinutes > 1440 ||
      !Number.isInteger(journal.interruptionCount) || journal.interruptionCount < 0 || journal.interruptionCount > 10000 ||
      !validStringArray(journal.distractors, 30, 100) ||
      typeof journal.mostProductiveSession !== "string" || journal.mostProductiveSession.length > 200 ||
      typeof journal.tomorrowAdjustment !== "string" || journal.tomorrowAdjustment.length > 1000
    ) return res.status(400).json({ message: "Nội dung nhật ký không hợp lệ." });

    await query(
      `INSERT INTO pop_journals
        (id, user_id, entry_date, estimated_short_video_minutes, focus_minutes, interruption_count,
         distractors, most_productive_session, tomorrow_adjustment, updated_at)
       VALUES ($1, $2, $3::date, $4, $5, $6, $7::jsonb, $8, $9, NOW())
       ON CONFLICT (user_id, entry_date) DO UPDATE SET
         estimated_short_video_minutes = EXCLUDED.estimated_short_video_minutes,
         focus_minutes = EXCLUDED.focus_minutes,
         interruption_count = EXCLUDED.interruption_count,
         distractors = EXCLUDED.distractors,
         most_productive_session = EXCLUDED.most_productive_session,
         tomorrow_adjustment = EXCLUDED.tomorrow_adjustment,
         updated_at = NOW()`,
      [randomUUID(), req.user.id, entryDate, journal.estimatedShortVideoMinutes, journal.focusMinutes,
        journal.interruptionCount, JSON.stringify(journal.distractors.map((value) => value.trim())),
        journal.mostProductiveSession.trim(), journal.tomorrowAdjustment.trim()]
    );
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

router.get("/journal/today", async (req, res, next) => {
  try {
    const dateText = typeof req.query.date === "string"
      ? req.query.date
      : new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date());
    const entryDate = parseJournalDate(dateText);
    if (!entryDate) return res.status(400).json({ message: "Ngày nhật ký không hợp lệ." });
    const result = await query(
      `SELECT id, entry_date, estimated_short_video_minutes, focus_minutes, interruption_count,
              distractors, most_productive_session, tomorrow_adjustment
         FROM pop_journals WHERE user_id = $1 AND entry_date = $2::date`,
      [req.user.id, entryDate]
    );
    if (result.rowCount === 0) return res.status(404).json({ message: "Chưa có nhật ký cho ngày này." });
    const row = result.rows[0];
    const storedDate = typeof row.entry_date === "string" ? row.entry_date : row.entry_date.toISOString();
    return res.json({
      id: row.id,
      date: formatJournalDate(storedDate),
      estimatedShortVideoMinutes: row.estimated_short_video_minutes,
      focusMinutes: row.focus_minutes,
      interruptionCount: row.interruption_count,
      distractors: row.distractors,
      mostProductiveSession: row.most_productive_session,
      tomorrowAdjustment: row.tomorrow_adjustment
    });
  } catch (error) {
    return next(error);
  }
});

router.get("/progress/summary", async (req, res, next) => {
  try {
    const days = Number(req.query.days ?? 7);
    if (!Number.isInteger(days) || days < 1 || days > 90) {
      return res.status(400).json({ message: "days phải nằm trong khoảng từ 1 đến 90." });
    }
    const defaultEndDate = new Intl.DateTimeFormat("en-GB", {
      timeZone: "UTC", day: "2-digit", month: "2-digit", year: "numeric"
    }).format(new Date());
    const endDate = parseJournalDate(typeof req.query.endDate === "string" ? req.query.endDate : defaultEndDate);
    if (!endDate) return res.status(400).json({ message: "endDate phải theo định dạng dd/MM/yyyy." });
    const timeZone = typeof req.query.timeZone === "string" ? req.query.timeZone : "UTC";
    try {
      new Intl.DateTimeFormat("en-US", { timeZone }).format(new Date());
    } catch {
      return res.status(400).json({ message: "timeZone không hợp lệ." });
    }
    const [journalResult, focusResult, planResult, assessmentResult, activityResult] = await Promise.all([
      query(
        `SELECT COALESCE(ROUND(AVG(estimated_short_video_minutes)), 0)::int AS avg_video
           FROM pop_journals
          WHERE user_id = $1 AND entry_date BETWEEN ($3::date - ($2::int - 1)) AND $3::date`,
        [req.user.id, days, endDate]
      ),
      query(
        `SELECT COALESCE(SUM(actual_minutes) FILTER (WHERE status = 'COMPLETED'), 0)::int AS focus_minutes,
                COUNT(*) FILTER (WHERE status = 'COMPLETED')::int AS completed_count,
                COALESCE(SUM(interruption_count), 0)::int AS interruption_count,
                COALESCE(AVG(self_rating) FILTER (WHERE self_rating > 0), 0)::numeric AS avg_rating
           FROM pop_focus_sessions
          WHERE user_id = $1
            AND (created_at AT TIME ZONE $4)::date BETWEEN ($3::date - ($2::int - 1)) AND $3::date`,
        [req.user.id, days, endDate, timeZone]
      ),
      query("SELECT plan FROM pop_personal_plans WHERE user_id = $1", [req.user.id]),
      query(
        `SELECT scores FROM pop_assessment_results
          WHERE user_id = $1
            AND (completed_at AT TIME ZONE $4)::date BETWEEN ($3::date - ($2::int - 1)) AND $3::date
          ORDER BY completed_at DESC LIMIT 1`,
        [req.user.id, days, endDate, timeZone]
      ),
      query(
        `SELECT activity_date::text FROM (
           SELECT entry_date AS activity_date FROM pop_journals WHERE user_id = $1
           UNION
           SELECT (created_at AT TIME ZONE $4)::date AS activity_date FROM pop_focus_sessions
            WHERE user_id = $1 AND status = 'COMPLETED'
         ) activity
         WHERE activity_date BETWEEN ($3::date - ($2::int - 1)) AND $3::date
         ORDER BY activity_date DESC`,
        [req.user.id, days, endDate, timeZone]
      )
    ]);

    const avgVideo = journalResult.rows[0].avg_video;
    const focus = focusResult.rows[0];
    const hasSavedPlan = planResult.rowCount > 0;
    const targetLimit = hasSavedPlan ? Number(planResult.rows[0].plan?.dailyShortVideoLimitMinutes ?? 0) : 0;
    const dates = activityResult.rows.map((row) => row.activity_date);
    const today = endDate;
    const yesterday = new Date(Date.parse(`${endDate}T00:00:00Z`) - 86400000).toISOString().slice(0, 10);
    let streak = 0;
    if (dates.length && (dates[0] === today || dates[0] === yesterday)) {
      let expected = dates[0];
      for (const date of dates) {
        if (date !== expected) break;
        streak += 1;
        expected = new Date(Date.parse(`${expected}T00:00:00Z`) - 86400000).toISOString().slice(0, 10);
      }
    }

    const highlights = [];
    if (focus.completed_count > 0) highlights.push(`Bạn đã hoàn thành ${focus.completed_count} phiên tập trung trong ${days} ngày.`);
    if (assessmentResult.rowCount) {
      const scores = assessmentResult.rows[0].scores;
      highlights.push(`Đánh giá gần nhất: VN ${scores.VN}/5, PT ${scores.PT}/5, PB ${scores.PB}/5, TT ${scores.TT}/5.`);
    }
    if (streak > 0) highlights.push(`Chuỗi hoạt động hiện tại: ${streak} ngày.`);

    const areasToAdjust = [];
    if (hasSavedPlan && Number(avgVideo) > targetLimit) {
      areasToAdjust.push(`Mức dùng video ngắn trung bình đang cao hơn giới hạn kế hoạch ${Number(avgVideo) - targetLimit} phút/ngày.`);
    }
    if (Number(focus.avg_rating) > 0 && Number(focus.avg_rating) < 3) {
      areasToAdjust.push("Thử rút ngắn phiên tập trung để nhịp làm việc phù hợp hơn.");
    }
    if (areasToAdjust.length === 0 && focus.completed_count === 0 && Number(avgVideo) === 0 && !assessmentResult.rowCount) {
      areasToAdjust.push("Chưa có đủ hoạt động trong giai đoạn này để đưa ra gợi ý cá nhân hóa.");
    }

    return res.json({
      daysRange: days,
      avgShortVideoMinutes: Number(avgVideo),
      totalFocusMinutes: focus.focus_minutes,
      completedSessionsCount: focus.completed_count,
      totalInterruptions: focus.interruption_count,
      avgFocusRating: Number(Number(focus.avg_rating).toFixed(1)),
      targetVideoLimitMinutes: targetLimit,
      hasSavedPlan,
      streaksDays: streak,
      highlights,
      areasToAdjust
    });
  } catch (error) {
    return next(error);
  }
});

router.post("/ai/advice", aiLimiter, async (req, res, next) => {
  try {
    const prompt = req.body?.prompt;
    if (typeof prompt !== "string" || !prompt.trim() || prompt.length > 2000) {
      return res.status(400).json({ message: "Vui lòng nhập câu hỏi (tối đa 2000 ký tự)." });
    }
    const timeZone = resolveTimeZone(req.body?.timeZone);

    // Nhật ký chỉ được dùng khi người dùng chủ động đồng ý.
    let reflection = "";
    let reflectionUsed = false;
    const reflectionDate = parseJournalDate(req.body?.reflectionDate);
    if (req.body?.includeReflection === true && reflectionDate) {
      const journal = await loadJournalForDate(req.user.id, reflectionDate);
      if (journal) {
        reflection = buildReflectionContext(journal);
        reflectionUsed = reflection.length > 0;
      }
    }

    // Số liệu tổng hợp 7 ngày để POP-AI tư vấn dựa trên dữ liệu thật. Lỗi nạp dữ liệu không được chặn tư vấn.
    let activity = null;
    try {
      activity = await loadActivityContext(req.user.id, timeZone);
    } catch (error) {
      console.error("ai_context_failed", { code: error?.code ?? "context_error" });
    }

    const response = await requestAdvice({ prompt: prompt.trim(), reflection, activity });
    return res.json({ ...response, reflectionUsed });
  } catch (error) {
    return next(error);
  }
});

export default router;

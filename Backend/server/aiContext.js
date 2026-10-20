import { query } from "./db.js";
import { formatJournalDate } from "./domain.js";

export const ACTIVITY_WINDOW_DAYS = 7;

export function resolveTimeZone(value) {
  if (typeof value !== "string" || !value || value.length > 64) return "UTC";
  try {
    new Intl.DateTimeFormat("en-US", { timeZone: value }).format(new Date());
    return value;
  } catch {
    return "UTC";
  }
}

// Ngày hiện tại (yyyy-MM-dd) theo múi giờ của thiết bị.
export function todayInTimeZone(timeZone, now = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone, year: "numeric", month: "2-digit", day: "2-digit"
  }).format(now);
}

// Số liệu tổng hợp 7 ngày gần nhất để POP-AI có dữ liệu thật thay vì phải đoán.
// Chỉ lấy số liệu và cấu hình kế hoạch; không lấy văn bản tự do (nhiệm vụ, nhật ký).
export async function loadActivityContext(userId, timeZone) {
  const endIso = todayInTimeZone(timeZone);
  const days = ACTIVITY_WINDOW_DAYS;
  const [journal, focus, plan, assessment] = await Promise.all([
    query(
      `SELECT COUNT(*)::int AS journal_days,
              COALESCE(ROUND(AVG(estimated_short_video_minutes)), 0)::int AS avg_video
         FROM pop_journals
        WHERE user_id = $1 AND entry_date BETWEEN ($3::date - ($2::int - 1)) AND $3::date`,
      [userId, days, endIso]
    ),
    query(
      `SELECT COALESCE(SUM(actual_minutes) FILTER (WHERE status = 'COMPLETED'), 0)::int AS focus_minutes,
              COUNT(*) FILTER (WHERE status = 'COMPLETED')::int AS completed_count,
              COALESCE(SUM(interruption_count), 0)::int AS interruption_count,
              COALESCE(AVG(self_rating) FILTER (WHERE self_rating > 0), 0)::numeric AS avg_rating
         FROM pop_focus_sessions
        WHERE user_id = $1
          AND (created_at AT TIME ZONE $4)::date BETWEEN ($3::date - ($2::int - 1)) AND $3::date`,
      [userId, days, endIso, timeZone]
    ),
    query("SELECT plan FROM pop_personal_plans WHERE user_id = $1", [userId]),
    query(
      `SELECT scores FROM pop_assessment_results
        WHERE user_id = $1 ORDER BY completed_at DESC LIMIT 1`,
      [userId]
    )
  ]);

  const [year, month, day] = endIso.split("-");
  const planRow = plan.rows[0]?.plan;
  const scores = assessment.rows[0]?.scores;
  return {
    days,
    endDate: formatJournalDate(`${year}-${month}-${day}`),
    journalDays: journal.rows[0].journal_days,
    avgShortVideoMinutes: journal.rows[0].avg_video,
    completedSessions: focus.rows[0].completed_count,
    focusMinutes: focus.rows[0].focus_minutes,
    interruptions: focus.rows[0].interruption_count,
    avgFocusRating: Number(Number(focus.rows[0].avg_rating).toFixed(1)),
    plan: planRow ? {
      dailyShortVideoLimitMinutes: planRow.dailyShortVideoLimitMinutes,
      trackedPlatforms: Array.isArray(planRow.trackedPlatforms) ? planRow.trackedPlatforms : [],
      sessionDurationMinutes: planRow.sessionDurationMinutes,
      targetDays: planRow.targetDays
    } : null,
    assessment: scores ? { VN: scores.VN, PT: scores.PT, PB: scores.PB, TT: scores.TT } : null
  };
}

export async function loadJournalForDate(userId, isoDate) {
  const result = await query(
    `SELECT estimated_short_video_minutes, focus_minutes, interruption_count,
            distractors, most_productive_session, tomorrow_adjustment
       FROM pop_journals WHERE user_id = $1 AND entry_date = $2::date LIMIT 1`,
    [userId, isoDate]
  );
  return result.rows[0] ?? null;
}

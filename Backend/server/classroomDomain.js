import { createHmac, createHash, randomBytes, timingSafeEqual } from "node:crypto";

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export const isUuid = (value) => typeof value === "string" && UUID_PATTERN.test(value);

/** Chuẩn hoá tên trường/lớp thành khoá so sánh: bỏ dấu, chữ thường, gộp khoảng trắng. */
export function nameKey(value) {
  if (typeof value !== "string") return "";
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "d")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

/** Tên hiển thị: cắt khoảng trắng thừa; trả null nếu rỗng hoặc quá dài. */
export function cleanName(value, maxLength = 100) {
  if (typeof value !== "string") return null;
  const cleaned = value.replace(/\s+/g, " ").trim();
  if (!cleaned || cleaned.length > maxLength) return null;
  return cleaned;
}

export function validateNotification(input) {
  const title = typeof input?.title === "string" ? input.title.trim() : "";
  const body = typeof input?.body === "string" ? input.body.trim() : "";
  if (title.length < 1 || title.length > 80) {
    return { error: "Tiêu đề thông báo phải từ 1 đến 80 ký tự." };
  }
  if (body.length < 1 || body.length > 500) {
    return { error: "Nội dung thông báo phải từ 1 đến 500 ký tự." };
  }
  let studentIds = null;
  if (input?.studentIds !== undefined && input.studentIds !== null) {
    if (!Array.isArray(input.studentIds) || input.studentIds.length === 0 || input.studentIds.length > 500 ||
      !input.studentIds.every(isUuid)) {
      return { error: "Danh sách học sinh nhận thông báo không hợp lệ." };
    }
    studentIds = [...new Set(input.studentIds.map((id) => id.toLowerCase()))];
  }
  return { title, body, studentIds };
}

export function validateTimeZone(value, fallback = "Asia/Ho_Chi_Minh") {
  const zone = typeof value === "string" && value ? value : fallback;
  try {
    new Intl.DateTimeFormat("en-US", { timeZone: zone }).format(new Date());
    return zone;
  } catch {
    return null;
  }
}

export function parseDays(value, fallback = 7, max = 90) {
  if (value === undefined) return fallback;
  const days = Number(value);
  return Number.isInteger(days) && days >= 1 && days <= max ? days : null;
}

// ---------------------------------------------------------------------------
// Cổng mật khẩu admin: mật khẩu giáo viên nhập ở trang /admin để được phép đăng nhập/đăng ký.
// Token không lưu DB: chuỗi "hạn.nonce.chữ ký HMAC", sống ngắn.
// ---------------------------------------------------------------------------
export const GATE_TTL_MS = 30 * 60 * 1000;

const sha256 = (text) => createHash("sha256").update(text, "utf8").digest();

export function safeEqualText(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  return timingSafeEqual(sha256(a), sha256(b));
}

function gateKey(adminPassword, secret) {
  return secret ? sha256(`pop-gate-secret:${secret}`) : sha256(`pop-gate:${adminPassword}`);
}

export function issueGateToken(adminPassword, { secret, now = Date.now() } = {}) {
  const expiresAt = now + GATE_TTL_MS;
  const payload = `${expiresAt}.${randomBytes(12).toString("base64url")}`;
  const signature = createHmac("sha256", gateKey(adminPassword, secret)).update(payload).digest("base64url");
  return { token: `${payload}.${signature}`, expiresAt };
}

export function verifyGateToken(token, adminPassword, { secret, now = Date.now() } = {}) {
  if (!adminPassword || typeof token !== "string" || token.length > 200) return false;
  const parts = token.split(".");
  if (parts.length !== 3) return false;
  const [expiresText, nonce, signature] = parts;
  const expiresAt = Number(expiresText);
  if (!Number.isFinite(expiresAt) || expiresAt < now || !nonce) return false;
  const expected = createHmac("sha256", gateKey(adminPassword, secret))
    .update(`${expiresText}.${nonce}`).digest("base64url");
  return safeEqualText(expected, signature);
}

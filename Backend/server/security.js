import { createHash, randomBytes, randomUUID } from "node:crypto";
import { pool, query } from "./db.js";

const ACCESS_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const REFRESH_TTL_MS = 90 * 24 * 60 * 60 * 1000;

export function hashToken(token) {
  return createHash("sha256").update(token, "utf8").digest("hex");
}

export async function createSession(userId) {
  const accessToken = randomBytes(32).toString("base64url");
  const refreshToken = randomBytes(32).toString("base64url");
  const accessExpiresAt = new Date(Date.now() + ACCESS_TTL_MS);
  const refreshExpiresAt = new Date(Date.now() + REFRESH_TTL_MS);
  await query(
    `INSERT INTO pop_sessions
      (id, user_id, access_token_hash, refresh_token_hash, access_expires_at, refresh_expires_at)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [randomUUID(), userId, hashToken(accessToken), hashToken(refreshToken), accessExpiresAt, refreshExpiresAt]
  );
  return { accessToken, refreshToken };
}

export async function rotateSession(refreshToken) {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const found = await client.query(
      `SELECT s.id, u.id AS user_id, u.email, u.full_name, u.role
         FROM pop_sessions s
         JOIN pop_users u ON u.id = s.user_id
        WHERE s.refresh_token_hash = $1 AND s.refresh_expires_at > NOW()
        FOR UPDATE OF s`,
      [hashToken(refreshToken)]
    );
    if (found.rowCount !== 1) {
      await client.query("ROLLBACK");
      return null;
    }

    const accessToken = randomBytes(32).toString("base64url");
    const newRefreshToken = randomBytes(32).toString("base64url");
    await client.query(
      `UPDATE pop_sessions
          SET access_token_hash = $1, refresh_token_hash = $2,
              access_expires_at = $3, refresh_expires_at = $4
        WHERE id = $5`,
      [
        hashToken(accessToken),
        hashToken(newRefreshToken),
        new Date(Date.now() + ACCESS_TTL_MS),
        new Date(Date.now() + REFRESH_TTL_MS),
        found.rows[0].id
      ]
    );
    await client.query("COMMIT");
    const user = found.rows[0];
    return { accessToken, refreshToken: newRefreshToken, user: toUserDto(user) };
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}

export async function requireAuth(req, res, next) {
  try {
    const header = req.get("authorization") ?? "";
    const match = /^Bearer\s+([A-Za-z0-9_-]{40,})$/i.exec(header);
    if (!match) return res.status(401).json({ message: "Vui lòng đăng nhập để tiếp tục." });
    const result = await query(
      `SELECT u.id, u.email, u.full_name, u.role
         FROM pop_sessions s
         JOIN pop_users u ON u.id = s.user_id
        WHERE s.access_token_hash = $1
          AND s.access_expires_at > NOW()
          AND s.refresh_expires_at > NOW()`,
      [hashToken(match[1])]
    );
    if (result.rowCount !== 1) {
      return res.status(401).json({ message: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại." });
    }
    req.user = result.rows[0];
    req.accessTokenHash = hashToken(match[1]);
    return next();
  } catch (error) {
    return next(error);
  }
}

export function toUserDto(user) {
  return {
    id: user.id,
    email: user.email,
    fullName: user.full_name,
    role: user.role
  };
}

export function requireTeacher(req, res, next) {
  if (req.user?.role !== "TEACHER") {
    return res.status(403).json({ message: "Chỉ tài khoản giáo viên mới dùng được chức năng này." });
  }
  return next();
}

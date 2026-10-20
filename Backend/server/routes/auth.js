import { randomUUID } from "node:crypto";
import { Router } from "express";
import rateLimit from "express-rate-limit";
import bcrypt from "bcryptjs";
import { query } from "../db.js";
import { normalizeEmail } from "../domain.js";
import { createSession, requireAuth, rotateSession, toUserDto } from "../security.js";

const router = Router();
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { message: "Quá nhiều lần thử. Vui lòng chờ rồi thử lại." }
});
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

router.post("/register", authLimiter, async (req, res, next) => {
  try {
    const email = normalizeEmail(req.body?.email);
    const password = req.body?.password;
    const fullName = typeof req.body?.fullName === "string" ? req.body.fullName.trim() : "";
    if (!emailPattern.test(email) || email.length > 254) {
      return res.status(400).json({ message: "Email không hợp lệ." });
    }
    if (typeof password !== "string" || password.length < 8 || password.length > 128) {
      return res.status(400).json({ message: "Mật khẩu phải có từ 8 đến 128 ký tự." });
    }
    if (!fullName || fullName.length > 100) {
      return res.status(400).json({ message: "Vui lòng nhập họ tên (tối đa 100 ký tự)." });
    }

    const userId = randomUUID();
    const passwordHash = await bcrypt.hash(password, 12);
    const inserted = await query(
      `INSERT INTO pop_users (id, email, full_name, password_hash)
       VALUES ($1, $2, $3, $4)
       RETURNING id, email, full_name, role`,
      [userId, email, fullName, passwordHash]
    );
    const user = inserted.rows[0];
    const tokens = await createSession(user.id);
    return res.status(201).json({ ...tokens, user: toUserDto(user) });
  } catch (error) {
    if (error?.code === "23505") {
      return res.status(409).json({ message: "Email này đã được đăng ký." });
    }
    return next(error);
  }
});

router.post("/login", authLimiter, async (req, res, next) => {
  try {
    const email = normalizeEmail(req.body?.email);
    const password = req.body?.password;
    if (!emailPattern.test(email) || typeof password !== "string" || password.length > 128) {
      return res.status(401).json({ message: "Email hoặc mật khẩu không đúng." });
    }
    const found = await query(
      "SELECT id, email, full_name, password_hash, role FROM pop_users WHERE email = $1",
      [email]
    );
    if (found.rowCount !== 1 || !(await bcrypt.compare(password, found.rows[0].password_hash))) {
      return res.status(401).json({ message: "Email hoặc mật khẩu không đúng." });
    }
    const user = found.rows[0];
    const tokens = await createSession(user.id);
    return res.json({ ...tokens, user: toUserDto(user) });
  } catch (error) {
    return next(error);
  }
});

router.post("/refresh", authLimiter, async (req, res, next) => {
  try {
    const refreshToken = req.body?.refreshToken;
    if (typeof refreshToken !== "string" || !/^[A-Za-z0-9_-]{40,}$/.test(refreshToken)) {
      return res.status(401).json({ message: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại." });
    }
    const rotated = await rotateSession(refreshToken);
    if (!rotated) return res.status(401).json({ message: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại." });
    return res.json(rotated);
  } catch (error) {
    return next(error);
  }
});

router.get("/me", requireAuth, (req, res) => {
  res.json(toUserDto(req.user));
});

router.post("/logout", requireAuth, async (req, res, next) => {
  try {
    await query("DELETE FROM pop_sessions WHERE access_token_hash = $1", [req.accessTokenHash]);
    return res.status(204).end();
  } catch (error) {
    return next(error);
  }
});

export default router;

import express from "express";
import { fileURLToPath } from "node:url";
import { pool, query, runMigrations } from "./db.js";
import authRoutes from "./routes/auth.js";
import popRoutes from "./routes/pop.js";
import classroomRoutes from "./routes/classroom.js";
import teacherRoutes from "./routes/teacher.js";

const app = express();
const port = Number(process.env.PORT || 3000);
const routeManifestPath = fileURLToPath(new URL("../public/manus-routes.json", import.meta.url));
const adminDir = fileURLToPath(new URL("../public/admin", import.meta.url));
let schemaReady = false;
app.disable("x-powered-by");
app.set("trust proxy", 1);
app.use((req, res, next) => {
  res.setHeader("Cache-Control", "no-store");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});
app.use(express.json({ limit: "64kb", strict: true }));

app.get("/", (_req, res) => res.json({ service: "POP AI API", version: "mobile/v1" }));
app.get("/manus-routes.json", (_req, res) => res.sendFile(routeManifestPath));
app.get("/healthz", async (_req, res) => {
  if (!schemaReady) return res.status(503).json({ status: "starting" });
  try {
    await query("SELECT 1");
    return res.status(200).json({ status: "ok", database: "connected" });
  } catch {
    return res.status(503).json({ status: "unavailable", database: "disconnected" });
  }
});
// Trang web quản trị dành cho giáo viên (mật khẩu admin -> đăng nhập/đăng ký giáo viên -> dashboard lớp).
app.use("/admin", (_req, res, next) => {
  res.setHeader(
    "Content-Security-Policy",
    "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; " +
    "base-uri 'none'; form-action 'none'; frame-ancestors 'none'"
  );
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
}, express.static(adminDir, { index: "index.html", dotfiles: "ignore" }));

const requireSchema = (_req, res, next) => {
  if (!schemaReady) return res.status(503).json({ message: "Máy chủ đang khởi động. Vui lòng thử lại." });
  return next();
};
app.use("/api/mobile/v1", requireSchema);
app.use("/api/teacher/v1", requireSchema);
app.use("/api/mobile/v1/auth", authRoutes);
app.use("/api/teacher/v1", teacherRoutes);
app.use("/api/mobile/v1", classroomRoutes);
app.use("/api/mobile/v1", popRoutes);
app.use((_req, res) => res.status(404).json({ message: "Không tìm thấy API." }));
app.use((error, req, res, _next) => {
  if (res.headersSent) return;
  if (error?.type === "entity.too.large") {
    return res.status(413).json({ message: "Dữ liệu gửi lên vượt quá giới hạn." });
  }
  if (error?.type === "entity.parse.failed") {
    return res.status(400).json({ message: "JSON request không hợp lệ." });
  }
  const status = Number.isInteger(error?.status) ? error.status : 500;
  console.error("request_failed", { path: req.path, status, code: error?.code ?? "application_error" });
  // Lỗi 5xx chỉ lộ message khi chủ động đánh dấu expose (ví dụ lỗi dịch vụ AI đã được viết sẵn tiếng Việt).
  const showMessage = status < 500 || error?.expose === true;
  return res.status(status).json({
    message: showMessage ? error.message : "Máy chủ tạm thời gặp sự cố. Vui lòng thử lại."
  });
});

const server = app.listen(port, "0.0.0.0", () => {
  console.log(`POP AI API listening on port ${port}`);
});

runMigrations()
  .then(() => {
    schemaReady = true;
    console.log("POP AI API database schema ready");
  })
  .catch(async (error) => {
    console.error("startup_migration_failed", { code: error?.code ?? "database_migration_error" });
    server.close();
    await pool.end().catch(() => {});
    process.exitCode = 1;
  });

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    server.close(async () => {
      await pool.end().catch(() => {});
      process.exit(0);
    });
  });
}

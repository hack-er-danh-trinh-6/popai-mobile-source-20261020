# POP AI API

Backend REST cho ứng dụng Android POP AI. API giữ các đường dẫn `api/mobile/v1` của ứng dụng, lưu dữ liệu vào PostgreSQL Aiven và gọi Manus LLM cho POP-AI. `DATABASE_URL` chỉ được đọc từ secret runtime phía server; không ghi secret vào file, APK hoặc log.

## Chạy trong Manus Preview

Yêu cầu Node.js 22+, pnpm 10.18.0 và secret `DATABASE_URL`. Chạy `pnpm install`, sau đó `pnpm dev`. Khi server khởi động, migration additive `server/migrations/001_pop_mobile_api.sql` tạo các bảng mới có tiền tố `pop_` trong schema `public`; migration có phiên bản và không xoá hoặc ghi đè bảng khác. API lắng nghe cổng `PORT` (mặc định 3000).

Aiven xác nhận `sslmode=require` bật mã hóa TLS nhưng không xác minh chứng chỉ server. Backend giữ chế độ đó và đặt TLS tối thiểu 1.2, đúng với URI đã cung cấp; muốn xác thực CA/hostname mạnh hơn, cần CA bundle tải từ trang Overview của dịch vụ Aiven.

`GET /healthz` kiểm tra kết nối PostgreSQL. Các endpoint người dùng nằm dưới `/api/mobile/v1` và cần bearer token, ngoại trừ đăng ký, đăng nhập và health check. `GET /plan` trả `404` khi tài khoản chưa lưu kế hoạch; không tạo kế hoạch mẫu.

## API

- `POST /api/mobile/v1/auth/register`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`, `POST /auth/logout`
- `POST /assessment/submit`, `GET /assessment/history`
- `GET /plan`, `POST /plan`
- `POST /focus/session`
- `POST /journal`, `GET /journal/today`
- `GET /progress/summary?days=7`
- `POST /ai/advice`

Các đường dẫn trên được ghép với prefix `/api/mobile/v1`. Tư vấn AI dùng `MANUS_API_URL` và `MANUS_API_KEY` runtime do Manus cấp (tuỳ chọn `MANUS_MODEL` nếu cần chỉ định mô hình); không cần API key riêng trong app. Phần tổng hợp tiến độ nhận `endDate` (`dd/MM/yyyy`) và `timeZone` dạng IANA từ thiết bị để thống nhất ngày lịch.

## POP-AI (`POST /ai/advice`)

Yêu cầu: `{ "prompt": string (≤ 2000 ký tự), "includeReflection": boolean, "reflectionDate": "dd/MM/yyyy", "timeZone": "IANA" }`. Phản hồi: `{ "adviceText": string, "actionableSteps": string[], "reflectionUsed": boolean }`.

- **Dữ liệu thật làm ngữ cảnh.** Server tự gắn số liệu tổng hợp 7 ngày gần nhất theo `timeZone` của thiết bị (phiên tập trung, số lần bị ngắt, video ngắn trung bình, cấu hình kế hoạch, điểm tự đánh giá gần nhất) để POP-AI không phải đoán xu hướng. Chỉ gửi số liệu và cấu hình; văn bản tự do (nhiệm vụ trong kế hoạch, ghi chú nhật ký) không được gửi trừ khi người dùng bật `includeReflection`. Nếu chưa có dữ liệu, POP-AI được hướng dẫn nói rõ thay vì bịa số liệu.
- **Nhật ký chỉ khi đồng ý.** `reflectionUsed` cho biết nhật ký của `reflectionDate` có thực sự được dùng (false nếu hôm đó chưa có nhật ký).
- **Định dạng đầu ra.** Server chấp nhận JSON thuần, JSON bọc trong khối mã hoặc có lời dẫn; chuẩn hoá các bước (tối đa 5, mỗi bước ≤ 200 ký tự để lưu được vào kế hoạch). JSON sai cấu trúc trả `502` thay vì hiển thị JSON thô.
- **An toàn.** Dữ liệu người dùng được đặt trong các khối có thẻ riêng và bị gỡ ký tự `<` `>` để chống giả mạo chỉ dẫn; system prompt hướng người dùng tới hỗ trợ khẩn cấp khi có dấu hiệu khủng hoảng và không đưa chẩn đoán y khoa.
- **Giới hạn.** 10 yêu cầu/15 phút theo tài khoản. Lỗi dịch vụ AI trả `503`/`504`/`502` kèm thông báo tiếng Việt; message gốc của nhà cung cấp không được chuyển tiếp.

Tài liệu TLS: [Aiven TLS/SSL certificates](https://aiven.io/docs/platform/concepts/tls-ssl-certificates).

## Quản lý lớp học (học sinh ↔ giáo viên)

Migration `002_classroom.sql` thêm các bảng `pop_schools`, `pop_classes`, `pop_class_members`, `pop_notifications`, `pop_notification_recipients`.

**Biến môi trường mới**

- `TEACHER_ADMIN_PASSWORD` (bắt buộc để dùng trang giáo viên): mật khẩu giáo viên nhập ở `/admin` để được đăng nhập/đăng ký. Chưa đặt thì API trả `503`.
- `TEACHER_GATE_SECRET` (tuỳ chọn): khoá ký phiên nhập mật khẩu admin; mặc định suy ra từ `TEACHER_ADMIN_PASSWORD`.

**Luồng**

1. Học sinh đăng ký trong app → app hỏi chọn trường/lớp → `POST /api/mobile/v1/class/join-request`.
2. Giáo viên mở `/admin` → nhập mật khẩu admin → đăng ký tài khoản giáo viên (khai báo trường và lớp) hoặc đăng nhập → dashboard: duyệt học sinh, xem thói quen tập trung, gửi thông báo.
3. Học sinh nhận thông báo như thông báo điện thoại (app kiểm tra 30 giây/lần khi mở và 15 phút/lần khi đóng).

**API học sinh** (`/api/mobile/v1`, cần token học sinh): `GET /schools?q=`, `GET /schools/:id/classes`, `GET|DELETE /class/membership`, `POST /class/join-request`, `GET /notifications`, `POST /notifications/deliver`, `POST /notifications/read`.

**API giáo viên** (`/api/teacher/v1`): `POST /gate`; `POST /auth/register|login` (cần header `X-Gate-Token`); `GET /auth/me`, `POST /auth/logout`; `GET|POST /classes`; `GET /classes/:id/requests`; `POST /classes/:id/requests/approve-all`, `/requests/:userId/approve|reject`; `GET /classes/:id/students?days=&timeZone=`; `GET /classes/:id/students/:userId/focus`; `DELETE /classes/:id/students/:userId`; `GET|POST /classes/:id/notifications`.

**Quyền riêng tư:** giáo viên chỉ thấy phiên tập trung (phút, số phiên, lần ngắt, tự đánh giá, tên nhiệm vụ) từ lúc học sinh được duyệt; không thấy nhật ký, bài đánh giá hay hội thoại POP-AI.

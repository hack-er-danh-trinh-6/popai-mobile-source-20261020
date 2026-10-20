# POP AI development source bundle

This ZIP preserves the complete Android and Backend source trees from the supplied project archive.

- Android source: `Android/` (build with `./gradlew :app:assembleDebug`).
- Backend source: `Backend/` (Node.js 22, pnpm 10.18; run `pnpm install` then `pnpm dev`).
- For development as requested, the PostgreSQL Aiven URI is embedded directly in `Backend/server/db.js`; no `.env` file is included.

**Security:** the source contains the database credential in plain text. Share privately and do not commit this bundle publicly; rotate the database credential if it is exposed. Manus Service API credentials are not included; AI uses `MANUS_API_URL` and `MANUS_API_KEY` supplied by the Manus runtime.

## Quản lý lớp học

- Backend: migration `002_classroom.sql`, `server/routes/teacher.js` (API giáo viên), `server/routes/classroom.js` (API học sinh), `server/classroomDomain.js` (hàm thuần + test), trang web giáo viên ở `Backend/public/admin/` (truy cập `/admin`).
- Cần đặt `TEACHER_ADMIN_PASSWORD` trên máy chủ để giáo viên đăng ký/đăng nhập (xem `Backend/README.md`).
- Android: `ui/classroom/` (chọn trường/lớp, trạng thái, hộp thư), `data/repository/ClassroomRepository.kt`, `core/notification/` (thông báo điện thoại qua WorkManager + kiểm tra 30 giây khi app mở). Sau khi đăng ký, học sinh được hỏi chọn lớp một lần.
- Tài khoản giáo viên không đăng nhập được trong app (chỉ trên web).
- Chưa biên dịch Android trong môi trường tạo bundle này: hãy chạy `./gradlew :app:assembleDebug` để kiểm tra.

## Khắc phục: "Máy chủ chưa hỗ trợ tính năng lớp học"

**Nguyên nhân.** App nhận `404` kèm thông báo mặc định `"Không tìm thấy API."` khi gọi `GET /api/mobile/v1/schools`
và `GET /api/mobile/v1/class/membership`. Mã nguồn trong `Backend/` đã có các route này (`server/routes/classroom.js`),
nên máy chủ đang chạy tại `BASE_URL` (`Android/app/build.gradle.kts`) là bản **cũ, chưa được triển khai lại** sau khi thêm lớp học.
Sửa code Android không làm hết lỗi này; phải triển khai lại `Backend/`.

**Kiểm tra nhanh máy chủ** (mở trong trình duyệt, không cần đăng nhập): `<BASE_URL>api/mobile/v1/schools`

- `{"message":"Không tìm thấy API."}` (404) → máy chủ vẫn là bản cũ → triển khai lại `Backend/`.
- `{"message":"Vui lòng đăng nhập để tiếp tục."}` (401) → đã là bản mới, route lớp học hoạt động.
- `{"message":"Máy chủ đang khởi động. Vui lòng thử lại."}` (503) → migration chưa chạy xong hoặc lỗi kết nối DB; xem log máy chủ.

**Triển khai lại.** Chạy lại toàn bộ thư mục `Backend/` (Node 22, `pnpm install --frozen-lockfile`, `pnpm start`; có `Dockerfile`).
Khi khởi động, `002_classroom.sql` tự tạo các bảng lớp học. Đặt biến môi trường `TEACHER_ADMIN_PASSWORD` để giáo viên dùng được `/admin`.

**Sau khi triển khai.** Giáo viên mở `/admin`, nhập mật khẩu admin, đăng ký tài khoản và khai báo trường + lớp.
Học sinh mở Lớp học trong app sẽ thấy trường đó (danh sách trống khi chưa có giáo viên nào khai báo là hành vi bình thường).

**Thay đổi ở app (`ui/classroom/`).** Lỗi tải danh sách trường/lớp không còn bị hiển thị như "chưa có trường nào";
khi chưa tải được trạng thái lớp, app ẩn bộ chọn trường và hiện nút **Thử lại**.

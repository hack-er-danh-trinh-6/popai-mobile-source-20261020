# POPAI Android + Backend

Source bundle containing the Android Kotlin app in `Android/` and the Node.js API in `Backend/`.

- Backend deployment notes and runtime environment variables: [`Backend/README.md`](Backend/README.md)
- Android API base URL is configured in `Android/app/build.gradle.kts`.
- Do not commit runtime secrets (`DATABASE_URL`, `MANUS_API_KEY`, `TEACHER_ADMIN_PASSWORD`) or signing keys.

The backend uses PostgreSQL and applies additive migrations at startup. See the backend README before deploying.

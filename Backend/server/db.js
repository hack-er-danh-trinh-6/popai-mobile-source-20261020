import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import pg from "pg";

const { Pool } = pg;
const suppliedUrl = process.env.DATABASE_URL;
if (!suppliedUrl) throw new Error("DATABASE_URL is required");
const dbUrl = new URL(suppliedUrl);
const sslMode = (dbUrl.searchParams.get("sslmode") ?? "").toLowerCase();
const poolOptions = {
  connectionString: suppliedUrl,
  max: 5,
  connectionTimeoutMillis: 8000,
  idleTimeoutMillis: 30000,
  statement_timeout: 15000,
  application_name: "pop-ai-api"
};
if (sslMode === "require") {
  // Aiven documents sslmode=require as encrypted TLS without CA verification.
  // Remove the query option so pg-connection-string cannot reinterpret it as verify-full.
  dbUrl.searchParams.delete("sslmode");
  poolOptions.connectionString = dbUrl.toString();
  poolOptions.ssl = { rejectUnauthorized: false, minVersion: "TLSv1.2" };
}

export const pool = new Pool(poolOptions);

export async function query(text, values = []) {
  return pool.query(text, values);
}

const MIGRATIONS = [
  { version: "001_pop_mobile_api", file: "./migrations/001_pop_mobile_api.sql" },
  { version: "002_classroom", file: "./migrations/002_classroom.sql" }
];

export async function runMigrations() {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    await client.query("SELECT pg_advisory_xact_lock(hashtext($1))", ["pop_ai_api_migrations"]);
    await client.query(`
      CREATE TABLE IF NOT EXISTS pop_schema_migrations (
        version TEXT PRIMARY KEY,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      )
    `);
    for (const migration of MIGRATIONS) {
      const existing = await client.query(
        "SELECT 1 FROM pop_schema_migrations WHERE version = $1",
        [migration.version]
      );
      if (existing.rowCount > 0) continue;
      const sql = await readFile(fileURLToPath(new URL(migration.file, import.meta.url)), "utf8");
      await client.query(sql);
      await client.query("INSERT INTO pop_schema_migrations(version) VALUES ($1)", [migration.version]);
    }
    await client.query("COMMIT");
  } catch (error) {
    await client.query("ROLLBACK").catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}

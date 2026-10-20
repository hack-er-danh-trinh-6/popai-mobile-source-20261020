import assert from "node:assert/strict";
import test from "node:test";
import {
  cleanName, GATE_TTL_MS, issueGateToken, isUuid, nameKey, parseDays,
  validateNotification, validateTimeZone, verifyGateToken
} from "./classroomDomain.js";

test("nameKey bỏ dấu, chữ hoa và khoảng trắng thừa", () => {
  assert.equal(nameKey("  THPT  Chu Văn An "), "thpt chu van an");
  assert.equal(nameKey("Lớp 10Đ1"), "lop 10d1");
  assert.equal(nameKey(null), "");
});

test("cleanName từ chối tên rỗng hoặc quá dài", () => {
  assert.equal(cleanName("  Lớp   10A1 "), "Lớp 10A1");
  assert.equal(cleanName("   "), null);
  assert.equal(cleanName("a".repeat(101)), null);
  assert.equal(cleanName(5), null);
});

test("validateNotification kiểm tra độ dài và danh sách học sinh", () => {
  assert.ok(validateNotification({ title: "", body: "x" }).error);
  assert.ok(validateNotification({ title: "x", body: "y".repeat(501) }).error);
  assert.ok(validateNotification({ title: "x", body: "y", studentIds: ["abc"] }).error);
  assert.ok(validateNotification({ title: "x", body: "y", studentIds: [] }).error);
  const id = "123e4567-e89b-12d3-a456-426614174000";
  const ok = validateNotification({ title: " Nhắc nhở ", body: " Nộp bài ", studentIds: [id, id.toUpperCase()] });
  assert.equal(ok.title, "Nhắc nhở");
  assert.equal(ok.body, "Nộp bài");
  assert.deepEqual(ok.studentIds, [id]);
  assert.equal(validateNotification({ title: "a", body: "b" }).studentIds, null);
});

test("parseDays và validateTimeZone", () => {
  assert.equal(parseDays(undefined), 7);
  assert.equal(parseDays("30"), 30);
  assert.equal(parseDays("0"), null);
  assert.equal(parseDays("91"), null);
  assert.equal(validateTimeZone(undefined), "Asia/Ho_Chi_Minh");
  assert.equal(validateTimeZone("Not/AZone"), null);
  assert.ok(isUuid("123e4567-e89b-12d3-a456-426614174000"));
  assert.equal(isUuid("nope"), false);
});

test("gate token hợp lệ, hết hạn và bị sửa đổi", () => {
  const now = 1_000_000;
  const { token } = issueGateToken("mat-khau", { now });
  assert.equal(verifyGateToken(token, "mat-khau", { now: now + 1000 }), true);
  assert.equal(verifyGateToken(token, "mat-khau-khac", { now: now + 1000 }), false);
  assert.equal(verifyGateToken(token, "mat-khau", { now: now + GATE_TTL_MS + 1 }), false);
  const tampered = token.replace(/^\d+/, String(now + 10 * GATE_TTL_MS));
  assert.equal(verifyGateToken(tampered, "mat-khau", { now: now + 1000 }), false);
  assert.equal(verifyGateToken("rac", "mat-khau"), false);
  assert.equal(verifyGateToken(token, "", { now }), false);
  const keyed = issueGateToken("mat-khau", { now, secret: "s1" });
  assert.equal(verifyGateToken(keyed.token, "mat-khau", { now, secret: "s1" }), true);
  assert.equal(verifyGateToken(keyed.token, "mat-khau", { now, secret: "s2" }), false);
});

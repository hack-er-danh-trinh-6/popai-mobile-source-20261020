export function normalizeEmail(value) {
  return typeof value === "string" ? value.trim().toLowerCase() : "";
}

export function parseJournalDate(value) {
  if (typeof value !== "string") return null;
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
  if (!match) return null;
  const [, dayText, monthText, yearText] = match;
  const day = Number(dayText);
  const month = Number(monthText);
  const year = Number(yearText);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) return null;
  return `${yearText}-${monthText}-${dayText}`;
}

export function formatJournalDate(isoDate) {
  const [year, month, day] = isoDate.slice(0, 10).split("-");
  return `${day}/${month}/${year}`;
}

export function scoreAssessment(answers) {
  if (!Array.isArray(answers) || answers.length !== 20) {
    throw new Error("Vui lòng gửi đủ 20 câu trả lời.");
  }
  const seen = new Set();
  const groups = { VN: [], PT: [], PB: [], TT: [] };
  for (const answer of answers) {
    const questionId = answer?.questionId;
    const score = Number(answer?.score);
    if (
      typeof questionId !== "string" ||
      !/^(VN|PT|PB|TT)[1-5]$/.test(questionId) ||
      !Number.isInteger(score) || score < 1 || score > 5 ||
      seen.has(questionId)
    ) throw new Error("Câu trả lời không hợp lệ.");
    seen.add(questionId);
    groups[questionId.slice(0, 2)].push(score);
  }
  for (const group of Object.values(groups)) {
    if (group.length !== 5) throw new Error("Bộ câu hỏi không đầy đủ.");
  }
  return Object.fromEntries(
    Object.entries(groups).map(([key, values]) => [
      key,
      Math.round((values.reduce((sum, value) => sum + value, 0) / values.length) * 10) / 10
    ])
  );
}

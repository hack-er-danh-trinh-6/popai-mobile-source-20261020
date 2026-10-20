package com.health.pop.domain.usecase

class ValidateAiPromptUseCase {

    /** Cắt câu hỏi theo giới hạn của server (POST /ai/advice nhận tối đa 2000 ký tự). */
    fun limit(raw: String): String = raw.take(MAX_PROMPT_LENGTH)

    /** Câu hỏi gửi đi: đã cắt khoảng trắng; nếu trống thì dùng câu hỏi mặc định. */
    fun resolvePrompt(raw: String): String = raw.trim().ifEmpty { DEFAULT_PROMPT }

    companion object {
        const val MAX_PROMPT_LENGTH = 2000
        const val DEFAULT_PROMPT =
            "Hãy phân tích xu hướng tuần qua và cho mình 2 lời khuyên nhỏ để tập trung tốt hơn."
    }
}

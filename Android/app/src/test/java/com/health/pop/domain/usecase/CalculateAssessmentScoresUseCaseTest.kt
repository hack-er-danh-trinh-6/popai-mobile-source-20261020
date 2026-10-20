package com.health.pop.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateAssessmentScoresUseCaseTest {

    private val calculateUseCase = CalculateAssessmentScoresUseCase()

    @Test
    fun testCalculateAssessmentScores_correctAverages() {
        val answers = mapOf(
            "VN1" to 5, "VN2" to 4, "VN3" to 3, "VN4" to 4, "VN5" to 4, // VN avg = 20 / 5 = 4.0
            "PT1" to 2, "PT2" to 3, "PT3" to 2, "PT4" to 3, "PT5" to 2, // PT avg = 12 / 5 = 2.4
            "PB1" to 1, "PB2" to 2, "PB3" to 1, "PB4" to 2, "PB5" to 2, // PB avg = 8 / 5 = 1.6
            "TT1" to 5, "TT2" to 5, "TT3" to 4, "TT4" to 5, "TT5" to 4  // TT avg = 23 / 5 = 4.6
        )

        val result = calculateUseCase(answers)

        assertEquals(4.0f, result.vnScore, 0.01f)
        assertEquals(2.4f, result.ptScore, 0.01f)
        assertEquals(1.6f, result.pbScore, 0.01f)
        assertEquals(4.6f, result.ttScore, 0.01f)
        assertEquals("Chỉ để tự theo dõi, không phải chẩn đoán", result.note)
    }
}

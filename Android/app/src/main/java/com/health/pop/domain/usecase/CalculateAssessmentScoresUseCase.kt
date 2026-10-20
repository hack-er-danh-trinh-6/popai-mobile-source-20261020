package com.health.pop.domain.usecase

import com.health.pop.domain.model.AssessmentGroup
import com.health.pop.domain.model.AssessmentResult

class CalculateAssessmentScoresUseCase {

    /**
     * Calculates score averages for VN, PT, PB, TT groups from 20 answers.
     * @param answers Map of questionId to score (1 to 5).
     */
    operator fun invoke(answers: Map<String, Int>): AssessmentResult {
        fun getGroupAverage(groupPrefix: String): Float {
            val groupScores = answers.filterKeys { it.startsWith(groupPrefix) }.values
            if (groupScores.isEmpty()) return 0f
            return groupScores.sum().toFloat() / groupScores.size
        }

        val vnAvg = getGroupAverage(AssessmentGroup.VN.code)
        val ptAvg = getGroupAverage(AssessmentGroup.PT.code)
        val pbAvg = getGroupAverage(AssessmentGroup.PB.code)
        val ttAvg = getGroupAverage(AssessmentGroup.TT.code)

        return AssessmentResult(
            vnScore = (Math.round(vnAvg * 10f) / 10f),
            ptScore = (Math.round(ptAvg * 10f) / 10f),
            pbScore = (Math.round(pbAvg * 10f) / 10f),
            ttScore = (Math.round(ttAvg * 10f) / 10f),
            note = "Chỉ để tự theo dõi, không phải chẩn đoán"
        )
    }
}

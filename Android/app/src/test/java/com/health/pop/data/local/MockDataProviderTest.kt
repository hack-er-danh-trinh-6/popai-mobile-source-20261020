package com.health.pop.data.local

import com.health.pop.domain.model.AssessmentGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class MockDataProviderTest {

    @Test
    fun testAssessmentQuestions_has20QuestionsIn4Groups() {
        val questions = MockDataProvider.assessmentQuestions
        assertEquals(20, questions.size)

        assertEquals(5, questions.count { it.group == AssessmentGroup.VN })
        assertEquals(5, questions.count { it.group == AssessmentGroup.PT })
        assertEquals(5, questions.count { it.group == AssessmentGroup.PB })
        assertEquals(5, questions.count { it.group == AssessmentGroup.TT })
    }
}

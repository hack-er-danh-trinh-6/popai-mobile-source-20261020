package com.health.pop.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorParserTest {

    @Test
    fun classroomErrorMessage_explainsMissingRoute() {
        assertEquals(
            CLASSROOM_UNAVAILABLE_MESSAGE,
            classroomErrorMessage(404, API_ROUTE_NOT_FOUND_MESSAGE)
        )
    }

    @Test
    fun classroomErrorMessage_keepsBusinessNotFound() {
        assertEquals("Không tìm thấy lớp.", classroomErrorMessage(404, "Không tìm thấy lớp."))
    }

    @Test
    fun classroomErrorMessage_keepsOtherStatusCodes() {
        assertEquals(API_ROUTE_NOT_FOUND_MESSAGE, classroomErrorMessage(500, API_ROUTE_NOT_FOUND_MESSAGE))
        assertEquals("Vui lòng đăng nhập để tiếp tục.", classroomErrorMessage(401, "Vui lòng đăng nhập để tiếp tục."))
    }
}

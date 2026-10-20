package com.health.pop.data.remote

import org.json.JSONObject
import retrofit2.Response

fun Response<*>.apiErrorMessage(defaultMessage: String): String {
    val responseText = try {
        errorBody()?.string().orEmpty()
    } catch (_: Exception) {
        ""
    }
    return try {
        JSONObject(responseText).optString("message").takeIf { it.isNotBlank() } ?: defaultMessage
    } catch (_: Exception) {
        defaultMessage
    }
}

/** Thông báo 404 "không có route" mà máy chủ trả khi không khớp endpoint nào (khác 404 nghiệp vụ như "Không tìm thấy lớp."). */
const val API_ROUTE_NOT_FOUND_MESSAGE = "Không tìm thấy API."

const val CLASSROOM_UNAVAILABLE_MESSAGE =
    "Máy chủ chưa hỗ trợ tính năng lớp học. Vui lòng thử lại sau khi máy chủ được cập nhật."

/**
 * Chỉ đổi thông báo khi máy chủ thiếu hẳn route (404 + thông báo mặc định của máy chủ);
 * các lỗi 404 nghiệp vụ khác giữ nguyên nội dung gốc.
 */
fun classroomErrorMessage(code: Int, serverMessage: String): String =
    if (code == 404 && serverMessage == API_ROUTE_NOT_FOUND_MESSAGE) CLASSROOM_UNAVAILABLE_MESSAGE else serverMessage

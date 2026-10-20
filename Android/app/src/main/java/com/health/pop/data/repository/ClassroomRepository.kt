package com.health.pop.data.repository

import com.health.pop.core.network.ApiResult
import com.health.pop.core.network.RetrofitClient
import com.health.pop.core.security.TokenManager
import com.health.pop.data.remote.ClassMembershipDto
import com.health.pop.data.remote.ClassNotificationDto
import com.health.pop.data.remote.ClassroomApiService
import com.health.pop.data.remote.JoinClassRequestDto
import com.health.pop.data.remote.MarkReadRequestDto
import com.health.pop.data.remote.apiErrorMessage
import com.health.pop.data.remote.classroomErrorMessage
import com.health.pop.domain.model.ClassInbox
import com.health.pop.domain.model.ClassMembership
import com.health.pop.domain.model.ClassNotification
import com.health.pop.domain.model.ClassOption
import com.health.pop.domain.model.MembershipStatus
import com.health.pop.domain.model.School
import retrofit2.Response

class ClassroomRepository(private val tokenManager: TokenManager) {

    private val api: ClassroomApiService by lazy {
        RetrofitClient.getClient(tokenManager).create(ClassroomApiService::class.java)
    }

    private val networkMessage = "Không thể kết nối máy chủ. Vui lòng thử lại."

    suspend fun searchSchools(query: String): ApiResult<List<School>> = call("Không thể tải danh sách trường.") {
        val response = api.getSchools(query.trim())
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body.mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                School(id = id, name = dto.name.orEmpty(), classCount = dto.classCount ?: 0)
            })
        } else {
            response.classroomError("Không thể tải danh sách trường.")
        }
    }

    suspend fun getClasses(schoolId: String): ApiResult<List<ClassOption>> = call("Không thể tải danh sách lớp.") {
        val response = api.getClasses(schoolId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body.mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                ClassOption(id = id, name = dto.name.orEmpty(), teacherName = dto.teacherName.orEmpty())
            })
        } else {
            response.classroomError("Không thể tải danh sách lớp.")
        }
    }

    suspend fun getMembership(): ApiResult<ClassMembership> = call("Không thể tải trạng thái lớp học.") {
        val response = api.getMembership()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body.toDomain())
        } else {
            response.classroomError("Không thể tải trạng thái lớp học.")
        }
    }

    suspend fun requestJoin(classId: String): ApiResult<ClassMembership> = call("Không thể gửi yêu cầu vào lớp.") {
        val response = api.requestJoin(JoinClassRequestDto(classId))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body.toDomain())
        } else {
            response.classroomError("Không thể gửi yêu cầu vào lớp.")
        }
    }

    /** Huỷ yêu cầu đang chờ, rời lớp, hoặc xoá kết quả bị từ chối. */
    suspend fun leaveClass(): ApiResult<Unit> = call("Không thể thực hiện thao tác này.") {
        val response = api.leaveClass()
        if (response.isSuccessful) ApiResult.Success(Unit)
        else response.classroomError("Không thể thực hiện thao tác này.")
    }

    suspend fun getInbox(): ApiResult<ClassInbox> = call("Không thể tải thông báo.") {
        val response = api.getNotifications()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(
                ClassInbox(
                    unreadCount = body.unreadCount ?: 0,
                    items = body.items.orEmpty().mapNotNull { it.toDomain() }
                )
            )
        } else {
            response.classroomError("Không thể tải thông báo.")
        }
    }

    /** Lấy các thông báo chưa từng giao tới thiết bị (server đánh dấu đã giao) để hiện thông báo điện thoại. */
    suspend fun deliverPending(): ApiResult<List<ClassNotification>> = call("Không thể đồng bộ thông báo.") {
        val response = api.deliverNotifications()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            ApiResult.Success(body.mapNotNull { it.toDomain() })
        } else {
            response.classroomError("Không thể đồng bộ thông báo.")
        }
    }

    suspend fun markAllRead(): ApiResult<Unit> = call("Không thể cập nhật thông báo.") {
        val response = api.markRead(MarkReadRequestDto(all = true))
        if (response.isSuccessful) ApiResult.Success(Unit)
        else response.classroomError("Không thể cập nhật thông báo.")
    }

    private suspend fun <T> call(defaultError: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
        if (tokenManager.getAccessToken().isNullOrBlank()) return ApiResult.Error("Vui lòng đăng nhập để tiếp tục.", 401)
        return try {
            block()
        } catch (_: Exception) {
            ApiResult.Error(networkMessage)
        }
    }

    private fun Response<*>.classroomError(defaultMessage: String): ApiResult.Error {
        val statusCode = code()
        return ApiResult.Error(classroomErrorMessage(statusCode, apiErrorMessage(defaultMessage)), statusCode)
    }

    private fun ClassMembershipDto.toDomain() = ClassMembership(
        status = runCatching { MembershipStatus.valueOf(status.orEmpty()) }.getOrDefault(MembershipStatus.NONE),
        classId = classId.orEmpty(),
        className = className.orEmpty(),
        schoolName = schoolName.orEmpty(),
        teacherName = teacherName.orEmpty()
    )

    private fun ClassNotificationDto.toDomain(): ClassNotification? {
        val notificationId = id ?: return null
        return ClassNotification(
            id = notificationId,
            title = title.orEmpty(),
            body = body.orEmpty(),
            className = className.orEmpty(),
            senderName = senderName.orEmpty(),
            createdAt = createdAt.orEmpty(),
            read = read
        )
    }
}

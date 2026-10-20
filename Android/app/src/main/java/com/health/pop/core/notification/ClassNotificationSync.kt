package com.health.pop.core.notification

import android.content.Context
import com.health.pop.core.network.ApiResult
import com.health.pop.data.repository.ClassroomRepository

object ClassNotificationSync {

    /**
     * Hỏi máy chủ các thông báo mới của giáo viên và hiện chúng như thông báo điện thoại.
     * Không gọi máy chủ khi người dùng đã tắt thông báo, để không đánh dấu "đã giao" mà thực tế không hiện được.
     */
    suspend fun run(context: Context, repository: ClassroomRepository) {
        if (!ClassNotificationHelper.canNotify(context)) return
        val result = repository.deliverPending()
        if (result is ApiResult.Success) {
            ClassNotificationHelper.show(context, result.data)
        }
    }
}

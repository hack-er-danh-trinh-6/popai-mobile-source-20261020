package com.health.pop.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.health.pop.core.security.TokenManager
import com.health.pop.data.repository.ClassroomRepository
import java.util.concurrent.TimeUnit

/** Kiểm tra thông báo của giáo viên định kỳ khi ứng dụng không mở (tối thiểu 15 phút do giới hạn của WorkManager). */
class ClassNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tokenManager = try {
            TokenManager(applicationContext)
        } catch (_: Exception) {
            return Result.success()
        }
        if (tokenManager.getAccessToken().isNullOrBlank() || tokenManager.getUser()?.role != "STUDENT") {
            return Result.success()
        }
        ClassNotificationSync.run(applicationContext, ClassroomRepository(tokenManager))
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "pop_class_notifications"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ClassNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

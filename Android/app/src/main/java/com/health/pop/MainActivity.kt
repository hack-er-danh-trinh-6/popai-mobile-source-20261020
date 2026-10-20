package com.health.pop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.health.pop.core.designsystem.theme.POPAITheme
import com.health.pop.core.notification.ClassNotificationHelper
import com.health.pop.core.notification.ClassNotificationSync
import com.health.pop.core.notification.ClassNotificationWorker
import com.health.pop.core.security.TokenManager
import com.health.pop.data.repository.AIRepository
import com.health.pop.data.repository.AssessmentRepository
import com.health.pop.data.repository.AuthRepository
import com.health.pop.data.repository.ClassroomRepository
import com.health.pop.data.repository.FocusRepository
import com.health.pop.data.repository.JournalRepository
import com.health.pop.data.repository.PlanRepository
import com.health.pop.data.repository.ProgressRepository
import com.health.pop.ui.navigation.AppNavHost
import com.example.pop.ui.splash.PopSplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Đặt true khi người dùng chạm vào thông báo của giáo viên để mở màn Lớp học.
    private var openClassroomRequest by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* không cần xử lý thêm */ }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(ClassNotificationHelper.EXTRA_OPEN_CLASSROOM, false)) {
            openClassroomRequest = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openClassroomRequest = intent?.getBooleanExtra(ClassNotificationHelper.EXTRA_OPEN_CLASSROOM, false) == true

        val tokenManager = TokenManager(this)
        val authRepository = AuthRepository(tokenManager)
        val assessmentRepository = AssessmentRepository(tokenManager)
        val planRepository = PlanRepository(tokenManager)
        val focusRepository = FocusRepository(tokenManager)
        val journalRepository = JournalRepository(tokenManager)
        val progressRepository = ProgressRepository(tokenManager)
        val aiRepository = AIRepository(tokenManager, planRepository)
        val classroomRepository = ClassroomRepository(tokenManager)

        ClassNotificationHelper.ensureChannel(this)
        ClassNotificationWorker.schedule(this)
        // Khi app đang mở: hỏi quyền thông báo một lần cho học sinh và kiểm tra thông báo mới của giáo viên mỗi 30 giây.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var askedPermission = false
                while (true) {
                    if (tokenManager.getUser()?.role == "STUDENT") {
                        if (!askedPermission) {
                            askedPermission = true
                            requestNotificationPermissionIfNeeded()
                        }
                        ClassNotificationSync.run(applicationContext, classroomRepository)
                    }
                    delay(30_000)
                }
            }
        }

        setContent {
            POPAITheme {
                val navController = rememberNavController()
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    PopSplashScreen(onFinished = { showSplash = false })
                } else {
                    AppNavHost(
                        navController = navController,
                        authRepository = authRepository,
                        assessmentRepository = assessmentRepository,
                        planRepository = planRepository,
                        focusRepository = focusRepository,
                        journalRepository = journalRepository,
                        progressRepository = progressRepository,
                        aiRepository = aiRepository,
                        classroomRepository = classroomRepository,
                        openClassroomRequest = openClassroomRequest,
                        onOpenClassroomHandled = { openClassroomRequest = false }
                    )
                }
            }
        }
    }
}

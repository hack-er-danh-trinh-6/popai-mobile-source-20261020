package com.health.pop.ui.navigation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.health.pop.data.repository.AIRepository
import com.health.pop.data.repository.AssessmentRepository
import com.health.pop.data.repository.AuthRepository
import com.health.pop.data.repository.ClassroomRepository
import com.health.pop.data.repository.FocusRepository
import com.health.pop.data.repository.JournalRepository
import com.health.pop.data.repository.PlanRepository
import com.health.pop.data.repository.ProgressRepository
import com.health.pop.ui.ai.AIViewModel
import com.health.pop.ui.ai.POPAIChatScreen
import com.health.pop.ui.auth.AuthScreen
import com.health.pop.ui.auth.AuthViewModel
import com.health.pop.ui.classroom.ClassroomScreen
import com.health.pop.ui.classroom.ClassroomViewModel
import com.health.pop.ui.focus.FocusTimerScreen
import com.health.pop.ui.focus.FocusViewModel
import com.health.pop.ui.home.HomeViewModel
import com.health.pop.ui.home.TodayHomeScreen
import com.health.pop.ui.journal.JournalViewModel
import com.health.pop.ui.journal.QuickJournalScreen
import com.health.pop.ui.onboarding.LocalOnboarding
import com.health.pop.ui.onboarding.OnboardingState
import com.health.pop.ui.onboarding.OnboardingTour
import com.health.pop.ui.plan.PersonalPlanScreen
import com.health.pop.ui.plan.PlanViewModel
import com.health.pop.ui.prevention.AssessmentResultScreen
import com.health.pop.ui.prevention.AssessmentScreen
import com.health.pop.ui.prevention.PreventionScreen
import com.health.pop.ui.prevention.PreventionViewModel
import com.health.pop.ui.progress.ProgressScreen
import com.health.pop.ui.progress.ProgressViewModel

private val mainTabOrder = listOf(
    Screen.Home.route,
    Screen.Plan.route,
    Screen.Focus.route,
    Screen.Journal.route,
    Screen.Progress.route,
    Screen.AI.route
).withIndex().associate { (index, route) -> route to index }

private fun isMainTab(route: String?): Boolean = route in mainTabOrder

// Material "emphasized decelerate": rời nhanh, về đích mượt.
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** 1 nếu chuyển sang tab bên phải, -1 nếu sang tab bên trái, null nếu không phải chuyển giữa hai tab chính. */
private fun tabDirection(fromRoute: String?, toRoute: String?): Int? {
    val from = mainTabOrder[fromRoute] ?: return null
    val to = mainTabOrder[toRoute] ?: return null
    return if (to >= from) 1 else -1
}

// Fade-through so le: màn cũ mờ đi rất nhanh (90ms) rồi màn mới mới hiện lên (trễ 90ms).
// Nhờ độ trễ này, khung hình đầu tiên (lúc màn mới đang được dựng và thường bị chậm, nhất là bản debug)
// diễn ra khi màn mới còn trong suốt nên không bị cảm giác "giật cục" hay "nhảy cóc" hiệu ứng.
private fun tabEnterTransition(direction: Int): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(360, easing = EmphasizedDecelerate),
        initialOffsetX = { fullWidth -> direction * fullWidth / 6 }
    ) + fadeIn(animationSpec = tween(240, delayMillis = 90, easing = LinearOutSlowInEasing))

private fun tabExitTransition(direction: Int): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(160, easing = FastOutLinearInEasing),
        targetOffsetX = { fullWidth -> -direction * fullWidth / 12 }
    ) + fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing))

private fun screenEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(240, delayMillis = 90, easing = LinearOutSlowInEasing))

private fun screenExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(90, easing = FastOutLinearInEasing))

private class PopViewModelFactory<VM : ViewModel>(
    private val createViewModel: () -> VM
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = createViewModel()
        require(modelClass.isInstance(viewModel))
        return viewModel as T
    }
}

@Composable
private inline fun <reified VM : ViewModel> popViewModel(noinline create: () -> VM): VM =
    viewModel(factory = PopViewModelFactory(create))

/**
 * Chuyển sang một tab chính. Dùng chung cho thanh điều hướng dưới và các nút nhanh trên Home để
 * bấm đúp không tạo ra hai màn hình chồng nhau (launchSingleTop) và trạng thái tab được giữ/khôi phục đồng nhất.
 */
private fun NavHostController.navigateToMainTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Chạy [onResume] mỗi khi [owner] (ví dụ một NavBackStackEntry) quay lại trạng thái RESUMED. */
@Composable
private fun OnResumeEffect(owner: LifecycleOwner, onResume: () -> Unit) {
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    authRepository: AuthRepository,
    assessmentRepository: AssessmentRepository,
    planRepository: PlanRepository,
    focusRepository: FocusRepository,
    journalRepository: JournalRepository,
    progressRepository: ProgressRepository,
    aiRepository: AIRepository,
    classroomRepository: ClassroomRepository,
    openClassroomRequest: Boolean = false,
    onOpenClassroomHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser by authRepository.currentUser.collectAsState()
    val showBottomBar = currentRoute != Screen.Auth.route && currentUser != null

    // Trạng thái hướng dẫn riêng cho từng tài khoản: tài khoản mới đăng nhập lần đầu sẽ thấy buổi giới thiệu.
    val context = LocalContext.current
    val onboardingState = remember(currentUser?.id) {
        currentUser?.id?.let { userId ->
            OnboardingState(
                prefs = context.applicationContext.getSharedPreferences("pop_onboarding", Context.MODE_PRIVATE),
                userId = userId
            )
        }
    }

    // Repository là singleton nên sau khi đăng xuất (hoặc phiên hết hạn) phải xoá dữ liệu của tài khoản cũ,
    // nếu không tài khoản đăng nhập tiếp theo sẽ thấy kế hoạch/nhật ký cũ khi mạng lỗi.
    LaunchedEffect(currentUser?.id) {
        if (currentUser == null) {
            planRepository.reset()
            journalRepository.reset()
        }
    }

    // Học sinh vừa đăng ký: hỏi chọn trường và lớp một lần trước khi vào Home.
    var classSetupOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser, currentRoute) {
        if (currentUser != null && currentRoute == Screen.Auth.route) {
            val askClass = authRepository.consumeClassSetupRequest() && currentUser?.role == "STUDENT"
            classSetupOnboarding = askClass
            navController.navigate(if (askClass) Screen.Classroom.route else Screen.Home.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
                launchSingleTop = true
            }
        } else if (currentUser == null && currentRoute != null && currentRoute != Screen.Auth.route) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Người dùng chạm vào thông báo của giáo viên: mở màn Lớp học khi đã đăng nhập.
    LaunchedEffect(openClassroomRequest, currentUser, currentRoute) {
        if (openClassroomRequest && currentUser != null && currentRoute != null && currentRoute != Screen.Auth.route) {
            classSetupOnboarding = false
            navController.navigate(Screen.Classroom.route) { launchSingleTop = true }
            onOpenClassroomHandled()
        }
    }

    CompositionLocalProvider(LocalOnboarding provides onboardingState) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBottomBar) {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { screen -> navController.navigateToMainTab(screen.route) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = if (currentUser == null) Screen.Auth.route else Screen.Home.route,
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = {
                        val direction = tabDirection(initialState.destination.route, targetState.destination.route)
                        if (direction != null) tabEnterTransition(direction) else screenEnterTransition()
                    },
                    exitTransition = {
                        val direction = tabDirection(initialState.destination.route, targetState.destination.route)
                        if (direction != null) tabExitTransition(direction) else screenExitTransition()
                    }
                ) {
                    composable(Screen.Auth.route) {
                        val vm = popViewModel { AuthViewModel(authRepository) }
                        AuthScreen(viewModel = vm, onLoginSuccess = {})
                    }
                    composable(Screen.Home.route) { backStackEntry ->
                        val vm = popViewModel {
                            HomeViewModel(authRepository, planRepository, journalRepository, progressRepository)
                        }
                        // ViewModel của Home được giữ lại khi chuyển tab, nên phải tải lại mỗi khi quay về Home
                        // (sau phiên tập trung, lưu nhật ký/kế hoạch, qua ngày mới, hoặc app quay lại foreground).
                        OnResumeEffect(backStackEntry) { vm.refresh() }
                        TodayHomeScreen(
                            viewModel = vm,
                            onNavigateToFocus = { navController.navigateToMainTab(Screen.Focus.route) },
                            onNavigateToJournal = { navController.navigateToMainTab(Screen.Journal.route) },
                            onNavigateToAssessment = {
                                navController.navigate(Screen.Prevention.route) { launchSingleTop = true }
                            },
                            onNavigateToPlan = { navController.navigateToMainTab(Screen.Plan.route) },
                            onNavigateToClassroom = {
                                classSetupOnboarding = false
                                navController.navigate(Screen.Classroom.route) { launchSingleTop = true }
                            },
                            onShowGuide = { onboardingState?.openTour() }
                        )
                    }
                    composable(Screen.Classroom.route) {
                        val vm = popViewModel { ClassroomViewModel(classroomRepository) }
                        ClassroomScreen(
                            viewModel = vm,
                            isOnboarding = classSetupOnboarding,
                            onClose = {
                                classSetupOnboarding = false
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(Screen.Prevention.route) {
                        val vm = popViewModel { PreventionViewModel(assessmentRepository) }
                        PreventionScreen(viewModel = vm, onStartAssessment = { navController.navigate(Screen.Assessment.route) })
                    }
                    composable(Screen.Assessment.route) {
                        val vm = popViewModel { PreventionViewModel(assessmentRepository) }
                        AssessmentScreen(viewModel = vm, onSubmitted = { navController.navigate(Screen.AssessmentResult.route) })
                    }
                    composable(Screen.AssessmentResult.route) {
                        val vm = popViewModel { PreventionViewModel(assessmentRepository) }
                        AssessmentResultScreen(
                            viewModel = vm,
                            onDone = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Plan.route) {
                        val vm = popViewModel { PlanViewModel(planRepository) }
                        PersonalPlanScreen(viewModel = vm)
                    }
                    composable(Screen.Focus.route) {
                        val plan = planRepository.currentPlan.value
                        val vm = popViewModel {
                            FocusViewModel(
                                repository = focusRepository,
                                defaultTargetMinutes = plan.sessionDurationMinutes,
                                defaultTaskName = plan.studyTasks.firstOrNull().orEmpty()
                            )
                        }
                        FocusTimerScreen(viewModel = vm)
                    }
                    composable(Screen.Journal.route) {
                        val vm = popViewModel { JournalViewModel(journalRepository) }
                        QuickJournalScreen(viewModel = vm)
                    }
                    composable(Screen.Progress.route) {
                        val vm = popViewModel { ProgressViewModel(progressRepository) }
                        ProgressScreen(viewModel = vm)
                    }
                    composable(Screen.AI.route) {
                        val vm = popViewModel { AIViewModel(aiRepository) }
                        POPAIChatScreen(viewModel = vm)
                    }
                }
            }

            if (onboardingState != null) {
                AnimatedVisibility(
                    visible = onboardingState.isTourVisible && currentRoute != Screen.Auth.route,
                    enter = fadeIn(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(220))
                ) {
                    OnboardingTour(onFinish = onboardingState::finishTour)
                }
            }
        }
    }
}

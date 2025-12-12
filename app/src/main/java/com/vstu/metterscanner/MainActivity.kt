package com.vstu.metterscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vstu.metterscanner.data.MeterRepository
import com.vstu.metterscanner.ui.screens.*
import com.vstu.metterscanner.ui.theme.MetterScannerTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        MeterRepository(applicationContext)
    }

    private val viewModel: MeterViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MeterViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MeterViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            MetterScannerTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController, viewModel = viewModel)
            }
        }
    }

}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: MeterViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_SCREEN
    ) {
        // Главный экран
        composable(Routes.MAIN_SCREEN) {
            MainScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // Добавить показания
        composable(Routes.ADD_METER_SCREEN) {
            AddMeterScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // История
        composable(Routes.HISTORY_SCREEN) {
            HistoryScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // Статистика
        composable(Routes.STATISTICS_SCREEN) {
            StatisticsScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // Настройки
        composable(Routes.SETTINGS_SCREEN) {
            SettingsScreen(navController = navController)
        }

        // Помощь
        composable(Routes.HELP_SCREEN) {
            HelpScreen(navController = navController)
        }

        // О приложении
        composable(Routes.ABOUT_SCREEN) {
            AboutScreen(navController = navController)
        }

        // Сканирование камерой
        composable(Routes.CAMERA_SCAN_SCREEN) {
            CameraScanScreen(
                onResult = { scannedValue, photoPath ->
                    // Возвращаемся с результатами
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "scannedValue", scannedValue
                    )
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "photoPath", photoPath
                    )
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Предпросмотр фото
        composable(
            route = Routes.PHOTO_PREVIEW_SCREEN,
            arguments = listOf(
                navArgument("photoPath") { type = androidx.navigation.NavType.StringType },
                navArgument("recognizedValue") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val photoPath = backStackEntry.arguments?.getString("photoPath") ?: ""
            val recognizedValue = backStackEntry.arguments?.getString("recognizedValue") ?: ""

            PhotoPreviewScreen(
                photoPath = photoPath,
                onConfirm = {
                    navController.popBackStack()
                },
                onRetake = {
                    navController.popBackStack()
                },
                recognizedValue = recognizedValue,
                viewModel = viewModel
            )
        }
    }
}

// Объект с маршрутами для навигации
object Routes {
    // Основные экраны
    const val MAIN_SCREEN = "main_screen"
    const val ADD_METER_SCREEN = "add_meter_screen"
    const val HISTORY_SCREEN = "history_screen"
    const val STATISTICS_SCREEN = "statistics_screen"
    const val SETTINGS_SCREEN = "settings_screen"
    const val HELP_SCREEN = "help_screen"
    const val ABOUT_SCREEN = "about_screen"
    const val CAMERA_SCAN_SCREEN = "camera_scan_screen"

    // Экран с параметрами
    const val PHOTO_PREVIEW_SCREEN = "photo_preview_screen/{photoPath}/{recognizedValue}"

    // Функции для создания маршрутов с параметрами
    fun createPhotoPreviewRoute(photoPath: String, recognizedValue: String): String {
        return "photo_preview_screen/$photoPath/$recognizedValue"
    }
}
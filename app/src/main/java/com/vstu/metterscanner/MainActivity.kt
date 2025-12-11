package com.vstu.metterscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vstu.metterscanner.data.MeterRepository
import com.vstu.metterscanner.ui.screens.AddMeterScreen
import com.vstu.metterscanner.ui.screens.MainScreen
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

                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                    composable("add") {
                        AddMeterScreen(
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}
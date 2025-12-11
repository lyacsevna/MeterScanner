package com.vstu.metterscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vstu.metterscanner.data.MeterRepository
import com.vstu.metterscanner.ui.screens.AddMeterScreen
import com.vstu.metterscanner.ui.screens.MainScreen
import com.vstu.metterscanner.ui.theme.MetterScannerTheme

class MainActivity : ComponentActivity() {
    private val repository = MeterRepository()

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
                        MainScreen(repository, navController)
                    }
                    composable("add") {
                        AddMeterScreen(repository, navController)
                    }
                }
            }
        }
    }
}
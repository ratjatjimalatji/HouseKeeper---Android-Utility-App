package de.yanneckreiss.mlkittutorial

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.yanneckreiss.mlkittutorial.entry.data.TaskViewModel.EntryViewModel
import de.yanneckreiss.mlkittutorial.gas.ui.GasCylinderTrackerScreen
import de.yanneckreiss.mlkittutorial.reports.ui.ReportsScreen
import de.yanneckreiss.mlkittutorial.ui.MainScreen

object AppRoutes {
    const val METER_SELECTION = "meter_selection"
    const val ELECTRICITY_CAMERA = "electricity_camera"
    const val WATER_METER = "water_meter"
    const val GAS_METER = "gas_meter"
    const val ENTRIES_HISTORY = "entries_history"
    const val REPORTS = "reports"
}

@SuppressLint("ComposeViewModelInjection")
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavHost(viewModel: EntryViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.METER_SELECTION
    ) {
        // Selection Screen (Home Dashboard)
        composable(AppRoutes.METER_SELECTION) {
            MeterSelectionScreen(
                onNavigateToElectricity = {
                    navController.navigate(AppRoutes.ELECTRICITY_CAMERA)
                },
                onNavigateToWater = {
                    navController.navigate(AppRoutes.WATER_METER)
                },
                onNavigateToGas = {
                    navController.navigate(AppRoutes.GAS_METER)
                },
                onNavigateToHistory = {
                    navController.navigate(AppRoutes.REPORTS)
                },
                onQuickLog = {
                    navController.navigate(AppRoutes.REPORTS)
                }
            )
        }

        // Electricity Camera Scanner Screen (CameraX + OCR)
        composable(AppRoutes.ELECTRICITY_CAMERA) {
            MainScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Water Tracker Screen
        composable(AppRoutes.WATER_METER) {
            // TO-DO: Water Tracker Screen
        }

        // Gas Cylinder & Meter Logging Screen
        composable(AppRoutes.GAS_METER) {
            GasCylinderTrackerScreen(
                entryViewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Utility Reports & Trends Screen
        composable(AppRoutes.REPORTS) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.ENTRIES_HISTORY) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

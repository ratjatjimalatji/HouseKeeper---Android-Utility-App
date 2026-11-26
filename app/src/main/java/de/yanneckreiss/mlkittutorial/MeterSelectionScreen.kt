package de.yanneckreiss.mlkittutorial

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposeModifierMissing", "ComposeViewModelInjection")
@Composable
fun MeterSelectionScreen(
    onNavigateToElectricity: () -> Unit,
    onNavigateToWater: () -> Unit,
    onNavigateToGas: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onQuickLog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text("House Check", fontWeight = FontWeight.Bold)
                        Text(
                            "Utility Tracking & Analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "OCR & App Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Electricity Card
            MeterNavigationCard(
                icon = Icons.Default.FlashOn,
                color = Color(0xFFFFA500),
                header = "Log Electricity Meter",
                description = "Scan meter camera view or enter kilowatt-hour (kWh) readings.",
                onClick = onNavigateToElectricity
            )

            // Water Card
            MeterNavigationCard(
                icon = Icons.Default.WaterDrop,
                color = Color.Cyan,
                header = "Log Water Meter",
                description = "Snap water meter photo & input reading to estimate usage.",
                onClick = onNavigateToWater
            )

            // Gas Cylinder Card
            MeterNavigationCard(
                icon = Icons.Default.LocalFireDepartment,
                color = Color.Red,
                header = "Log propane cylinder exchanges",
                description = "Track propane cylinder exchanges & annual gas usage.",
                onClick = onNavigateToGas
            )

            // Reports & Analytics Card
            MeterNavigationCard(
                icon = Icons.Default.BarChart,
                color = Color.Blue,
                header = "Reports",
                description = "Find utility patterns and usage reports.",
                onClick = onNavigateToHistory
            )
        }
    }
}

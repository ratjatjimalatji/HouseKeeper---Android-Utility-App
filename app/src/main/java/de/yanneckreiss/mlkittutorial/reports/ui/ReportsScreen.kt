package de.yanneckreiss.mlkittutorial.reports.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.yanneckreiss.mlkittutorial.reports.domain.model.ReportGraphPoint
import de.yanneckreiss.mlkittutorial.reports.ui.components.MonthSelectorRow
import de.yanneckreiss.mlkittutorial.reports.ui.components.ReportsLineGraph
import de.yanneckreiss.mlkittutorial.reports.ui.components.ReportsProjectionCards
import de.yanneckreiss.mlkittutorial.reports.ui.components.UtilityToggleRow
import de.yanneckreiss.mlkittutorial.reports.viewmodel.ReportsViewModel
import java.util.Locale

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Usage Reports & Trends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${uiState.selectedUtility.displayName} • ${uiState.selectedMonth.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = uiState.selectedUtility.composeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${uiState.loggedReadingsCount} Logged",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ========================================================
            // 1. UTILITY TOGGLE ROW (GAS, ELECTRICITY, PROPANE)
            // ========================================================
            item {
                UtilityToggleRow(
                    selectedUtility = uiState.selectedUtility,
                    onSelectUtility = { viewModel.setUtilityFilter(it) }
                )
            }

            // ========================================================
            // 2. HISTORICAL MONTH SELECTOR ROW
            // ========================================================
            item {
                MonthSelectorRow(
                    selectedMonth = uiState.selectedMonth,
                    availableMonths = uiState.availableMonths,
                    onSelectMonth = { viewModel.setSelectedMonth(it) }
                )
            }

            // ========================================================
            // 3. WEEKLY & MONTHLY USAGE PROJECTION CARDS
            // ========================================================
            item {
                ReportsProjectionCards(
                    weeklyProjection = uiState.weeklyProjection,
                    monthlyProjection = uiState.monthlyProjection,
                    utilityType = uiState.selectedUtility
                )
            }

            // ========================================================
            // 4. INTERACTIVE LINE GRAPH (SOLID DOTS FOR LOGGED, DOTTED LINES FOR TRENDS)
            // ========================================================
            item {
                ReportsLineGraph(
                    graphPoints = uiState.graphPoints,
                    utilityType = uiState.selectedUtility
                )
            }

            // ========================================================
            // 5. LOGGED READINGS BREAKDOWN FOR SELECTED MONTH
            // ========================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Readings in ${uiState.selectedMonth.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.loggedReadingsCount} records",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val loggedPoints = uiState.graphPoints.filter { it.isLoggedReading }
            if (loggedPoints.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No readings logged in ${uiState.selectedMonth.displayName}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = loggedPoints,
                    key = { index, point -> "${point.date}_$index" }
                ) { _, point ->
                    LoggedReadingReportCard(
                        point = point,
                        utilityUnit = uiState.selectedUtility.defaultUnit,
                        utilityColor = uiState.selectedUtility.composeColor
                    )
                }
            }
        }
    }
}

/**
 * Card displaying an individual logged reading record in the report.
 */
@Composable
private fun LoggedReadingReportCard(
    point: ReportGraphPoint,
    utilityUnit: String,
    utilityColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(utilityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = utilityColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = point.dayLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = utilityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Logged",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = utilityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "Usage: ${String.format(Locale.US, "%.1f", point.value)} $utilityUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (point.meterReading != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Meter Reading",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f", point.meterReading),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

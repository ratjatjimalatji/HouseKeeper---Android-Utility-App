package de.yanneckreiss.mlkittutorial.reports.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import de.yanneckreiss.mlkittutorial.entry.domain.model.TrendDirection
import de.yanneckreiss.mlkittutorial.reports.domain.model.MonthlyProjection
import de.yanneckreiss.mlkittutorial.reports.domain.model.WeeklyProjection
import java.util.Locale

/**
 * Cards displaying trends analysis projecting weekly and monthly usage.
 */
@Composable
fun ReportsProjectionCards(
    weeklyProjection: WeeklyProjection,
    monthlyProjection: MonthlyProjection,
    utilityType: UtilityType,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. WEEKLY PROJECTION CARD
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Weekly Trend",
                        tint = utilityType.composeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Weekly Projected",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.1f", weeklyProjection.projectedUnits)} ${utilityType.defaultUnit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Burn rate: ${String.format(Locale.US, "%.1f", weeklyProjection.dailyBurnRate)} ${utilityType.defaultUnit}/day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Trend badge
                TrendIndicatorBadge(
                    percentage = weeklyProjection.trendPercentage,
                    direction = weeklyProjection.trendDirection,
                    label = weeklyProjection.comparisonLabel
                )
            }
        }

        // 2. MONTHLY PROJECTION CARD
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = "Monthly Trend",
                        tint = utilityType.composeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Monthly Projected",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.1f", monthlyProjection.projectedMonthEndUnits)} ${utilityType.defaultUnit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Logged: ${String.format(Locale.US, "%.1f", monthlyProjection.actualUnitsSoFar)} ${utilityType.defaultUnit} (${monthlyProjection.daysPassed}d)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Trend badge
                TrendIndicatorBadge(
                    percentage = monthlyProjection.trendPercentage,
                    direction = monthlyProjection.trendDirection,
                    label = monthlyProjection.comparisonLabel
                )
            }
        }
    }
}

@Composable
private fun TrendIndicatorBadge(
    percentage: Double,
    direction: TrendDirection,
    label: String
) {
    val isUp = direction == TrendDirection.INCREASING
    val badgeColor = if (isUp) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val textColor = if (isUp) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)
    val icon = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", percentage)}% $label",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

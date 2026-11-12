package de.yanneckreiss.mlkittutorial.reports.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType

/**
 * Filter bar allowing users to toggle between Gas, Electricity, and Propane reports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityToggleRow(
    selectedUtility: UtilityType,
    onSelectUtility: (UtilityType) -> Unit,
    modifier: Modifier = Modifier
) {
    val reportUtilities = listOf(
        UtilityType.GAS,
        UtilityType.ELECTRICITY,
        UtilityType.PROPANE
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            reportUtilities.forEach { utility ->
                val isSelected = utility == selectedUtility
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectUtility(utility) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = utility.emoji)
                            Text(
                                text = utility.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = utility.composeColor.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = utility.composeColor,
                        selectedBorderWidth = 1.8.dp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

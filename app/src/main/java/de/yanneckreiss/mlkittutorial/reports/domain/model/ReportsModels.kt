package de.yanneckreiss.mlkittutorial.reports.domain.model

import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import de.yanneckreiss.mlkittutorial.entry.domain.model.TrendDirection
import kotlinx.datetime.LocalDate

data class MonthOption(
    val monthKey: String, // e.g. "2026-10"
    val displayName: String, // e.g. "October 2026"
    val year: Int,
    val month: Int,
    val isCurrentMonth: Boolean = false
)

data class ReportGraphPoint(
    val date: LocalDate,
    val dayLabel: String,
    val value: Double, // Units consumed
    val meterReading: Double? = null,
    val isLoggedReading: Boolean = false, // True if user manually logged a reading on this exact date
    val isProjected: Boolean = false, // True for future days in the month
    val isMeasured: Boolean = false, // True if usage for this day was derived from two consecutive meter readings
    val weatherImpactScore: Double = 0.0,
    val notes: String = ""
)

data class WeeklyProjection(
    val projectedUnits: Double,
    val dailyBurnRate: Double,
    val trendPercentage: Double,
    val trendDirection: TrendDirection,
    val comparisonLabel: String = "vs previous week"
)

data class MonthlyProjection(
    val projectedMonthEndUnits: Double,
    val actualUnitsSoFar: Double,
    val daysPassed: Int,
    val daysRemaining: Int,
    val trendPercentage: Double,
    val trendDirection: TrendDirection,
    val comparisonLabel: String = "vs previous month"
)

data class ReportsUiState(
    val selectedUtility: UtilityType = UtilityType.GAS,
    val selectedMonth: MonthOption = MonthOption("2026-10", "October 2026", 2026, 10, true),
    val availableMonths: List<MonthOption> = emptyList(),
    val weeklyProjection: WeeklyProjection = WeeklyProjection(0.0, 0.0, 0.0, TrendDirection.STABLE),
    val monthlyProjection: MonthlyProjection = MonthlyProjection(0.0, 0.0, 0, 0, 0.0, TrendDirection.STABLE),
    val graphPoints: List<ReportGraphPoint> = emptyList(),
    val loggedReadingsCount: Int = 0,
    val averageDailyUnits: Double = 0.0,
    val peakDayUnits: Double = 0.0,
    val isLoading: Boolean = false
)

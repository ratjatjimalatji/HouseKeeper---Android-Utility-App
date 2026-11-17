package de.yanneckreiss.mlkittutorial.reports.domain.util

import de.yanneckreiss.mlkittutorial.entry.data.local.Entry
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import de.yanneckreiss.mlkittutorial.entry.domain.model.TrendDirection
import de.yanneckreiss.mlkittutorial.reports.domain.model.MonthOption
import de.yanneckreiss.mlkittutorial.reports.domain.model.MonthlyProjection
import de.yanneckreiss.mlkittutorial.reports.domain.model.ReportGraphPoint
import de.yanneckreiss.mlkittutorial.reports.domain.model.WeeklyProjection
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Domain Service responsible for turning logged meter readings into daily usage,
 * and calculating weekly/monthly projections from that usage.
 *
 * A logged entry is a METER READING, not a purchase. Usage is derived from the
 * difference between two consecutive readings, spread evenly over the days between them:
 *
 *   Day 1 reading = 140 kWh, Day 5 reading = 90 kWh
 *   -> 50 kWh used over 4 days -> 12.5 kWh/day for days 2, 3, 4 and 5.
 *
 * The absolute difference is used so both count-down (prepaid) and count-up meters work.
 */
@Singleton
class TrendsProjectionEngine @Inject constructor() {

    private data class Reading(val date: LocalDate, val value: Double)

    /** One reading per date (the latest logged that day), sorted oldest first. */
    private fun extractReadings(entries: List<Entry>, utilityType: UtilityType): List<Reading> =
        entries
            .asSequence()
            .filter { it.detectedUtilityType == utilityType && it.meterReading > 0.0 }
            .mapNotNull { entry ->
                runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { entry to it }
            }
            .sortedWith(compareBy({ it.second }, { it.first.timestamp }, { it.first.id }))
            .toList()
            .associate { (entry, date) -> date to entry.meterReading } // last reading of a day wins
            .map { (date, value) -> Reading(date, value) }

    /**
     * Daily usage for every day covered by two consecutive readings.
     * The days after the earlier reading up to and including the later reading
     * each receive (difference / days elapsed).
     */
    private fun buildDailyUsage(readings: List<Reading>): Map<LocalDate, Double> {
        val usage = mutableMapOf<LocalDate, Double>()
        readings.zipWithNext { previous, current ->
            val daysElapsed = previous.date.daysUntil(current.date)
            if (daysElapsed <= 0) return@zipWithNext
            val perDay = abs(current.value - previous.value) / daysElapsed
            var day = previous.date.plus(1, DateTimeUnit.DAY)
            while (day <= current.date) {
                usage[day] = perDay
                day = day.plus(1, DateTimeUnit.DAY)
            }
        }
        return usage
    }

    /** Average measured daily usage over the most recent [window] measured days up to [upTo]. */
    private fun recentMeasuredRate(
        dailyUsage: Map<LocalDate, Double>,
        upTo: LocalDate,
        window: Int = 30
    ): Double? = dailyUsage
        .filterKeys { it <= upTo }
        .toSortedMap()
        .values
        .toList()
        .takeLast(window)
        .takeIf { it.isNotEmpty() }
        ?.average()

    fun generateReportGraphPoints(
        monthOption: MonthOption,
        utilityType: UtilityType,
        entries: List<Entry>
    ): List<ReportGraphPoint> {
        val daysInMonth = getDaysInMonth(monthOption.year, monthOption.month)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Readings from ALL months are used so the first reading of this month can be
        // paired with the last reading of the previous month.
        val readings = extractReadings(entries, utilityType)
        val readingsByDate = readings.associate { it.date to it.value }
        val dailyUsage = buildDailyUsage(readings)

        val monthEnd = LocalDate(monthOption.year, monthOption.month, daysInMonth)
        val monthStart = LocalDate(monthOption.year, monthOption.month, 1)
        val measuredThisMonth = dailyUsage.filterKeys { it in monthStart..monthEnd }.values

        // Rate used to fill days without measured usage (future days, or days after the latest reading)
        val fillRate = measuredThisMonth.takeIf { it.isNotEmpty() }?.average()
            ?: recentMeasuredRate(dailyUsage, monthEnd)
            ?: getDefaultDailyRate(utilityType)

        return (1..daysInMonth).map { day ->
            val date = LocalDate(monthOption.year, monthOption.month, day)
            val dayLabel = "${getMonthShortName(monthOption.month)} $day"
            val reading = readingsByDate[date]
            val measured = dailyUsage[date]
            val entry = entries.lastOrNull { it.date == date.toString() && it.detectedUtilityType == utilityType }

            when {
                measured != null -> ReportGraphPoint(
                    date = date,
                    dayLabel = dayLabel,
                    value = measured,
                    meterReading = reading,
                    isLoggedReading = reading != null,
                    isProjected = false,
                    isMeasured = true,
                    weatherImpactScore = entry?.weatherScore ?: 20.0,
                    notes = if (reading != null) entry?.description.orEmpty() else "Interpolated between readings"
                )

                reading != null -> ReportGraphPoint(
                    // First reading on record: no earlier reading to measure usage against yet.
                    date = date,
                    dayLabel = dayLabel,
                    value = fillRate,
                    meterReading = reading,
                    isLoggedReading = true,
                    isProjected = false,
                    isMeasured = false,
                    weatherImpactScore = entry?.weatherScore ?: 20.0,
                    notes = "First reading (usage estimated)"
                )

                else -> {
                    val isFuture = date > today
                    ReportGraphPoint(
                        date = date,
                        dayLabel = dayLabel,
                        value = fillRate,
                        meterReading = null,
                        isLoggedReading = false,
                        isProjected = isFuture,
                        isMeasured = false,
                        weatherImpactScore = 20.0,
                        notes = if (isFuture) "Projected" else "Estimated (awaiting next reading)"
                    )
                }
            }
        }
    }

    /**
     * Total usage for a whole month: measured usage plus the estimated fill for days
     * not yet covered by readings. Returns null when no usage could be measured that month.
     */
    fun calculateMonthTotal(
        year: Int,
        month: Int,
        utilityType: UtilityType,
        entries: List<Entry>
    ): Double? {
        val points = generateReportGraphPoints(
            MonthOption(monthKey = "", displayName = "", year = year, month = month),
            utilityType,
            entries
        )
        return if (points.any { it.isMeasured }) points.sumOf { it.value } else null
    }

    fun calculateWeeklyProjection(
        graphPoints: List<ReportGraphPoint>,
        utilityType: UtilityType
    ): WeeklyProjection {
        val measuredUsage = graphPoints.filter { it.isMeasured }.sortedBy { it.date }.map { it.value }

        // Daily rate = average measured usage over the last 7 measured days
        val recentWeek = measuredUsage.takeLast(7)
        val previousWeek = measuredUsage.dropLast(7).takeLast(7)

        val dailyRate = recentWeek.takeIf { it.isNotEmpty() }?.average()
            ?: graphPoints.firstOrNull()?.value
            ?: getDefaultDailyRate(utilityType)

        val previousRate = previousWeek.takeIf { it.isNotEmpty() }?.average()
        val percentageChange = if (previousRate != null && previousRate > 0.0) {
            ((dailyRate - previousRate) / previousRate) * 100.0
        } else {
            0.0
        }

        return WeeklyProjection(
            projectedUnits = dailyRate * 7.0,
            dailyBurnRate = dailyRate,
            trendPercentage = abs(percentageChange),
            trendDirection = trendDirectionFor(percentageChange),
            comparisonLabel = if (previousRate != null) "vs previous week" else "Log more readings to see a trend"
        )
    }

    fun calculateMonthlyProjection(
        graphPoints: List<ReportGraphPoint>,
        utilityType: UtilityType,
        previousMonthUnits: Double
    ): MonthlyProjection {
        // Usage actually measured from readings so far this month
        val measuredPoints = graphPoints.filter { it.isMeasured }
        val actualUnitsSoFar = measuredPoints.sumOf { it.value }
        val measuredDays = measuredPoints.size
        val remainingDays = graphPoints.size - measuredDays

        val avgDailyRate = if (measuredDays > 0) {
            actualUnitsSoFar / measuredDays
        } else {
            graphPoints.firstOrNull()?.value ?: getDefaultDailyRate(utilityType)
        }

        // Month-end total = measured so far + measured daily rate for every day not yet covered by readings
        val projectedMonthEndUnits = actualUnitsSoFar + (avgDailyRate * remainingDays)

        val percentageChange = if (previousMonthUnits > 0) {
            ((projectedMonthEndUnits - previousMonthUnits) / previousMonthUnits) * 100.0
        } else {
            0.0
        }

        return MonthlyProjection(
            projectedMonthEndUnits = projectedMonthEndUnits,
            actualUnitsSoFar = actualUnitsSoFar,
            daysPassed = measuredDays,
            daysRemaining = remainingDays,
            trendPercentage = abs(percentageChange),
            trendDirection = trendDirectionFor(percentageChange),
            comparisonLabel = "vs last month (${String.format(Locale.US, "%.1f", previousMonthUnits)} ${utilityType.defaultUnit})"
        )
    }

    private fun trendDirectionFor(percentageChange: Double): TrendDirection = when {
        percentageChange > 2.0 -> TrendDirection.INCREASING
        percentageChange < -2.0 -> TrendDirection.DECREASING
        else -> TrendDirection.STABLE
    }

    private fun getDefaultDailyRate(utilityType: UtilityType): Double {
        return when (utilityType) {
            UtilityType.ELECTRICITY -> 14.5 // kWh/day
            UtilityType.GAS -> 2.8 // m³/day
            UtilityType.PROPANE -> 0.6 // kg/day
            UtilityType.WATER -> 220.0 // L/day
        }
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 30
        }
    }

    private fun getMonthShortName(month: Int): String {
        return when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Month"
        }
    }
}

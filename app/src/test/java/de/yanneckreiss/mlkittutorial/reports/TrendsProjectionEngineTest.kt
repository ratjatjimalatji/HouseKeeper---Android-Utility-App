package de.yanneckreiss.mlkittutorial.reports

import de.yanneckreiss.mlkittutorial.entry.data.local.Entry
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import de.yanneckreiss.mlkittutorial.reports.domain.model.MonthOption
import de.yanneckreiss.mlkittutorial.reports.domain.util.TrendsProjectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendsProjectionEngineTest {

    private val engine = TrendsProjectionEngine()
    private val month = MonthOption("2025-03", "March 2025", 2025, 3, false)

    private fun reading(id: Int, date: String, value: Double) = Entry(
        id = id, date = date, monthKey = date.take(7), meterReading = value, description = "Electricity [kWh]"
    )

    @Test
    fun usageIsSpreadEvenlyBetweenTwoReadings() {
        val entries = listOf(reading(1, "2025-03-01", 140.0), reading(2, "2025-03-05", 90.0))
        val points = engine.generateReportGraphPoints(month, UtilityType.ELECTRICITY, entries)

        // 50 kWh over 4 days -> 12.5 kWh/day on days 2..5
        (2..5).forEach { day ->
            val p = points[day - 1]
            assertEquals(12.5, p.value, 1e-9)
            assertTrue(p.isMeasured)
        }
        assertTrue(points[4].isLoggedReading)
        assertFalse(points[0].isMeasured) // first reading has nothing to compare against

        val monthly = engine.calculateMonthlyProjection(points, UtilityType.ELECTRICITY, 300.0)
        assertEquals(50.0, monthly.actualUnitsSoFar, 1e-9)
        assertEquals(4, monthly.daysPassed)
        assertEquals(12.5 * 31, monthly.projectedMonthEndUnits, 1e-9)

        val weekly = engine.calculateWeeklyProjection(points, UtilityType.ELECTRICITY)
        assertEquals(12.5, weekly.dailyBurnRate, 1e-9)
        assertEquals(87.5, weekly.projectedUnits, 1e-9)
    }

    @Test
    fun readingFromPreviousMonthBridgesIntoThisMonth() {
        val entries = listOf(reading(1, "2025-02-27", 100.0), reading(2, "2025-03-03", 140.0))
        val points = engine.generateReportGraphPoints(month, UtilityType.ELECTRICITY, entries)
        // 40 units over 4 days (Feb 28, Mar 1, 2, 3) -> 10/day, Mar 1..3 measured
        (1..3).forEach { assertEquals(10.0, points[it - 1].value, 1e-9) }
        assertEquals(30.0, engine.calculateMonthlyProjection(points, UtilityType.ELECTRICITY, 0.0).actualUnitsSoFar, 1e-9)
    }
}

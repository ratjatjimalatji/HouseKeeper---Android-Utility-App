package de.yanneckreiss.mlkittutorial.reports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.yanneckreiss.mlkittutorial.entry.data.local.Entry
import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import de.yanneckreiss.mlkittutorial.entry.domain.repository.IEntryRepository
import de.yanneckreiss.mlkittutorial.gas.data.GasCylinderRepository
import de.yanneckreiss.mlkittutorial.reports.domain.model.MonthOption
import de.yanneckreiss.mlkittutorial.reports.domain.model.ReportsUiState
import de.yanneckreiss.mlkittutorial.reports.domain.util.TrendsProjectionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val entryRepository: IEntryRepository,
    private val gasRepository: GasCylinderRepository,
    private val projectionEngine: TrendsProjectionEngine
) : ViewModel() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Past 6 months list
    val availableMonths: List<MonthOption> = createAvailableMonthsList(today.year, today.monthNumber)

    private val _selectedUtility = MutableStateFlow(UtilityType.GAS)
    val selectedUtility: StateFlow<UtilityType> = _selectedUtility.asStateFlow()

    private val _selectedMonth = MutableStateFlow(availableMonths.first())
    val selectedMonth: MonthOption
        get() = _selectedMonth.value

    private val _rawEntries = MutableStateFlow<List<Entry>>(emptyList())

    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedUtility,
        _selectedMonth,
        _rawEntries
    ) { utility, month, entries ->
        val filteredEntries = entries.filter { entry ->
            entry.detectedUtilityType == utility
        }

        // Generate Graph Points (Logged readings as solid dots, trends between as dotted lines)
        val graphPoints = projectionEngine.generateReportGraphPoints(
            monthOption = month,
            utilityType = utility,
            entries = filteredEntries
        )

        // Previous month usage, derived from the difference between meter readings
        val (prevYear, prevMonth) = getPreviousMonth(month.year, month.month)
        val prevMonthTotal = projectionEngine.calculateMonthTotal(prevYear, prevMonth, utility, filteredEntries)
            ?: getDefaultPreviousMonthUnits(utility)

        val weeklyProj = projectionEngine.calculateWeeklyProjection(graphPoints, utility)
        val monthlyProj = projectionEngine.calculateMonthlyProjection(graphPoints, utility, prevMonthTotal)

        val loggedCount = graphPoints.count { it.isLoggedReading }
        val measuredPoints = graphPoints.filter { it.isMeasured }.ifEmpty { graphPoints }
        val avgDaily = if (measuredPoints.isNotEmpty()) measuredPoints.map { it.value }.average() else 0.0
        val peakDaily = if (measuredPoints.isNotEmpty()) measuredPoints.maxOf { it.value } else 0.0

        ReportsUiState(
            selectedUtility = utility,
            selectedMonth = month,
            availableMonths = availableMonths,
            weeklyProjection = weeklyProj,
            monthlyProjection = monthlyProj,
            graphPoints = graphPoints,
            loggedReadingsCount = loggedCount,
            averageDailyUnits = avgDaily,
            peakDayUnits = peakDaily,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    init {
        viewModelScope.launch {
            entryRepository.getAllEntries().collect { list ->
                if (list.isEmpty()) {
                    val sampleData = generateSampleHistoricalEntries()
                    entryRepository.insertList(sampleData)
                    _rawEntries.value = sampleData
                } else {
                    _rawEntries.value = list
                }
            }
        }
    }

    fun setUtilityFilter(utilityType: UtilityType) {
        _selectedUtility.value = utilityType
    }

    fun setSelectedMonth(monthOption: MonthOption) {
        _selectedMonth.value = monthOption
    }

    private fun getDefaultPreviousMonthUnits(utilityType: UtilityType): Double {
        return when (utilityType) {
            UtilityType.ELECTRICITY -> 420.0
            UtilityType.GAS -> 82.0
            UtilityType.PROPANE -> 18.0
            UtilityType.WATER -> 6400.0
        }
    }

    private fun getPreviousMonth(year: Int, month: Int): Pair<Int, Int> =
        if (month > 1) year to month - 1 else year - 1 to 12

    companion object {
        private fun createAvailableMonthsList(currentYear: Int, currentMonth: Int): List<MonthOption> {
            val months = mutableListOf<MonthOption>()
            var y = currentYear
            var m = currentMonth

            for (i in 0 until 6) {
                val isCurrent = (i == 0)
                val monthKey = "$y-${m.toString().padStart(2, '0')}"
                val monthName = getFullMonthName(m)
                months.add(
                    MonthOption(
                        monthKey = monthKey,
                        displayName = "$monthName $y",
                        year = y,
                        month = m,
                        isCurrentMonth = isCurrent
                    )
                )

                m--
                if (m < 1) {
                    m = 12
                    y--
                }
            }
            return months
        }

        private fun getFullMonthName(month: Int): String {
            return when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> "Month"
            }
        }

        private fun generateSampleHistoricalEntries(): List<Entry> {
            val list = mutableListOf<Entry>()
            // Generate sample readings across past months for Gas, Electricity, and Propane
            val utilityConfigs = listOf(
                Triple("Natural Gas [m³]", 120.0, 2.5),
                Triple("Electricity [kWh]", 2400.0, 14.0),
                Triple("Propane Gas [kg]", 18.0, 0.6)
            )

            var idCounter = 1000
            for ((desc, baseReading, avgStep) in utilityConfigs) {
                var currentReading = baseReading
                var previousDate: LocalDate? = null
                // Create entries oldest first (August, September, October 2026) so the
                // meter reading only ever climbs over time, like a real meter
                val monthDates = listOf(
                    Triple(2026, 8, listOf(4, 11, 18, 25)),
                    Triple(2026, 9, listOf(2, 9, 16, 23, 30)),
                    Triple(2026, 10, listOf(3, 8, 15, 22, 28))
                )

                for ((y, m, days) in monthDates) {
                    val monthKey = "$y-${m.toString().padStart(2, '0')}"
                    for (day in days) {
                        val date = LocalDate(y, m, day)
                        val daysElapsed = previousDate?.daysUntil(date) ?: 7
                        previousDate = date

                        val prev = currentReading
                        val step = avgStep * (0.8 + (day % 4) * 0.15)
                        currentReading += step * daysElapsed // usage for the days since the last reading
                        val dateStr = date.toString()

                        list.add(
                            Entry(
                                id = idCounter++,
                                date = dateStr,
                                monthKey = monthKey,
                                meterReading = currentReading,
                                previousReadingValue = prev,
                                unitsConsumedSinceLastVisit = currentReading - prev,
                                daysElapsedSinceLastReading = daysElapsed,
                                weatherScore = 25.0 + (day % 3) * 5,
                                temperatureCelsius = 18.0 + (day % 4),
                                description = desc
                            )
                        )
                    }
                }
            }
            return list
        }
    }
}

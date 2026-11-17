package de.yanneckreiss.mlkittutorial.ui.camera

import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Test

class MeterReadingParserTest {

    @Test
    fun electricityUsesKwhLabel() {
        assertEquals("1234.5", MeterReadingParser.extract("Total 1234.5 k W h", UtilityType.ELECTRICITY))
        assertEquals("", MeterReadingParser.extract("12345", UtilityType.ELECTRICITY))
    }

    @Test
    fun waterPrefersValueWithUnit() {
        assertEquals("1234.5", MeterReadingParser.extract("SN 99887766 1234,5 m³", UtilityType.WATER))
        assertEquals("850", MeterReadingParser.extract("Usage 850 L", UtilityType.WATER))
    }

    @Test
    fun waterFallsBackToLongestDigitRun() {
        assertEquals("004521", MeterReadingParser.extract("x10 004521 12", UtilityType.WATER))
        assertEquals("", MeterReadingParser.extract("no digits", UtilityType.WATER))
    }
}

package de.yanneckreiss.mlkittutorial.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class KwhRegexTest {

    private val kwhRegex = Regex("""(\d+(?:\.\d+)?)\s*[kK]\s*[wW]\s*[hH]""")

    @Test
    fun testStandardKwhMatching() {
        val input = "Total reading 1234.5 kWh today"
        val match = kwhRegex.find(input)
        assertEquals("1234.5", match?.groupValues?.get(1))
    }

    @Test
    fun testKwhWithSpaceBetweenKAndWh() {
        val input = "Reading 4567.89 k Wh detected"
        val match = kwhRegex.find(input)
        assertEquals("4567.89", match?.groupValues?.get(1))
    }

    @Test
    fun testKwhWithSpaceBetweenAllLetters() {
        val input = "Meter 9876 k W h"
        val match = kwhRegex.find(input)
        assertEquals("9876", match?.groupValues?.get(1))
    }

    @Test
    fun testKwhUppercaseAndSpaced() {
        val input = "CURRENT VALUE: 3141.59 K W H"
        val match = kwhRegex.find(input)
        assertEquals("3141.59", match?.groupValues?.get(1))
    }

    @Test
    fun testKwhNoSpacesBetweenNumberAndUnit() {
        val input = "Reading 102030kWh"
        val match = kwhRegex.find(input)
        assertEquals("102030", match?.groupValues?.get(1))
    }
}

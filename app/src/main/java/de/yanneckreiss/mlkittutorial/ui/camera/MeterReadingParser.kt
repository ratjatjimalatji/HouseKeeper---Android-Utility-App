package de.yanneckreiss.mlkittutorial.ui.camera

import de.yanneckreiss.mlkittutorial.entry.data.local.UtilityType

/**
 * Extracts a meter reading from raw OCR text for the given utility.
 */
object MeterReadingParser {

    // kWh values, allowing spaces between unit letters ("1234.5 kWh", "1234.5 k Wh", "1234.5 k W h")
    private val kwhRegex = Regex("""(\d+(?:\.\d+)?)\s*[kK]\s*[wW]\s*[hH]""")

    // Water values followed by a unit label ("01234.5 m³", "1234 m3", "1234 L")
    private val waterWithUnitRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:[mM]\s*[³3]|[lL](?:itres?|iters?)?\b)""")

    // Fallback for water dials that show digits only: a run of at least 3 digits
    private val digitRunRegex = Regex("""\d{3,}(?:[.,]\d+)?""")

    /** Returns the reading as a plain decimal string, or "" when nothing was found. */
    fun extract(text: String, utilityType: UtilityType): String {
        val raw = when (utilityType) {
            UtilityType.WATER ->
                waterWithUnitRegex.find(text)?.groupValues?.get(1)
                    ?: digitRunRegex.findAll(text).maxByOrNull { it.value.length }?.value
            else -> kwhRegex.find(text)?.groupValues?.get(1)
        }
        return raw?.replace(',', '.')?.takeIf { it.toDoubleOrNull() != null }.orEmpty()
    }
}

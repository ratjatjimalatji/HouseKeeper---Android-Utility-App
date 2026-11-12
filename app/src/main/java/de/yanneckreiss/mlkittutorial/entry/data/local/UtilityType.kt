package de.yanneckreiss.mlkittutorial.entry.data.local

import androidx.compose.ui.graphics.Color

enum class UtilityType(
    val id: String,
    val displayName: String,
    val emoji: String,
    val defaultUnit: String,
    val colorHex: Long
) {
    GAS(
        id = "GAS",
        displayName = "Natural Gas",
        emoji = "🔥",
        defaultUnit = "m³",
        colorHex = 0xFFE64A19 // Warm Flame Orange
    ),
    ELECTRICITY(
        id = "ELECTRICITY",
        displayName = "Electricity",
        emoji = "⚡",
        defaultUnit = "kWh",
        colorHex = 0xFFFFB300 // Amber / Golden
    ),
    PROPANE(
        id = "PROPANE",
        displayName = "Propane Gas",
        emoji = "♨️",
        defaultUnit = "kg",
        colorHex = 0xFFD32F2F // Crimson Red
    ),
    WATER(
        id = "WATER",
        displayName = "Water",
        emoji = "💧",
        defaultUnit = "L",
        colorHex = 0xFF0288D1 // Ocean Blue
    );

    val composeColor: Color
        get() = Color(colorHex)

    companion object {
        fun fromString(value: String?): UtilityType {
            val v = value?.lowercase()?.trim() ?: return ELECTRICITY
            return when {
                v.contains("propane") || v.contains("lpg") || v.contains("cylinder") || v.contains("kg") || v.contains("♨") -> PROPANE
                v.contains("gas") || v.contains("natural") || v.contains("m3") || v.contains("m³") || v.contains("🔥") -> GAS
                v.contains("water") || v.contains("aqua") || v.contains("liter") || v.contains("litre") || v.contains("💧") -> WATER
                else -> ELECTRICITY
            }
        }
    }
}

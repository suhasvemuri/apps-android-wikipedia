package org.wikipedia.settings

enum class AdaptiveReadingWidthMode(val prefValue: String, val widthDp: Int) {
    COMPACT("compact", 680),
    BALANCED("balanced", 780),
    WIDE("wide", 900),
    IMMERSIVE("immersive", 1040);

    companion object {
        fun fromPrefValue(value: String): AdaptiveReadingWidthMode {
            return entries.firstOrNull { it.prefValue == value } ?: BALANCED
        }
    }
}

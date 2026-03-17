package org.wikipedia.settings

enum class LeadImageStyle(val prefValue: String) {
    HERO("hero"),
    EDITORIAL("editorial"),
    COMPACT("compact");

    companion object {
        fun fromPrefValue(value: String): LeadImageStyle {
            return entries.firstOrNull { it.prefValue == value } ?: EDITORIAL
        }
    }
}

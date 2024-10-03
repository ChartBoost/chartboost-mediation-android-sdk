package com.chartboost.sdk.internal.clickthrough

internal enum class ClickPreference(val value: Int) {
    CLICK_PREFERENCE_EMBEDDED(0),
    CLICK_PREFERENCE_NATIVE(1),
    ;

    companion object {
        fun fromValue(prefValue: Int): ClickPreference {
            return when (prefValue) {
                0 -> CLICK_PREFERENCE_EMBEDDED
                1 -> CLICK_PREFERENCE_NATIVE
                else -> CLICK_PREFERENCE_EMBEDDED
            }
        }
    }
}

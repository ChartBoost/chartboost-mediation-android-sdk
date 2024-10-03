package com.chartboost.sdk.privacy

import android.content.SharedPreferences

private const val TCFSTRING_KEY = "IABTCF_TCString"

class TCFv2(
    private val defaultSharedPreferences: SharedPreferences,
) {
    fun getTCFString(): String? {
        return defaultSharedPreferences.getString(TCFSTRING_KEY, null)
    }
}

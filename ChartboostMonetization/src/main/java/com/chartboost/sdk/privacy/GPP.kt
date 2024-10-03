package com.chartboost.sdk.privacy

import android.content.SharedPreferences

private const val GPP_KEY = "IABGPP_HDR_GppString"
private const val GPP_SECTION_STRING_ID = "IABGPP_GppSID"

class GPP(private val defaultSharedPreferences: SharedPreferences) {
    fun getGppString(): String? {
        return defaultSharedPreferences.getString(GPP_KEY, null)
    }

    fun getGppSid(): String? {
        return defaultSharedPreferences.getString(GPP_SECTION_STRING_ID, null)
    }
}

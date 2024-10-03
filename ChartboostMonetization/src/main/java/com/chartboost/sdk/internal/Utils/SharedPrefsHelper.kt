package com.chartboost.sdk.internal.utils

import android.content.SharedPreferences
import com.chartboost.sdk.internal.logging.Logger

class SharedPrefsHelper(private val sharedPrefs: SharedPreferences) {
    fun loadFromSharedPrefs(sharedPrefsKey: String): String? {
        return try {
            sharedPrefs.getString(sharedPrefsKey, null)
        } catch (ex: Exception) {
            Logger.e("Load from shared prefs exception", ex)
            null
        }
    }

    fun saveIntoSharedPrefs(
        sharedPrefsKey: String,
        data: String?,
    ) {
        return try {
            sharedPrefs.edit().putString(sharedPrefsKey, data).apply()
        } catch (ex: Exception) {
            Logger.e("Save to shared prefs exception", ex)
        }
    }
}

package com.chartboost.sdk.internal.utils

import android.content.res.Resources
import com.chartboost.sdk.internal.logging.Logger
import java.io.BufferedReader

class ResourceLoader(private val resources: Resources) {
    fun readRawResourceFile(resourceId: Int): String? {
        return try {
            resources.openRawResource(resourceId).use { inputStream ->
                inputStream.bufferedReader().use(BufferedReader::readText)
            }
        } catch (ex: Exception) {
            Logger.e("Raw resource file exception", ex)
            null
        }
    }
}

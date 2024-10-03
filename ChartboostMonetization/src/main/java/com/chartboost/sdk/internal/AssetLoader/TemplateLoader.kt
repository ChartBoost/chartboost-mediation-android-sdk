package com.chartboost.sdk.internal.AssetLoader

import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import java.io.File

internal class TemplateLoader(eventTracker: EventTrackerExtensions) : EventTrackerExtensions by eventTracker {
    fun formatTemplateHtml(
        htmlFile: File,
        allParams: Map<String, String>,
        adTypeName: String,
        location: String,
    ): String? {
        return try {
            val regex = """\{\{\s*([^}]+)\s*\}\}|\{%\s*([^}]+)\s*%\}""".toRegex()
            val params = allParams.filterKeys { it.startsWith("{{") || it.startsWith("{%") }
            regex.replace(htmlFile.readText(Charsets.UTF_8)) { matchResult ->
                val key = matchResult.value
                params[key] ?: key
            }.checkForMissingParameters()
        } catch (e: Exception) {
            Logger.e("Failed to parse template", e)
            trackShowHTMLError(adTypeName, location, e.toString())
            null
        }
    }

    private fun String.checkForMissingParameters(): String {
        if (this.contains("{{")) {
            throw IllegalArgumentException("Missing required template parameter $this")
        }
        return this
    }

    private fun trackShowHTMLError(
        adTypeName: String,
        location: String,
        errorMsg: String,
    ) {
        CriticalEvent.instance(
            TrackingEventName.Show.HTML_MISSING_MUSTACHE_ERROR,
            errorMsg,
            adTypeName,
            location,
        ).track()
    }
}

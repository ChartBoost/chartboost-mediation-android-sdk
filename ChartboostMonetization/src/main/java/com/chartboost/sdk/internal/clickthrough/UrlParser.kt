package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.Libraries.getBooleanOrNull
import org.json.JSONObject

private const val SHOULD_DISMISS_ARG = "shouldDismiss"
private const val URL_ARG = "url"

internal class UrlParser {
    fun parseOpenUrlArgsObject(json: JSONObject?) =
        CBUrl(
            json.getUrl(),
            json.shouldDismiss(),
        )

    private fun JSONObject?.getUrl(): String {
        return this?.optString(URL_ARG, "") ?: ""
    }

    private fun JSONObject?.shouldDismiss(): Boolean? {
        return this?.getBooleanOrNull(SHOULD_DISMISS_ARG)
    }
}

package com.chartboost.sdk.internal.WebView

import com.chartboost.sdk.internal.logging.Logger.e
import org.json.JSONObject

internal class WebViewCorsErrorHandler {
    /**
     * Takes message from the webview and detects potential CORS error
     * In case error is present, notify cors callback
     *
     * @param webviewMessage
     * @param callback
     */
    fun handleCorsError(
        webviewMessage: String?,
        callback: CorsErrorCallback?,
    ) {
        if (isMessageACorsError(webviewMessage)) {
            e(CORS_ERROR)
            callback?.notifyCorsError(JSONObject().put(CORS_ERROR_KEY, CORS_ERROR))
        }
    }

    /**
     * Only way to check for the CORS error is to see last log coming from the webview
     * Logs needs to contain CORS and header name in the message
     *
     * @param webviewMessage
     * @return
     */
    private fun isMessageACorsError(webviewMessage: String?): Boolean {
        return webviewMessage?.let {
            it.contains(CORS_HEADER) &&
                it.contains(CORS_ORIGIN) &&
                !it.contains("http://") &&
                !it.contains("https://")
        } ?: false
    }

    internal interface CorsErrorCallback {
        fun notifyCorsError(data: JSONObject?)
    }
}

private const val CORS_ERROR =
    "CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource"
private const val CORS_ERROR_KEY = "message"
private const val CORS_ORIGIN = "'null'"
private const val CORS_HEADER = "Access-Control-Allow-Origin"

package com.chartboost.sdk.internal.WebView

import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONObject

internal class CBTemplateProxy(
    eventTracker: EventTrackerExtensions,
) : EventTrackerExtensions by eventTracker {
    fun callOnBackgroundJSFunction(
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        callJSFunctionWithoutParameter(
            NativeCommand.ON_BACKGROUND.cmdName,
            webview,
            location,
            adTypeName,
        )
    }

    fun callOnForegroundJSFunction(
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        callJSFunctionWithoutParameter(
            NativeCommand.ON_FOREGROUND.cmdName,
            webview,
            location,
            adTypeName,
        )
    }

    fun callOnPlaybackTimeJSFunction(
        webview: CBWebView?,
        currentTime: Float,
        location: String,
        adTypeName: String,
    ) {
        val json = JSONObject()
        json.put("seconds", currentTime)
        callJSFunctionWithParameter(
            NativeCommand.PLAYBACK_TIME.cmdName,
            json.toString(),
            webview,
            location,
            adTypeName,
        )
    }

    fun callOnVideoFailedJSFunction(
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        callJSFunctionWithoutParameter(
            NativeCommand.VIDEO_FAILED.cmdName,
            webview,
            location,
            adTypeName,
        )
    }

    fun callOnVideoStartedJSFunction(
        webview: CBWebView?,
        duration: Float,
        location: String,
        adTypeName: String,
    ) {
        val json = JSONObject()
        json.put("totalDuration", duration)
        callJSFunctionWithParameter(
            NativeCommand.VIDEO_STARTED.cmdName,
            json.toString(),
            webview,
            location,
            adTypeName,
        )
    }

    fun callOnVideoEndedJSFunction(
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        callJSFunctionWithoutParameter(
            NativeCommand.VIDEO_ENDED.cmdName,
            webview,
            location,
            adTypeName,
        )
    }

    private fun callJSFunctionWithoutParameter(
        function: String,
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        val webUrl = "javascript:Chartboost.EventHandler.handleNativeEvent(\"$function\")"
        callJSFunction(webUrl, webview, location, adTypeName)
    }

    private fun callJSFunctionWithParameter(
        function: String,
        param: String,
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        val webUrl = "javascript:Chartboost.EventHandler.handleNativeEvent(\"$function\", $param)"
        callJSFunction(webUrl, webview, location, adTypeName)
    }

    private fun callJSFunction(
        url: String,
        webview: CBWebView?,
        location: String,
        adTypeName: String,
    ) {
        try {
            if (webview == null) {
                CriticalEvent(
                    TrackingEventName.Show.WEBVIEW_ERROR,
                    "Webview is null",
                    adTypeName,
                    location,
                ).track()
                Logger.e("Calling native to javascript webview is null")
                return
            }
            Logger.d("Calling native to javascript: $url")
            webview.loadUrl(url)
        } catch (e: Exception) {
            CriticalEvent(
                TrackingEventName.Show.WEBVIEW_CRASH,
                "Cannot open url: $e",
                adTypeName,
                location,
            ).track()
            Logger.e("Calling native to javascript. Cannot open url", e)
        }
    }
}

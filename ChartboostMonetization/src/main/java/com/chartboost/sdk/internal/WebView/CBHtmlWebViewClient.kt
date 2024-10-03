package com.chartboost.sdk.internal.WebView

import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.internal.clickthrough.CBUrl
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.EventTracker
import java.lang.reflect.Modifier.PRIVATE

internal class CBHtmlWebViewClient(
    private val impressionInterface: ImpressionInterface,
    internal val gestureDetector: SingleClickGestureDetector,
    callback: CustomWebViewInterface,
    eventTracker: EventTracker,
) : CustomWebViewClient(callback, eventTracker) {
    @VisibleForTesting(otherwise = PRIVATE)
    var hasLoadFinished: Boolean = false

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String,
    ): Boolean {
        return attemptToOpenUrl(url)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        return attemptToOpenUrl(request.url.toString())
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        super.onPageFinished(view, url)
        hasLoadFinished = true
    }

    private fun attemptToOpenUrl(url: String): Boolean {
        if (!hasLoadFinished) {
            Logger.e("Attempt to open $url detected before WebView loading finished.")
            impressionInterface.onClickBeforeLoadFinished(CBUrl(url, false))
            return true // consume the click without doing anything
        }

        if (gestureDetector.hasClick) {
            impressionInterface.onTemplateOpenURLEvent(
                CBUrl(url, false),
            )
            gestureDetector.resetClickState()
            return true
        }
        return false
    }
}

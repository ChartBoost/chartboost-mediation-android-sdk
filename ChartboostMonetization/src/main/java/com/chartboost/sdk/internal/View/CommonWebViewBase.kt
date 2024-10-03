package com.chartboost.sdk.internal.View

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.RelativeLayout
import com.chartboost.sdk.BuildConfig
import com.chartboost.sdk.internal.WebView.*
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.legacy.Factory
import com.chartboost.sdk.tracking.EventTracker

@SuppressLint("ViewConstructor")
internal open class CommonWebViewBase(
    context: Context,
    html: String,
    callback: CustomWebViewInterface,
    baseExternalPathURL: String?,
    eventTracker: EventTracker,
    cbWebViewFactory: (Context) -> CBWebView = { CBWebView(it) },
    cbWebChromeClientFactory: (View) -> WebChromeClient = { WebChromeClient() },
    cbWebViewClientFactory: (CustomWebViewInterface, EventTracker) -> CustomWebViewClient = {
            cb, et ->
        CustomWebViewClient(cb, et)
    },
) : ViewBase(context) {
    init {
        this.isFocusable = false
        val factory = Factory.instance()
        webViewContainer = factory.intercept(RelativeLayout(context))
        webView = cbWebViewFactory(context)
        UserAgentHelper.setUserAgent(context) // Set with new WebView instance

        try {
            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG_WEBVIEW)
        } catch (e: RuntimeException) {
            Logger.w("Exception while enabling webview debugging", e)
        }

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        webView?.apply {
            settings.setSupportZoom(false)
            layoutParams = params
            setBackgroundColor(0)
            webViewClient = factory.intercept(cbWebViewClientFactory(callback, eventTracker))
            webViewContainer?.let {
                it.layoutParams = params
                webChromeClient = cbWebChromeClientFactory(it)
                it.addView(this)
            }
            loadDataWithBaseURL(baseExternalPathURL, html, "text/html", "utf-8", null)
        }
    }
}

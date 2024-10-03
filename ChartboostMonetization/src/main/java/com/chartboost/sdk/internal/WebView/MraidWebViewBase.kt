package com.chartboost.sdk.internal.WebView

import android.annotation.SuppressLint
import android.content.Context
import com.chartboost.sdk.tracking.EventTracker

@SuppressLint("ViewConstructor")
internal class MraidWebViewBase(
    context: Context,
    html: String,
    callback: CustomWebViewInterface,
    baseExternalPathURL: String?,
    nativeBridgeCommand: NativeBridgeCommand,
    eventTracker: EventTracker,
    cbWebViewFactory: (Context) -> CBWebView = { CBWebView(it) },
) : RichWebViewBase(
        context,
        html,
        callback,
        baseExternalPathURL,
        nativeBridgeCommand,
        eventTracker = eventTracker,
        cbWebViewFactory = cbWebViewFactory,
    ) {
    init {
        this.addView(webViewContainer)
        callback.onWebViewInit()
        callback.onRegisterWebViewTimeout()
    }
}

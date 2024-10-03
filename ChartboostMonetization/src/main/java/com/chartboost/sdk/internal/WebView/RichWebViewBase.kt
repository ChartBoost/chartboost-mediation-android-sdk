package com.chartboost.sdk.internal.WebView

import android.annotation.SuppressLint
import android.content.Context
import com.chartboost.sdk.internal.View.CommonWebViewBase
import com.chartboost.sdk.tracking.EventTracker

/**
 * RichWebViewBase
 * An abstract implementation of CommonWebViewBase which utilizes the CBRichWebChromeClient to
 * support rich media such as Mraid and VAST
 */

@SuppressLint("ViewConstructor")
internal open class RichWebViewBase(
    context: Context,
    html: String,
    callback: CustomWebViewInterface,
    baseExternalPathURL: String?,
    nativeBridgeCommand: NativeBridgeCommand,
    webViewCorsErrorHandler: WebViewCorsErrorHandler = WebViewCorsErrorHandler(),
    eventTracker: EventTracker,
    cbWebViewFactory: (Context) -> CBWebView = { CBWebView(it) },
) : CommonWebViewBase(
        context,
        html,
        callback,
        baseExternalPathURL,
        eventTracker = eventTracker,
        cbWebViewFactory = cbWebViewFactory,
        cbWebChromeClientFactory = { container ->
            CBRichWebChromeClient(
                container,
                nativeBridgeCommand,
                webViewCorsErrorHandler,
            )
        },
    )

package com.chartboost.sdk.internal.View

import android.content.Context
import android.webkit.WebChromeClient
import android.widget.RelativeLayout
import com.chartboost.sdk.internal.Libraries.Orientation
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.logging.Logger.d

abstract class ViewBase(context: Context) : RelativeLayout(context) {
    var webView: CBWebView? = null
    var webChromeClient: WebChromeClient? = null
    var webViewContainer: RelativeLayout? = null
    var lastOrientation: Orientation? = null

    init {
        this.isFocusableInTouchMode = true
        this.requestFocus()
    }

    open fun destroyWebview() {
        if (webView == null) {
            d("Webview is null on destroyWebview")
            return
        }
        webViewContainer?.let {
            it.removeView(webView)
            removeView(it)
        } ?: d("webViewContainer is null destroyWebview")

        webView?.run {
            loadUrl("about:blank")
            onPause()
            removeAllViews()
            destroy()
        }

        removeAllViews()
    }
}

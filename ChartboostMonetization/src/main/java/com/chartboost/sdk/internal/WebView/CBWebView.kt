package com.chartboost.sdk.internal.WebView

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.chartboost.sdk.BuildConfig

/**
 * This class serves as a WebView to be used in conjunction with a CBVideoEnabledWebChromeClient.
 * It makes possible:
 * - To detect the HTML5 video ended event so that the CBVideoEnabledWebChromeClient can exit full-screen.
 *
 *
 * Important notes:
 * - Javascript is enabled by default and must not be disabled with getSettings().setJavaScriptEnabled(false).
 * - setWebChromeClient() must be called before any loadData(), loadDataWithBaseURL() or loadUrl() method.
 *
 * @author Cristian Perez (http://cpr.name)
 */
open class CBWebView : WebView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /**
     * Pass only a CBVideoEnabledWebChromeClient instance.
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun setWebChromeClient(client: WebChromeClient?) {
        super.setWebChromeClient(client)
        finishWebViewSetup()
    }

    private fun finishWebViewSetup() {
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            // The below flags are used to resize the webview contents based on the device density.
            loadWithOverviewMode = true
            useWideViewPort = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            builtInZoomControls = false
            displayZoomControls = false
        }
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // disable this on production for performance reasons:
            setWebContentsDebuggingEnabled(true)
        }
    }
}

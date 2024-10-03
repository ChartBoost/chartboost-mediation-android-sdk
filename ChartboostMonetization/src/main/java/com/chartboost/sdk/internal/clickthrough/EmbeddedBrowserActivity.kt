package com.chartboost.sdk.internal.clickthrough

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.chartboost.sdk.internal.di.getEventTracker
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import org.json.JSONObject

class EmbeddedBrowserActivity : Activity(), EventTrackerExtensions by getEventTracker() {
    private val view: View by lazy {
        frameLayout.apply {
            addView(webView)
        }
    }

    private val frameLayout: FrameLayout by lazy {
        FrameLayout(this).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
        }
    }

    private val webView: WebView by lazy {
        WebView(this@EmbeddedBrowserActivity).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            webViewClient = EmbeddedBrowserClient()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        runCatching {
            super.onCreate(savedInstanceState)
            setContentView(view)
            intent.url()?.let(webView::loadUrl) ?: close()
        }.onFailure(::close)
    }

    private fun close(t: Throwable? = null) {
        Logger.e("Error loading URL into embedded browser", t)
        finish()
    }

    private inner class EmbeddedBrowserClient : WebViewClient() {
        private val trackedErrorCodeList =
            listOf(
                ERROR_UNKNOWN,
                ERROR_HOST_LOOKUP,
                ERROR_UNSUPPORTED_AUTH_SCHEME,
                ERROR_CONNECT,
                ERROR_REDIRECT_LOOP,
                ERROR_UNSUPPORTED_SCHEME,
                ERROR_FAILED_SSL_HANDSHAKE,
                ERROR_BAD_URL,
            )

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            super.onReceivedError(view, request, error)
            Logger.e("onReceivedError: $error")
            error.trackEmbeddedBrowserError()
        }

        private fun WebResourceError?.trackEmbeddedBrowserError() {
            if (isTrackedError()) {
                ErrorEvent(
                    name = TrackingEventName.Navigation.FAILURE,
                    message = asErrorMessage(),
                ).track()
            }
        }

        private fun WebResourceError?.isTrackedError(): Boolean = trackedErrorCodeList.any { it == this?.errorCode }

        private fun WebResourceError?.asErrorMessage(): String {
            return JSONObject().apply {
                put("url", intent?.url())
                put("error", this@asErrorMessage?.description ?: "")
            }.toString()
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?,
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            Logger.e("onReceivedHttpError: $errorResponse")
            errorResponse.trackEmbeddedBrowserError()
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?,
        ): Boolean {
            ErrorEvent(
                name = TrackingEventName.Click.FAILURE,
                message =
                    if (detail?.didCrash() == true) {
                        "Webview crashed $detail"
                    } else {
                        "Webview killed, likely due to low memory"
                    },
            ).track()
            (view?.context as? Activity)?.finish()
            return true
        }

        private fun WebResourceResponse?.trackEmbeddedBrowserError() {
            ErrorEvent(
                name = TrackingEventName.Navigation.FAILURE,
                message = asErrorMessage(),
            ).track()
        }

        private fun WebResourceResponse?.asErrorMessage(): String {
            return JSONObject().apply {
                put("url", intent?.url())
                put("error", "HTTP status code: ${this@asErrorMessage?.statusCode}")
            }.toString()
        }
    }

    internal companion object {
        private const val KEY_INTENT_URL = "KEY_INTENT_URL"

        fun intent(
            context: Context,
            url: String,
        ): Intent =
            Intent(context, EmbeddedBrowserActivity::class.java)
                .putExtra(KEY_INTENT_URL, url)

        private fun Intent?.url(): String? = this?.getStringExtra(KEY_INTENT_URL)
    }
}

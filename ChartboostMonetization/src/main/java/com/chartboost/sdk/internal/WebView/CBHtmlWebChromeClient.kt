package com.chartboost.sdk.internal.WebView

import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.chartboost.sdk.internal.logging.Logger

internal class CBHtmlWebChromeClient : WebChromeClient() {
    override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
        val consoleMsg = cm.message()
        Logger.d(
            "Chartboost Html Webview: $consoleMsg -- From line ${cm.lineNumber()} of ${cm.sourceId()}",
        )
        return true
    }

    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean {
        Logger.d("Chartboost Html Webview: onJsAlert with url of $url -- $message")
        result.confirm()
        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String,
        result: JsResult,
    ): Boolean {
        Logger.d("Chartboost Html Webview: onJsConfirm with url of $url -- $message")
        result.cancel()
        return true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String,
        defaultValue: String,
        result: JsPromptResult,
    ): Boolean {
        Logger.d("Chartboost Html Webview: onJsPrompt with url of $url -- $message")
        result.confirm()
        return true
    }
}

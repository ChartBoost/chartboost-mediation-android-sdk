package com.chartboost.sdk.internal.WebView

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.WebViewClient

@SuppressLint("ClickableViewAccessibility")
internal class CBHtmlWebView(
    context: Context,
) : CBWebView(context) {
    override fun setWebViewClient(client: WebViewClient) {
        super.setWebViewClient(client)

        val gestureDetector =
            if (client is CBHtmlWebViewClient) {
                client.gestureDetector
            } else {
                null
            }

        setOnTouchListener { _, motionEvent ->
            gestureDetector?.onTouchEvent(motionEvent)
            return@setOnTouchListener motionEvent.action == MotionEvent.ACTION_MOVE
        }
    }
}

package com.chartboost.sdk.internal.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.BLACK
import android.view.SurfaceView
import android.widget.FrameLayout
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.WebView.CustomWebViewInterface
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.WebView.RichWebViewBase
import com.chartboost.sdk.tracking.EventTracker

@SuppressLint("ViewConstructor")
internal class VideoBase(
    context: Context,
    html: String,
    callback: CustomWebViewInterface,
    nativeBridgeCommand: NativeBridgeCommand,
    baseExternalPathURL: String?,
    private var surface: SurfaceView?,
    private var videoBackground: FrameLayout = FrameLayout(context),
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
        if (surface != null) {
            videoBackground.apply {
                layoutParams =
                    FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setBackgroundColor(BLACK)
            }
            this.addView(videoBackground)
            // place video surface on top of the background
            videoBackground.addView(surface)
        } else {
            error("SurfaceView is not ready. Cannot display video.")
        }

        this.addView(webViewContainer)
        callback.onWebViewInit()
        callback.onRegisterWebViewTimeout()
    }

    fun removeSurfaceView() {
        if (surface != null) {
            surface?.visibility = GONE
            videoBackground.removeView(surface)
            this.removeView(videoBackground)
        }
    }
}

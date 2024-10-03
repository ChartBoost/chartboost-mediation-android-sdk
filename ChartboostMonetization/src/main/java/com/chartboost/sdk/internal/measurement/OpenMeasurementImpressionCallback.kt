package com.chartboost.sdk.internal.measurement

import android.view.View
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.WebView.CBWebView
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import com.iab.omid.library.chartboost.adsession.media.PlayerState

interface OpenMeasurementImpressionCallback {
    fun onImpressionDestroyWebview()

    fun onImpressionOnWebviewPageStarted(
        mtype: MediaTypeOM,
        webview: CBWebView,
        verificationScriptResourcesList: List<VerificationScriptResource>,
    )

    fun onImpressionNotifyFriendlyObstructionCreated(view: View)

    fun onImpressionNotifyVideoStarted(
        videoDuration: Float,
        volume: Float,
    )

    fun onImpressionNotifyVideoBuffer(isBufferStart: Boolean)

    fun onImpressionNotifyVideoProgress(quartile: Quartile)

    fun onImpressionNotifyVideoComplete()

    fun onImpressionNotifyVideoSkipped()

    fun onImpressionNotifyVideoPaused()

    fun onImpressionNotifyVideoResumed()

    fun onImpressionNotifyVolumeChanged(volume: Float)

    fun onImpressionNotifyStateChanged(state: PlayerState)

    fun onImpressionNotifyClick()
}

enum class Quartile {
    FIRST,
    MIDDLE,
    THIRD,
}

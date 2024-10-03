package com.chartboost.sdk.internal.measurement

import android.content.Context
import android.view.View
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.logging.Logger
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import com.iab.omid.library.chartboost.adsession.media.PlayerState

class OpenMeasurementController(
    private val openMeasurementManager: OpenMeasurementManager,
    private val openMeasurementSessionBuilder: OpenMeasurementSessionBuilder,
) : OpenMeasurementImpressionCallback {
    private var omTracker: OpenMeasurementTracker? = null
    private var omVisibilityTracker: VisibilityTracker? = null

    fun isOmSdkEnabled(): Boolean {
        return openMeasurementManager.isOmSdkEnabled()
    }

    fun signalImpressionEvent() {
        omTracker?.signalImpressionEvent() ?: Logger.d(
            "signalImpressionEvent missing om tracker",
        )
    }

    fun createVisibilityTracker(
        context: Context,
        trackedView: View,
        rootView: View,
        visibilityTrackerListener: VisibilityTracker.VisibilityTrackerListener,
    ) {
        destroyVisibilityTracker()
        openMeasurementManager.getOmVisibilityTrackerConfig().let { config ->
            omVisibilityTracker =
                VisibilityTracker(
                    context,
                    trackedView,
                    rootView,
                    config.minVisibleDips,
                    config.minVisibleDurationMs,
                    config.visibilityCheckIntervalMs,
                    config.traversalLimit,
                ).apply {
                    this.visibilityTrackerListener = visibilityTrackerListener
                    start()
                }
        }
    }

    fun destroyVisibilityTracker() {
        omVisibilityTracker?.destroy()
        omVisibilityTracker = null
    }

    override fun onImpressionDestroyWebview() {
        omTracker?.stopSession() ?: Logger.d("onImpressionDestroyWebview missing om tracker")
        omTracker = null
    }

    override fun onImpressionOnWebviewPageStarted(
        mtype: MediaTypeOM,
        webview: CBWebView,
        verificationScriptResourcesList: List<VerificationScriptResource>,
    ) {
        try {
            buildOmTrackerAndStartTracking(mtype, webview, verificationScriptResourcesList)
        } catch (e: Exception) {
            Logger.d("OMSDK Session error", e)
        }
    }

    override fun onImpressionNotifyFriendlyObstructionCreated(view: View) {
        omTracker?.registerFriendlyObstruction(view)
    }

    override fun onImpressionNotifyVideoStarted(
        videoDuration: Float,
        volume: Float,
    ) {
        omTracker?.signalMediaStart(videoDuration, volume) ?: Logger.d(
            "onImpressionNotifyVideoStarted missing om tracker",
        )
    }

    override fun onImpressionNotifyVideoProgress(quartile: Quartile) {
        omTracker?.run {
            when (quartile) {
                Quartile.FIRST -> signalMediaFirstQuartile()
                Quartile.MIDDLE -> signalMediaMidpoint()
                Quartile.THIRD -> signalMediaThirdQuartile()
            }
        } ?: Logger.d(
            "onImpressionNotifyVideoProgress missing om tracker",
        )
    }

    override fun onImpressionNotifyVideoComplete() {
        omTracker?.signalMediaComplete() ?: Logger.d(
            "onImpressionNotifyVideoComplete missing om tracker",
        )
    }

    override fun onImpressionNotifyVideoSkipped() {
        omTracker?.signalMediaSkipped() ?: Logger.d(
            "onImpressionNotifyVideoSkipped missing om tracker",
        )
    }

    override fun onImpressionNotifyVideoPaused() {
        omTracker?.signalMediaPause() ?: Logger.d(
            "onImpressionNotifyVideoPaused missing om tracker",
        )
    }

    override fun onImpressionNotifyVideoResumed() {
        omTracker?.signalMediaResume() ?: Logger.d(
            "onImpressionNotifyVideoResumed missing om tracker",
        )
    }

    override fun onImpressionNotifyVideoBuffer(isBufferStart: Boolean) {
        omTracker?.run {
            if (isBufferStart) {
                signalMediaBufferStart()
            } else {
                signalMediaBufferFinish()
            }
        } ?: Logger.d(
            "onImpressionNotifyVideoBuffer missing om tracker",
        )
    }

    override fun onImpressionNotifyVolumeChanged(volume: Float) {
        omTracker?.signalMediaVolumeChange(volume) ?: Logger.d(
            "onImpressionNotifyVolumeChanged missing om tracker",
        )
    }

    override fun onImpressionNotifyStateChanged(state: PlayerState) {
        omTracker?.signalMediaStateChange(state) ?: Logger.d(
            "onImpressionNotifyStateChanged missing om tracker",
        )
    }

    override fun onImpressionNotifyClick() {
        omTracker?.signalUserInteractionClick() ?: Logger.d(
            "onImpressionNotifyClick missing om tracker",
        )
    }

    @Throws(Exception::class)
    private fun buildOmTrackerAndStartTracking(
        mtype: MediaTypeOM,
        webview: CBWebView,
        verificationScriptResourcesList: List<VerificationScriptResource>,
    ) {
        openMeasurementManager.initialize()
        stopSession() // this should not happen but just in case it has to cleared to avoid leaks
        openMeasurementSessionBuilder.createOmSession(
            webview,
            mtype,
            openMeasurementManager.getOmidPartner(),
            openMeasurementManager.getOmSdkJsLib(),
            verificationScriptResourcesList,
            openMeasurementManager.isVerificationEnabled(),
            openMeasurementManager.getVerificationListFromConfig(),
        )?.let { sessionHolder ->
            omTracker =
                OpenMeasurementTracker(sessionHolder, openMeasurementManager.isOmSdkEnabled())
        }
        startAndLoadSession()
    }

    private fun startAndLoadSession() {
        omTracker?.run {
            startSession()
            signalLoadEvent()
        } ?: Logger.d(
            "startAndLoadSession missing tracker",
        )
    }

    private fun stopSession() {
        omTracker?.stopSession()
        omTracker = null
    }
}

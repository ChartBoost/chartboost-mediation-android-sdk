package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.AdUnitManager.data.AdUnit
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import com.iab.omid.library.chartboost.adsession.media.PlayerState

internal class ImpressionDismiss(
    private val adUnit: AdUnit,
    private val location: String,
    private val adType: AdType,
    private val adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
    private val impressionIntermediateCallback: ImpressionIntermediateCallback,
    private val appRequest: AppRequest,
    private val downloader: Downloader,
    private val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    eventTracker: EventTrackerExtensions,
) : ImpressionDismissable, EventTrackerExtensions by eventTracker {
    private var closed = true

    override fun onClose(state: ImpressionState) {
        closed = true
        openMeasurementImpressionCallback.onImpressionNotifyStateChanged(PlayerState.NORMAL)
        when (state) {
            ImpressionState.DISPLAYED -> dismissImpression()
            ImpressionState.LOADED -> {
                removeImpression()
                CriticalEvent(
                    TrackingEventName.Show.CLOSE_BEFORE_TEMPLATE_SHOW_ERROR,
                    "onClose with state Loaded",
                    adType.name,
                    location,
                ).track()
            }
            else -> { /* ignore */ }
        }
        adUnitRendererImpressionCallback.onImpressionCloseTriggered(appRequest)
    }

    /**
     * It will call public callback onDismiss
     */
    override fun callImpressionDismissCallback() {
        adUnitRendererImpressionCallback.onImpressionDismissed(adUnit.impressionId)
    }

    override fun setClosed(close: Boolean) {
        closed = close
    }

    /**
     * Dismiss the current visible impression.
     */
    private fun dismissImpression() {
        Logger.e("Dismissing impression")
        impressionIntermediateCallback.setImpressionState(ImpressionState.DISMISSING)
        removeImpression()
    }

    private fun removeImpression() {
        Logger.e("Removing impression")
        impressionIntermediateCallback.setImpressionState(ImpressionState.NONE)
        impressionIntermediateCallback.destroyImpression()
        downloader.resume()
    }
}

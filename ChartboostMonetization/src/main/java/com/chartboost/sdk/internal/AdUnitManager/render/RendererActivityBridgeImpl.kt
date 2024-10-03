package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.utils.ImpressionActivityIntentWrapper
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import com.chartboost.sdk.view.CBImpressionActivity
import java.lang.ref.WeakReference

internal class RendererActivityBridgeImpl(
    private val impressionActivityIntentWrapper: ImpressionActivityIntentWrapper,
    eventTracker: EventTrackerExtensions,
) : RendererActivityBridge, EventTrackerExtensions by eventTracker {
    private var activityInterface: WeakReference<ActivityRendererInterface>? = null
    private var adUnitRendererActivityInterface: WeakReference<AdUnitRendererActivityInterface>? =
        null

    private var isFinishedHandled = false

    override fun setActivityRendererInterface(
        activityInterface: ActivityRendererInterface,
        activity: CBImpressionActivity,
    ) {
        this.activityInterface = WeakReference(activityInterface)
        // now we can set the view
        adUnitRendererActivityInterface?.get()?.onActivityIsReadyToDisplay(activity)
    }

    override fun displayViewOnActivity(viewBase: ViewBase) {
        activityInterface?.get()?.displayViewOnActivity(viewBase) ?: Logger.d(
            "activityInterface is null",
        )
    }

    override fun startActivity(adUnitRendererActivityInterface: AdUnitRendererActivityInterface) {
        this.adUnitRendererActivityInterface = WeakReference(adUnitRendererActivityInterface)
        try {
            impressionActivityIntentWrapper.run {
                startActivity(getImpressionActivityIntent())
            }
        } catch (e: Exception) {
            Logger.e(
                "Please add CBImpressionActivity in AndroidManifest.xml" +
                    " following README.md instructions",
                e,
            )
            failure(CBError.Impression.ACTIVITY_MISSING_IN_MANIFEST)
        }
    }

    override fun onStart() {
        adUnitRendererActivityInterface?.get()?.impressionOnStart() ?: Logger.d(
            "Bridge onStart missing callback to renderer",
        )
    }

    override fun onResume() {
        adUnitRendererActivityInterface?.get()?.impressionOnResume() ?: Logger.d(
            "Bridge onResume missing callback to renderer",
        )
    }

    override fun onPause() {
        adUnitRendererActivityInterface?.get()?.impressionOnPause() ?: Logger.d(
            "Bridge onPause missing callback to renderer",
        )
    }

    override fun onDestroy() {
        sendDismissFailureEvent()
        adUnitRendererActivityInterface?.get()?.impressionOnDestroy() ?: Logger.d(
            "Bridge onDestroy missing callback to renderer",
        )
        activityInterface?.clear()
        adUnitRendererActivityInterface?.clear()
    }

    override fun onConfigurationChange() {
        adUnitRendererActivityInterface?.get()?.onConfigurationChange()
    }

    override fun failure(error: CBError.Impression) {
        adUnitRendererActivityInterface?.get()?.failure(error)
    }

    override fun onBackPressed(): Boolean {
        return adUnitRendererActivityInterface?.get()?.onBackPressed() ?: false
    }

    override fun finishActivity() {
        isFinishedHandled = true
        activityInterface?.get()?.finishActivity()
    }

    override fun applyOrientationProperties(
        forceOrientation: Int,
        allowOrientationChange: Boolean,
    ) {
        activityInterface?.get()?.applyOrientationProperties(forceOrientation, allowOrientationChange)
    }

    override fun restoreOriginalOrientation() {
        activityInterface?.get()?.restoreOriginalOrientation()
    }

    private fun sendDismissFailureEvent() {
        if (!isFinishedHandled) {
            ErrorEvent(
                TrackingEventName.Show.DISMISS_MISSING,
                "dismiss_missing happened due to sdk closure outside expected flow",
            ).track()
        }
    }
}

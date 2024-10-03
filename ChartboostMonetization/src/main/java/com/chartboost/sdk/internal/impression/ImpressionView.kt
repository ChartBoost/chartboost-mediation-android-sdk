package com.chartboost.sdk.internal.impression

import android.view.View
import android.view.ViewGroup
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.Model.CBError.Impression
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.internal.clickthrough.ImpressionClickCallback
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.logging.Logger.e
import com.chartboost.sdk.internal.logging.Logger.i
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.legacy.VastVideoEvent
import com.chartboost.sdk.view.CBImpressionActivity
import java.lang.ref.WeakReference

internal class ImpressionView(
    private val appRequest: AppRequest,
    private val viewProtocol: CBViewProtocol,
    private val downloader: Downloader,
    bannerView: ViewGroup?, // e.g ChartboostBanner
    private val adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
    private val impressionIntermediateCallback: ImpressionIntermediateCallback,
    private val impressionClickCallback: ImpressionClickCallback,
) : ImpressionViewable {
    private val bannerView: WeakReference<ViewGroup> = WeakReference(bannerView)

    /**
     * We check visibility based on the OM verification visibility. If OM is enabled
     * but doesn't detect visibility, ad display won't be successful
     */
    private var isVisible = false

    /**
     * Use this to indicate if the fact that tasks that must be run upon showing
     * and impression have been processed. Use this to avoid,
     * for example, notifying the server twice.
     */
    private var isShowProcessed = false

    /**
     * Keep track if impression was signaled already
     */
    override var wasImpressionSignaled: Boolean = false

    /**
     * Keeps the view state for the background/foreground
     */
    private var isPaused = false

    /**
     * Keep track if video show was sent alredy
     */
    private var isVideoShowSent = false

    /**
     * Keep track if impression was already closed
     */
    private var impressionClosed: Boolean = false

    /**
     * This is banner view holder
     */
    override fun getHostView(): ViewGroup? {
        return bannerView.get()
    }

    override fun setIsVisible(visible: Boolean) {
        isVisible = visible
    }

    override fun getIsVisible(): Boolean {
        return isVisible
    }

    override fun setIsShowProcessed(showProcessed: Boolean) {
        isShowProcessed = showProcessed
    }

    override fun getIsShowProcessed(): Boolean {
        return isShowProcessed
    }

    override fun setIsVideoShowSent(showSent: Boolean) {
        isVideoShowSent = showSent
    }

    override fun getIsVideoShowSent(): Boolean {
        return isVideoShowSent
    }

    override fun setImpressionClosed(impressionClose: Boolean) {
        impressionClosed = impressionClose
    }

    override fun getImpressionClosed(): Boolean {
        return impressionClosed
    }

    override fun sendImpressionReadyToBeDisplayedCallback() {
        adUnitRendererImpressionCallback.onImpressionReadyToBeDisplayed()
    }

    /**
     * for an impression that needs to show some UI elements
     * before fully shown (eg a rewarded view that can be declined),
     * call this to indicate when it has definitely been fully shown.
     * feel free to call more than once, subsequent calls will have no effect.
     */
    override fun shownFully() {
        adUnitRendererImpressionCallback.onImpressionShownFully(
            appRequest,
        )
    }

    /**
     * Impression onFailure calls impressionError which removes the impression from the view and
     * calls callback either onShowFailure or onAdFailToLoad in AdUnitManager postErrorByAdType
     * and trigger onDismiss
     * @param error
     */
    override fun onFailure(error: Impression) {
        isVideoShowSent = true
        adUnitRendererImpressionCallback.onImpressionError(
            appRequest,
            error,
        )
    }

    override fun onStart() {
        impressionClickCallback.setImpressionClick(false)
    }

    override fun onResume() {
        impressionClickCallback.setImpressionClick(false)
        if (isPaused) {
            isPaused = false
            viewProtocol.onResume()
        }
    }

    override fun onPause() {
        if (!isPaused) {
            isPaused = true
            viewProtocol.onPause()
        }
    }

    override fun closeImpression() {
        if (getImpressionClosed()) return
        setImpressionClosed(true)

        if (getIsVideoShowSent()) {
            impressionIntermediateCallback.callImpressionDismissCallback()
        } else {
            onFailure(Impression.INTERNAL)
        }

        // send the skipped event but user manually triggered closure of the ad with back button
        viewProtocol.sendWebViewVastOmEvent(VastVideoEvent.SKIP)
        impressionIntermediateCallback.callOnClose()
        viewProtocol.restoreOriginalOrientation()
    }

    /**
     * Display a view given the impression. If you want a loading view,
     * make sure that the impression's state is set to be waiting for a loading view.
     */
    override fun displayOnActivity(
        state: ImpressionState,
        activity: CBImpressionActivity,
    ) {
        if (state != ImpressionState.LOADING) {
            displayOnActivityImpl(activity)
        } else {
            Logger.d("displayOnActivity invalid state: $state")
        }
    }

    override fun displayOnHostView(hostView: ViewGroup?) {
        try {
            if (hostView == null) {
                e("Cannot display on host because it is null!")
                onFailure(Impression.ERROR_DISPLAYING_VIEW)
                return
            }

            val error = viewProtocol.tryCreatingViewOnHostView(hostView)
            if (error != null) {
                e("displayOnHostView tryCreatingViewOnHostView error $error")
                onFailure(error)
                return
            }

            viewProtocol.view?.let {
                attachViewToHostAndSignalImpression(hostView, it)
            } ?: {
                e("Cannot display on host because view was not created!")
                onFailure(Impression.ERROR_CREATING_VIEW)
            }
        } catch (e: Exception) {
            e("displayOnHostView e", e)
            onFailure(Impression.ERROR_CREATING_VIEW)
        }
    }

    private fun attachViewToHostAndSignalImpression(
        hostView: ViewGroup,
        adView: View,
    ) {
        impressionIntermediateCallback.setImpressionState(ImpressionState.DISPLAYED)
        viewProtocol.view?.context?.let {
            adUnitRendererImpressionCallback.onImpressionViewCreated(it)
        } ?: e("Missing context on onImpressionViewCreated")
        hostView.addView(adView)
        downloader.pause()
    }

    /**
     * Display an impression, ensuring there is a popup view to put it in.
     */
    private fun displayOnActivityImpl(activity: CBImpressionActivity) {
        impressionIntermediateCallback.setImpressionState(ImpressionState.DISPLAYED)
        try {
            val error = viewProtocol.tryCreatingViewOnActivity(activity)
            if (error != null) {
                onFailure(error)
                return
            }
        } catch (e: Exception) {
            e("Cannot create view in protocol", e)
            onFailure(Impression.ERROR_CREATING_VIEW)
            return
        }
        i("Displaying the impression")
    }
}

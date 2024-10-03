package com.chartboost.sdk.internal.AdUnitManager.render

import android.content.Context
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.AdUnitManager.impression.ImpressionBuilder
import com.chartboost.sdk.internal.AssetLoader.TemplateLoader
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.Networking.EndpointRepository
import com.chartboost.sdk.internal.Networking.requests.models.ShowParamsModel
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.CBUrl
import com.chartboost.sdk.internal.clickthrough.ImpressionClickCallback
import com.chartboost.sdk.internal.impression.CBImpression
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.ImpressionIntermediateCallback
import com.chartboost.sdk.internal.impression.ImpressionViewProtocolBuilder
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.logging.Logger.e
import com.chartboost.sdk.internal.measurement.OpenMeasurementController
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.PlayerState
import com.chartboost.sdk.legacy.VastVideoEvent
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.InfoEvent
import com.chartboost.sdk.tracking.TrackingEventName
import com.chartboost.sdk.view.CBImpressionActivity
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// -1 for protocols that that cannot retrieve a video state or protocol is not video protocol
const val IMPRESSION_WITHOUT_VIDEO_STATE = -1

// TODO Refactor this class
internal class AdUnitRenderer(
    private val adType: AdType,
    private val reachability: CBReachability,
    private val fileCache: FileCache,
    private val videoRepository: VideoRepository,
    private val impressionBuilder: ImpressionBuilder,
    private val adUnitRendererShowRequest: AdUnitRendererShowRequest,
    private val openMeasurementController: OpenMeasurementController,
    private val viewProtocolBuilder: ImpressionViewProtocolBuilder,
    private val rendererActivityBridge: RendererActivityBridge,
    private val nativeBridgeCommand: NativeBridgeCommand,
    private val templateLoader: TemplateLoader,
    val mediation: Mediation?,
    private val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val eventTracker: EventTrackerExtensions,
    private val endpointRepository: EndpointRepository,
) : AdUnitRendererImpressionCallback,
    ImpressionIntermediateCallback,
    ImpressionClickCallback,
    ImpressionInterface,
    AdUnitRendererActivityInterface,
    EventTrackerExtensions by eventTracker {
    private var callback: AdUnitRendererAdCallback? = null
    private var impression: CBImpression? = null

    /**
     * A map of impression ID to AppRequest objects.
     */
    private val appRequests = mutableMapOf<String, AppRequest>()

    /**
     * Removes any trace of banner ad and removes it from the view when requested
     */
    fun detachBannerImpression() {
        try {
            impression?.let {
                openMeasurementController.onImpressionDestroyWebview()
                it.getHostView()?.run {
                    removeAllViews()
                    invalidate()
                }
                it.viewProtocolDestroy()
                impression = null
                callback = null
            }
        } catch (e: Exception) {
            e("detachBannerImpression error", e)
        }
    }

    /**
     * Function prepares the AppRequest to be ready to display the ad. Prepares the
     * CBImpression object and passes the responsibility further to the view controller
     * and impression itself which communicates with via callback.
     * There is only one renderer per ad hence no need to worry about the callback
     */
    fun render(
        appRequest: AppRequest,
        callback: AdUnitRendererAdCallback,
    ) {
        this.callback = callback
        if (!reachability.isNetworkAvailable) {
            postErrorByAdType(appRequest, CBError.Impression.INTERNET_UNAVAILABLE_AT_SHOW)
            return
        }

        val adUnit = appRequest.adUnit
        if (adUnit == null) {
            reportError(appRequest, CBError.Impression.NO_AD_FOUND)
            return
        }

        // This happens when user tries to show after pressing cache but before cache success callback
        if (!fileCache.isAssetsAvailable(adUnit)) {
            reportError(appRequest, CBError.Impression.ASSET_MISSING)
            return
        }

        startShowTracking(appRequest)
        showReadyOrPrecache(appRequest)
    }

    private fun showReadyOrPrecache(appRequest: AppRequest) {
        if (appRequest.adUnit?.isPrecacheVideoAd == true) {
            videoRepository.downloadVideoFile(
                appRequest.adUnit?.videoUrl ?: "",
                appRequest.adUnit?.videoFilename ?: "",
                showImmediately = true,
            ) {
                showReady(appRequest)
            }
        } else {
            showReady(appRequest)
        }
    }

    private fun startShowTracking(appRequest: AppRequest) {
        if (!appRequest.isTrackedShow) {
            appRequest.isTrackedShow = true
            InfoEvent(
                TrackingEventName.Show.START,
                "",
                adType.name,
                appRequest.location,
            ).track()
        }
    }

    /**
     * Main handling of render action. Send the callback confirming that sdk is about to display an ad.
     * Build impression via ImpressionBuilder. For the banner store the impression locally to be
     * able to detach it later if needed. Lastly, update the impression state and pass the
     * responsibility to CBUIManager via SHOW_IMPRESSION_FOR_AD_UNIT command.
     *
     */
    private fun showReady(appRequest: AppRequest) {
        if (impression != null && appRequest.bannerData == null) {
            e("Fullscreen impression is currently loading.")
            return
        }

        if (!reachability.isNetworkAvailable) {
            postErrorByAdType(appRequest, CBError.Impression.INTERNET_UNAVAILABLE_AT_SHOW)
            return
        }

        callback?.onRequestedToShow(getImpressionIdFromAppRequest(appRequest))

        val impressionHolder =
            impressionBuilder.createImpressionHolderFromAppRequest(
                appRequest,
                callback = this,
                appRequest.bannerData?.bannerView,
                impressionIntermediateCallback = this,
                impressionClickCallback = this,
                viewProtocolBuilder,
                impressionInterface = this,
                webViewTimeoutInterface,
                nativeBridgeCommand,
                templateLoader,
            )

        impression = impressionHolder.impression
        showImpressionOrHandleError(
            appRequest,
            impressionHolder.impression,
            impressionHolder.error,
        )
    }

    private fun showImpressionOrHandleError(
        appRequest: AppRequest,
        impression: CBImpression?,
        error: CBError.Impression?,
    ) {
        if (error != null) {
            reportError(appRequest, error)
            removeAppRequest(appRequest)
            return
        }

        uiScope.launch {
            impression?.prepareAndDisplay()
                ?: reportError(appRequest, CBError.Impression.PENDING_IMPRESSION_ERROR)
        }
    }

    private fun reportError(
        appRequest: AppRequest,
        error: CBError.Impression,
    ) {
        postErrorByAdType(appRequest, error)
        if (error == CBError.Impression.NO_AD_FOUND) return
        appRequest.let {
            e(
                "reportError: adTypeTraits: ${adType.name}" +
                    " reason: ${CBConstants.REASON_CACHE}  format: web" +
                    " error: $error" +
                    " adId: ${it.adUnit?.adId}" +
                    " appRequest.location: ${it.location}",
            )
        }
    }

    private fun postErrorByAdType(
        appRequest: AppRequest,
        error: CBError.Impression,
    ) {
        callback?.onShowFailure(getImpressionIdFromAppRequest(appRequest), error) ?: Logger.d(
            "Missing AdUnitRendererAdCallback while sending onShowFailure with error: $error",
        )
    }

    private fun getImpressionIdFromAppRequest(appRequest: AppRequest?): String? {
        return appRequest?.adUnit?.impressionId
    }

    private fun notifyServerShow(appRequest: AppRequest) {
        adUnitRendererShowRequest.execute(
            url = endpointRepository.getEndPointUrl(adType.showEndPoint),
            showParams =
                ShowParamsModel(
                    appRequest.adUnit?.adId,
                    appRequest.location,
                    getVideoStateFromCurrentImpression(),
                    adType.name,
                    mediation,
                ),
        )
    }

    private fun onImpressionErrorReport(
        appRequest: AppRequest,
        error: CBError.Impression,
    ) {
        reportError(appRequest, error)
        if (error != CBError.Impression.IMPRESSION_ALREADY_VISIBLE) {
            removeAppRequest(appRequest)
        }
        openMeasurementController.destroyVisibilityTracker()
    }

    private fun removeAppRequest(appRequest: AppRequest) {
        appRequest.isTrackedShow = false
        appRequest.adUnit = null
    }

    override fun onImpressionReadyToBeDisplayed() {
        impression?.let {
            it.setImpressionState(ImpressionState.LOADED)
            if (it.shouldDisplayOnHostView()) {
                it.displayOnHostView(it.getHostView())
            } else {
                rendererActivityBridge.startActivity(this)
            }
        } ?: e("Cannot display missing impression onImpressionReadyToBeDisplayed")
    }

    override fun onActivityIsReadyToDisplay(activity: CBImpressionActivity) {
        impression?.let {
            it.displayOnActivity(it.getImpressionState(), activity)
            it.getViewProtocolView()?.let { viewBase ->
                rendererActivityBridge.displayViewOnActivity(viewBase)
            }
        } ?: e("Cannot display missing impression onActivityIsReadyToDisplay")
    }

    override fun impressionOnStart() {
        impression?.onStart()
    }

    override fun impressionOnResume() {
        impression?.onResume()
    }

    override fun impressionOnPause() {
        impression?.onPause()
    }

    override fun impressionOnDestroy() {
        impression?.closeImpression()
        nativeBridgeCommand.hideViewCallback = null
        nativeBridgeCommand.clearImpressionInterface()
    }

    override fun onConfigurationChange() {
        impression?.viewProtocolConfigurationChange()
    }

    override fun failure(error: CBError.Impression) {
        impression?.onFailure(error)
    }

    override fun onBackPressed(): Boolean {
        if (impression?.isPlayerPlaying() != true || impression?.canBeClosed() == true) {
            rendererActivityBridge.finishActivity()
        }
        return true
    }

    override fun onImpressionCloseTriggered(appRequest: AppRequest) {
        removeAppRequest(appRequest)
        openMeasurementController.destroyVisibilityTracker()
    }

    override fun onImpressionClicked(impressionId: String) {
        trackClick(TrackingEventName.Click.SUCCESS, "")
        callback?.onImpressionClicked(impressionId)
    }

    override fun onImpressionClickedFailed(
        impressionId: String,
        url: String?,
        error: CBError.Click,
    ) {
        trackClick(TrackingEventName.Click.FAILURE, error.name)
        callback?.onImpressionClickedFailed(impressionId, url, error)
    }

    private fun trackClick(
        trackName: TrackingEventName,
        msg: String,
    ) {
        InfoEvent(
            trackName,
            msg,
            adType.name,
            impression?.getLocation() ?: "No location",
            mediation,
        ).track()
    }

    override fun onImpressionRewarded(
        impressionId: String?,
        reward: Int,
    ) {
        callback?.onReward(impressionId, reward)
    }

    override fun onImpressionDismissed(impressionId: String?) {
        callback?.onImpressionDismissed(impressionId)
        openMeasurementController.destroyVisibilityTracker()
    }

    /**
     * Do not track any impressions in this method as it is template-driven and the template has its
     * own logic to track impressions. For SDK-driven impressions, the impression is tracked when
     * the visibility threshold is met, which is in the `onVisibilityTrackerThresholdMet`. The
     * `isVisible()` flag here is set to true in that method.
     */
    override fun onImpressionShownFully(appRequest: AppRequest) {
        setShowProcessed(true)
        val impressionId = getImpressionIdFromAppRequest(appRequest)
        impressionId?.let {
            appRequests[it] = appRequest
        } ?: e("Unable to store app request because impression ID is missing. Impression tracking will not work.")

        scheduleDismissMissing(appRequest.location)
        if (isVisible()) {
            sendImpressionSignals(impressionId)
        }
    }

    /**
     * 1. Schedule dismiss missing to be send after the show but before dismiss in case there
     *    is no dismiss callback.
     *
     * 2. Banner doesn't have dismiss callback.
     */
    private fun scheduleDismissMissing(location: String) {
        if (adType != AdType.Banner) {
            ErrorEvent(
                name = TrackingEventName.Show.DISMISS_MISSING,
                message = "dismiss_missing due to ad not finished",
                adType = adType.name,
                location = location,
                mediation = mediation,
            ).persist()
        }
    }

    private fun sendImpressionSignals(impressionId: String?) {
        impression?.wasImpressionSignaled = true
        callback?.onImpressionSuccess(impressionId)
        openMeasurementController.signalImpressionEvent()

        // By removing the appRequest, we are making sure that the event is not sent again, even
        // when sendImpressionSignals() is called multiple times for the same creative (e.g. when the
        // template triggers `.show` and when the visibility threshold is met a bit later).
        appRequests.remove(impressionId)?.let {
            callback?.onShowSuccess(impressionId)
            notifyServerShow(it)
        }
    }

    override fun onImpressionViewCreated(context: Context) {
        impression?.let {
            if (!openMeasurementController.isOmSdkEnabled()) {
                it.setIsVisible(true)
                Logger.d("Cannot create visibility tracker due to the OM SDK being disabled!")
                return
            }

            if (it.isViewProtocolViewNotCreated()) {
                e("Cannot create VisibilityTracker due to missing view!")
                return
            }

            it.getViewProtocolView()?.let { view ->
                openMeasurementController.createVisibilityTracker(
                    context,
                    view,
                    view.rootView,
                ) {
                    onVisibilityTrackerThresholdMet(it)
                }
            }
        } ?: e("Missing impression onImpressionViewCreated")
    }

    override fun closeActivity() {
        rendererActivityBridge.finishActivity()
    }

    override fun applyActivityOrientation(
        forceOrientation: Int,
        allowOrientationChange: Boolean,
    ) {
        rendererActivityBridge.applyOrientationProperties(forceOrientation, allowOrientationChange)
    }

    override fun restoreOriginalOrientation() {
        rendererActivityBridge.restoreOriginalOrientation()
    }

    private fun onVisibilityTrackerThresholdMet(impression: CBImpression) {
        e("Visibility check success!")
        impression.setIsVisible(true)
        // OM impression logic:
        // - visibility tracker is configurable and it measures the view based on the config
        //   and callas this visibility threshold met
        // - typical uses case would be: visibility was measured but show was not triggered yet
        //   hence we don't do anything because first show call needs to happen and then show will
        //   trigger the signals
        // - if show happened but visibility was not triggered by the om callback then we should trigger
        //   signals from inside of this callback when om measures visiblity
        // - signals should be send once per ad
        if (impression.getIsShowProcessed() && !impression.wasImpressionSignaled) {
            sendImpressionSignals(impression.getAdUnitImpressionId())
        }
    }

    override fun onImpressionError(
        appRequest: AppRequest,
        error: CBError.Impression,
    ) {
        onImpressionErrorReport(appRequest, error)
        CriticalEvent(
            TrackingEventName.Show.UNEXPECTED_DISMISS_ERROR,
            "",
            adType.name,
            appRequest.location,
            mediation,
        ).track()
        rendererActivityBridge.finishActivity()
    }

    // IMPRESSION CALLBACK
    private fun getVideoStateFromCurrentImpression(): Int {
        return impression?.getViewProtocolAssetDownloadStateNow() ?: IMPRESSION_WITHOUT_VIDEO_STATE
    }

    override fun destroyImpression() {
        impression?.viewProtocolDestroy()
        impression = null
        callback = null
    }

    override fun setImpressionClosed(close: Boolean) {
        impression?.setClosed(close)
    }

    override fun setImpressionClick(click: Boolean) {
        impression?.click = click
    }

    override fun callImpressionClickFailureCallback(
        url: String?,
        error: CBError.Click,
    ) {
        impression?.callImpressionClickFailureCallback(url, error)
            ?: Logger.d("Missing impression on impression click failure callback ")
    }

    override fun callImpressionClickSuccessCallback() {
        impression?.onImpressionClickSuccessCallback()
            ?: Logger.d("Missing impression on impression click success callback ")
    }

    override fun callDismissAfterClick() {
        if (impression?.getImpressionState() == ImpressionState.DISPLAYED && adType != AdType.Banner) {
            rendererActivityBridge.finishActivity()
        }
    }

    override fun setShowProcessed(showProcessed: Boolean) {
        impression?.setIsShowProcessed(showProcessed)
    }

    override fun isVisible(): Boolean {
        return impression?.getIsVisible() ?: false
    }

    override fun setImpressionState(state: ImpressionState) {
        impression?.setImpressionState(state)
    }

    override fun callOnClose() {
        impression?.callOnClose()
    }

    override fun callImpressionDismissCallback() {
        Logger.d("DISMISS_MISSING event was successfully removed upon dismiss callback")
        ErrorEvent(
            TrackingEventName.Show.DISMISS_MISSING,
            "",
            "",
            "",
        ).clearFromStorage()
        impression?.callImpressionDismissCallback()
    }

    private val webViewTimeoutInterface =
        object : WebViewTimeoutInterface {
            override fun onWebViewTimeoutReached() {
                impression?.onImpressionFailure(CBError.Impression.WEB_VIEW_PAGE_LOAD_TIMEOUT)
            }
        }

    override fun templateCloseEvent() {
        rendererActivityBridge.finishActivity()
    }

    override fun getAdUnitParameters(): String {
        return impression?.getAdUnitParameters() ?: ""
    }

    override fun getProtocolMaxSize(): String {
        return impression?.getProtocolMaxSize() ?: ""
    }

    override fun getProtocolScreenSize(): String {
        return impression?.getProtocolScreenSize() ?: ""
    }

    override fun getProtocolCurrentPosition(): String {
        return impression?.getProtocolCurrentPosition() ?: ""
    }

    override fun getProtocolDefaultPosition(): String {
        return impression?.getProtocolDefaultPosition() ?: ""
    }

    override fun getProtocolOrientationProperties(): String {
        return impression?.getProtocolOrientationProperties() ?: ""
    }

    override fun sendWebViewVastOmEvent(vastVideoEvent: VastVideoEvent) {
        impression?.sendWebViewVastOmEvent(vastVideoEvent)
    }

    override fun updatePlayerState(playerState: PlayerState) {
        impression?.updatePlayerState(playerState)
    }

    override fun templateVideoCompletedEvent() {
        impression?.templateVideoCompletedEvent()
    }

    override fun setVideoPosition(pos: Float) {
        impression?.setVideoPosition(pos)
    }

    override fun setVideoDuration(pos: Float) {
        impression?.setVideoDuration(pos)
    }

    override fun sendQuartileEvent(
        videoDuration: Float,
        currentInSec: Float,
    ) {
        impression?.sendQuartileEvent(videoDuration, currentInSec)
    }

    override fun warning(msg: String) {
        e("WebView warning occurred closing the webview $msg")
    }

    override fun onShowImpression() {
        impression?.onShowImpression()
    }

    /**
     * In case of an error reported by webview, grant reward to a user only if rewarded ad was
     * already displayed
     */
    override fun impressionNotifyDidCompleteRewardedOnError() {
        impression?.impressionNotifyDidCompleteRewardedOnError()
    }

    override fun sendWebViewVASTTrackingEvents(event: String) {
        impression?.sendWebViewVASTTrackingEvents(event)
    }

    // OPEN URL template event provides the url and should dismiss boolean
    override fun onTemplateOpenURLEvent(url: CBUrl) {
        impression?.onOpenURL(url)
    }

    // CLICK template event provides should dismiss boolean and url provided by the ad response
    override fun onTemplateClickEvent(cbUrl: CBUrl) {
        impression?.templateClickEvent(cbUrl.shouldDismiss)
    }

    // Used to open URLs WITHOUT triggering a click event
    override fun onOpenNonClickURL(url: CBUrl) {
        impression?.onOpenNonClickURL(url)
    }

    override fun onClickBeforeLoadFinished(url: CBUrl) {
        impression?.clickTriggeredBeforeLoadFinished(url)
    }

    override fun setOrientationProperties(
        allowOrientationChange: Boolean,
        forceOrientation: String,
    ) {
        impression?.setOrientationProperties(allowOrientationChange, forceOrientation)
    }

    override fun templateRewardEvent() {
        impression?.impressionNotifyDidCompleteAd()
    }

    override fun onRewardedVideoCompleted() {
        impression?.onRewardedVideoCompleted()
    }

    override fun playVideo() {
        impression?.playVideo()
    }

    override fun pauseVideo() {
        impression?.pauseVideo()
    }

    override fun closeVideo() {
        impression?.closeVideo()
    }

    override fun mute() {
        impression?.muteVideo()
    }

    override fun unmute() {
        impression?.unmuteVideo()
    }

    override fun passVerificationResourcesFromTemplate(verificationScriptResourceList: List<VerificationScriptResource>) {
        impression?.passVerificationResourcesFromTemplate(verificationScriptResourceList)
    }
}

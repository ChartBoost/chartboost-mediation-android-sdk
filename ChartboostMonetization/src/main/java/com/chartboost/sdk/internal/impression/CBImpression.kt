package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.AdUnitManager.render.IMPRESSION_WITHOUT_VIDEO_STATE
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionState
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.clickthrough.ImpressionClickable
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.video.VideoProtocol
import com.chartboost.sdk.legacy.PlayerState
import com.chartboost.sdk.legacy.VastVideoEvent
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource

internal class CBImpression(
    private val impressionDependency: ImpressionDependency,
    impressionClick: ImpressionClickable,
    impressionDismiss: ImpressionDismissable,
    impressionComplete: ImpressionCompletable,
    impressionView: ImpressionViewable,
) : ImpressionClickable by impressionClick,
    ImpressionDismissable by impressionDismiss,
    ImpressionCompletable by impressionComplete,
    ImpressionViewable by impressionView,
    ImpressionStateInterface {
    private var state: ImpressionState = ImpressionState.LOADING

    fun prepareAndDisplay() {
        state = ImpressionState.LOADING
        val error = impressionDependency.viewProtocol.prepare()
        if (error == null) {
            sendImpressionReadyToBeDisplayedCallback()
        } else {
            onImpressionFailure(error)
        }
    }

    fun sendImpressionCompleteRequest() {
        sendVideoCompleteRequest(
            impressionDependency.location,
            impressionDependency.viewProtocol.videoPosition,
            impressionDependency.viewProtocol.videoDuration,
        )
    }

    fun onShowImpression() {
        if (state == ImpressionState.DISPLAYED) {
            if (!getIsVideoShowSent()) {
                shownFully()
                setIsVideoShowSent(true)
            }
        }
    }

    fun onImpressionClickSuccessCallback() {
        submitClickRequest(
            impressionDependency.location,
            impressionDependency.viewProtocol.videoPosition,
            impressionDependency.viewProtocol.videoDuration,
        )
        callImpressionClickSuccessCallback()
    }

    override fun getImpressionState(): ImpressionState {
        return state
    }

    override fun setImpressionState(newState: ImpressionState) {
        state = newState
    }

    fun callOnClose() {
        onClose(state)
    }

    fun canBeClosed(): Boolean {
        return impressionDependency.adTypeTraits.canBeClosed
    }

    fun getLocation(): String {
        return impressionDependency.location
    }

    fun isImpressionRewarded(): Boolean {
        return impressionDependency.adTypeTraits == AdType.Rewarded
    }

    fun shouldDisplayOnHostView(): Boolean {
        return impressionDependency.adTypeTraits.shouldDisplayOnHostView
    }

    fun getAdUnitImpressionId(): String {
        return impressionDependency.adUnit.impressionId
    }

    fun getAdUnitTemplate(): String {
        return impressionDependency.adUnit.template
    }

    // ViewProtocol Wrapping functions
    fun viewProtocolDestroy() {
        impressionDependency.viewProtocol.destroy()
    }

    fun isViewProtocolViewNotCreated(): Boolean {
        return impressionDependency.viewProtocol.view == null ||
            impressionDependency.viewProtocol.view?.rootView == null
    }

    fun getViewProtocolView(): ViewBase? {
        return impressionDependency.viewProtocol.view
    }

    fun getViewProtocolAssetDownloadStateNow(): Int {
        if (impressionDependency.viewProtocol is VideoProtocol) {
            return impressionDependency.viewProtocol.getAssetDownloadStateNow()
        }
        return IMPRESSION_WITHOUT_VIDEO_STATE
    }

    fun viewProtocolConfigurationChange() {
        impressionDependency.viewProtocol.onConfigurationChange()
    }

    fun onImpressionFailure(error: CBError.Impression) {
        if (getIsVideoShowSent()) {
            impressionDependency.adUnitRendererImpressionCallback.closeActivity()
            return
        }
        onFailure(error)
    }

    fun impressionNotifyDidCompleteRewardedOnError() {
        if (getIsVideoShowSent() && impressionDependency.adTypeTraits == AdType.Rewarded) {
            impressionNotifyDidCompleteAd()
        }
    }

    fun sendWebViewVASTTrackingEvents(event: String) {
        if (event.isNotEmpty()) {
            impressionDependency.adUnit.events[event]?.forEach { url ->
                impressionDependency.viewProtocol.sendRequest(url)
            }
        }
    }

    fun setOrientationProperties(
        allowOrientationChange: Boolean,
        forceOrientation: String,
    ) {
        impressionDependency.viewProtocol.setOrientationProperties(
            allowOrientationChange,
            forceOrientation,
        )
    }

    fun onRewardedVideoCompleted() {
        if (impressionDependency.impressionCounter.onRewardedVideoCompletedPlayCount <= 1) {
            impressionSendVideoCompleteRequest()
            impressionDependency.impressionCounter.onRewardedVideoCompletedPlayCount++
        }
    }

    fun playVideo() {
        try {
            (impressionDependency.viewProtocol as VideoProtocol).playVideo()
        } catch (e: Exception) {
            Logger.e("Invalid play video command", e)
        }
    }

    fun pauseVideo() {
        try {
            (impressionDependency.viewProtocol as VideoProtocol).pauseVideo()
        } catch (e: Exception) {
            Logger.e("Invalid pause video command", e)
        }
    }

    fun closeVideo() {
        try {
            (impressionDependency.viewProtocol as VideoProtocol).closeVideo()
        } catch (e: Exception) {
            Logger.e("Invalid close video command", e)
        }
    }

    fun muteVideo() {
        try {
            if (impressionDependency.viewProtocol is VideoProtocol) {
                impressionDependency.viewProtocol.muteVideo()
            } else {
                impressionDependency.viewProtocol.mute()
                impressionDependency.viewProtocol.sendWebViewVastOmEvent(VastVideoEvent.VOLUME_CHANGE)
            }
        } catch (e: Exception) {
            Logger.e("Invalid mute video command", e)
        }
    }

    fun unmuteVideo() {
        try {
            if (impressionDependency.viewProtocol is VideoProtocol) {
                impressionDependency.viewProtocol.unmuteVideo()
            } else {
                impressionDependency.viewProtocol.unmute()
                impressionDependency.viewProtocol.sendWebViewVastOmEvent(VastVideoEvent.VOLUME_CHANGE)
            }
        } catch (e: Exception) {
            Logger.e("Invalid unmute video command", e)
        }
    }

    fun passVerificationResourcesFromTemplate(verificationScriptResourceList: List<VerificationScriptResource>) {
        impressionDependency.viewProtocol.passVerificationResourcesFromTemplate(
            verificationScriptResourceList,
        )
    }

    fun sendQuartileEvent(
        videoDuration: Float,
        currentInSec: Float,
    ) {
        impressionDependency.viewProtocol.sendQuartileEvent(videoDuration, currentInSec)
    }

    fun setVideoDuration(pos: Float) {
        impressionDependency.viewProtocol.videoDuration = pos
    }

    fun setVideoPosition(pos: Float) {
        impressionDependency.viewProtocol.videoPosition = pos
    }

    fun templateVideoCompletedEvent() {
        if (impressionDependency.impressionCounter.onVideoCompletedPlayCount <= 1) {
            impressionNotifyDidCompleteAd()
            impressionSendVideoCompleteRequest()
            impressionDependency.impressionCounter.onVideoCompletedPlayCount++
        }
    }

    fun updatePlayerState(playerState: PlayerState) {
        impressionDependency.viewProtocol.updatePlayerState(playerState)
    }

    fun isPlayerPlaying(): Boolean {
        return impressionDependency.viewProtocol.isPlayerPlaying()
    }

    fun sendWebViewVastOmEvent(vastVideoEvent: VastVideoEvent) {
        impressionDependency.viewProtocol.sendWebViewVastOmEvent(vastVideoEvent)
    }

    fun templateClickEvent(shouldDismiss: Boolean?) {
        onClick(shouldDismiss, state)
    }

    fun errorWebView(error: String) {
        impressionDependency.viewProtocol.onWebViewError(error).run {
            onImpressionFailure(this)
        }
    }

    fun getProtocolOrientationProperties(): String {
        return impressionDependency.viewProtocol.orientationProperties
    }

    fun getProtocolDefaultPosition(): String {
        return impressionDependency.viewProtocol.defaultPosition
    }

    fun getProtocolCurrentPosition(): String {
        return impressionDependency.viewProtocol.currentPosition
    }

    fun getProtocolScreenSize(): String {
        return impressionDependency.viewProtocol.screenSize
    }

    fun getProtocolMaxSize(): String {
        return impressionDependency.viewProtocol.maxSize
    }

    fun getAdUnitParameters(): String {
        return impressionDependency.adUnit.getParametersAsString()
    }

    fun impressionNotifyDidCompleteAd() {
        if (impressionDependency.impressionCounter.impressionNotifyDidCompleteAdPlayCount <= 1) {
            notifyDidCompleteAd()
            impressionDependency.impressionCounter.impressionNotifyDidCompleteAdPlayCount++
        }
    }

    private fun impressionSendVideoCompleteRequest() {
        if (impressionDependency.impressionCounter.impressionSendVideoCompleteRequestPlayCount <= 1) {
            sendImpressionCompleteRequest()
            impressionDependency.impressionCounter.impressionSendVideoCompleteRequestPlayCount++
        }
    }
}

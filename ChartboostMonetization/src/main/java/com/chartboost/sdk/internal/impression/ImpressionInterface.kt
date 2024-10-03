package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.clickthrough.CBUrl
import com.chartboost.sdk.legacy.PlayerState
import com.chartboost.sdk.legacy.VastVideoEvent
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource

internal interface ImpressionInterface {
    fun getAdUnitParameters(): String {
        return ""
    }

    fun getProtocolMaxSize(): String {
        return ""
    }

    fun getProtocolScreenSize(): String {
        return ""
    }

    fun getProtocolCurrentPosition(): String {
        return ""
    }

    fun getProtocolDefaultPosition(): String {
        return ""
    }

    fun getProtocolOrientationProperties(): String {
        return ""
    }

    fun onWebViewError(error: String): CBError.Impression = CBError.Impression.INTERNAL

    fun templateCloseEvent() { }

    fun sendWebViewVastOmEvent(vastVideoEvent: VastVideoEvent) { }

    fun updatePlayerState(playerState: PlayerState) { }

    fun templateVideoCompletedEvent() { }

    fun setVideoPosition(pos: Float) { }

    fun setVideoDuration(pos: Float) { }

    fun sendQuartileEvent(
        videoDuration: Float,
        currentInSec: Float,
    ) { }

    fun warning(msg: String) { }

    fun onShowImpression() { }

    fun impressionNotifyDidCompleteRewardedOnError() { }

    fun sendWebViewVASTTrackingEvents(event: String) { }

    fun onTemplateClickEvent(cbUrl: CBUrl) { }

    fun onTemplateOpenURLEvent(url: CBUrl) { }

    fun onOpenNonClickURL(url: CBUrl) { }

    fun onClickBeforeLoadFinished(url: CBUrl) { }

    fun setOrientationProperties(
        allowOrientationChange: Boolean,
        forceOrientation: String,
    ) { }

    fun templateRewardEvent() { }

    fun onRewardedVideoCompleted() { }

    fun playVideo() { }

    fun pauseVideo() { }

    fun closeVideo() { }

    fun mute() { }

    fun unmute() { }

    fun passVerificationResourcesFromTemplate(verificationScriptResourceList: List<VerificationScriptResource>) { }
}

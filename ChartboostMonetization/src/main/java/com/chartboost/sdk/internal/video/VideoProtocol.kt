package com.chartboost.sdk.internal.video

import android.content.Context
import android.view.SurfaceView
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.WebView.CBWebView
import com.chartboost.sdk.internal.WebView.NativeBridgeCommand
import com.chartboost.sdk.internal.di.AdsVideoPlayerFactory
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.logging.Logger.d
import com.chartboost.sdk.internal.logging.Logger.e
import com.chartboost.sdk.internal.logging.Logger.i
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.utils.now
import com.chartboost.sdk.internal.video.player.AdsVideoPlayer
import com.chartboost.sdk.internal.video.player.AdsVideoPlayerListener
import com.chartboost.sdk.internal.video.player.BackgroundListener
import com.chartboost.sdk.internal.video.player.ScreenOrientationChangeListener
import com.chartboost.sdk.internal.video.repository.DownloadState
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.internal.video.repository.VideoRepository.Companion.VIDEO_STATE_EMPTY
import com.chartboost.sdk.legacy.CBViewProtocol
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.InfoEvent
import com.chartboost.sdk.tracking.TrackingEventName
import com.iab.omid.library.chartboost.adsession.media.PlayerState

// TODO Big constructor, probably wants to be refactored

/**
 * VideoProtocol handles media player and local server operation
 */
internal class VideoProtocol(
    context: Context,
    location: String,
    mtype: MediaTypeOM,
    adUnitParameters: String,
    uiPoster: UiPoster,
    private val fileCache: FileCache,
    templateProxy: CBTemplateProxy,
    private val videoRepository: VideoRepository,
    private val videoFilename: String,
    private val mediation: Mediation?,
    private val adsVideoPlayerFactory: AdsVideoPlayerFactory,
    networkService: CBNetworkService,
    private val templateHtml: String,
    openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
    private val impressionInterface: ImpressionInterface,
    webViewTimeoutInterface: WebViewTimeoutInterface,
    private val nativeBridgeCommand: NativeBridgeCommand,
    private val eventTracker: EventTrackerExtensions,
    private val cbWebViewFactory: (Context) -> CBWebView = { CBWebView(it) },
) : CBViewProtocol(
        context = context,
        location = location,
        adUnitMType = mtype,
        adTypeTraitsName = adUnitParameters,
        uiPoster = uiPoster,
        fileCache = fileCache,
        networkRequestService = networkService,
        templateProxy = templateProxy,
        mediation = mediation,
        templateHtml = templateHtml,
        openMeasurementImpressionCallback = openMeasurementImpressionCallback,
        adUnitRendererCallback = adUnitRendererImpressionCallback,
        webViewTimeoutInterface = webViewTimeoutInterface,
        eventTracker = eventTracker,
    ),
    AdsVideoPlayerListener {
    private var protocolVideoDuration = 0L
    private var videoPlayTimestamp = 0L
    private var videoStartTimestamp = 0L
    private var getAssetDownloadStateAtVideoStart = VIDEO_STATE_EMPTY

    private var videoBase: VideoBase? = null

    private var videoPlayer: AdsVideoPlayer? = null

    override fun createViewObject(context: Context): ViewBase? {
        nativeBridgeCommand.setImpressionInterface(impressionInterface)
        d("createViewObject()")

        val surface: SurfaceView
        try {
            surface = SurfaceView(context)
        } catch (e: Exception) {
            onWebViewError("Can't instantiate SurfaceView: $e")
            return null
        }

        videoBase =
            try {
                VideoBase(
                    context = context,
                    html = templateHtml,
                    callback = customWebViewInterface,
                    nativeBridgeCommand = nativeBridgeCommand,
                    baseExternalPathURL = baseExternalPathURL,
                    surface = surface,
                    eventTracker = eventTracker,
                    cbWebViewFactory = cbWebViewFactory,
                )
            } catch (e: Exception) {
                onWebViewError("Can't instantiate VideoBase: $e")
                null
            }

        videoPlayer =
            adsVideoPlayerFactory(
                context,
                surface,
                this,
                uiPoster,
                fileCache,
            ).apply {
                videoRepository.getVideoAsset(videoFilename)?.let {
                    asset(it)
                } ?: e("Video asset not found in the repository")
            }

        return videoBase
    }

    override fun destroy() {
        d("destroyView()")
        // view was cancelled by the user, for example back button, need to detach media player to
        // avoid memory leak otherwise it will keep on playing in the background
        detachVideoPlayer()
        super.destroy()
    }

    override fun onResume() {
        i("onResume()")
        // Added this for safety to trigger the queue in case we detect foreground event during show,
        // this won't do anything if there is download going on or queue is empty
        videoRepository.startDownloadIfPossible(null, 1, false)
        videoPlayer?.run {
            (this as? BackgroundListener)?.onComingFromBackground()
            play()
        }
        super.onResume()
    }

    override fun onPause() {
        i("onPause()")
        videoPlayer?.pause()
        super.onPause()
    }

    override fun onConfigurationChange() {
        val w = videoBase?.width ?: 0
        val h = videoBase?.height ?: 0
        (videoPlayer as? ScreenOrientationChangeListener)?.onScreenOrientationChange(w, h)
    }

    // This is called by the template
    fun playVideo() {
        d("playVideo()")
        sendOpenMeasurementStartEvent()
        videoPlayTimestamp = now()
        videoPlayer?.play()
    }

    private fun sendOpenMeasurementStartEvent() {
        openMeasurementImpressionCallback.onImpressionNotifyStateChanged(PlayerState.FULLSCREEN)
        if (videoPlayer?.wasMediaStartedForTheFirstTime() == false) {
            openMeasurementImpressionCallback.onImpressionNotifyVideoStarted(
                protocolVideoDuration / 1000f,
                videoPlayer?.volume() ?: 1f,
            )
        } else {
            openMeasurementImpressionCallback.onImpressionNotifyVideoResumed()
        }
    }

    fun pauseVideo() {
        d("pauseVideo()")
        // pause video received from the template or on going to background
        openMeasurementImpressionCallback.onImpressionNotifyVideoPaused()
        videoPlayer?.pause()
    }

    fun closeVideo() {
        // close video received from the template
        detachVideoPlayer()
    }

    fun muteVideo() {
        // mute video received from the template
        videoPlayer?.mute()
        openMeasurementImpressionCallback.onImpressionNotifyVolumeChanged(0f)
    }

    fun unmuteVideo() {
        // unmute video received from the template
        videoPlayer?.unmute()
        openMeasurementImpressionCallback.onImpressionNotifyVolumeChanged(1f)
    }

    fun getAssetDownloadStateNow(): DownloadState {
        d("getAssetDownloadStateNow()")
        return videoRepository.getVideoAsset(videoFilename)?.let {
            videoRepository.getVideoDownloadState(it)
        } ?: VIDEO_STATE_EMPTY
    }

    override fun onVideoDisplayStarted() {
        d("onVideoDisplayStarted")
        // Here we send even to the template
        notifyTemplateVideoStarted()
        videoStartTimestamp = now()
    }

    override fun onVideoDisplayPrepared(duration: Long) {
        d("onVideoDisplayPrepared ready to receive signal from template, duration: $duration")
        getAssetDownloadStateAtVideoStart = getAssetDownloadStateNow()
        protocolVideoDuration = duration
        onPageFinishedWebView()
    }

    override fun onVideoDisplayProgress(position: Long) {
        val posInSeconds = position / 1000f
        val durationInSeconds = protocolVideoDuration / 1000f
        if (SandboxBridgeSettings.isSandboxMode) {
            i("onVideoDisplayProgress: $posInSeconds/$durationInSeconds")
        }
        templateProxy?.callOnPlaybackTimeJSFunction(
            getWebView(),
            posInSeconds,
            location,
            adTypeTraitsName,
        )
        sendQuartileEvent(durationInSeconds, posInSeconds)
    }

    override fun onVideoDisplayCompleted() {
        d("onVideoDisplayCompleted")
        sendTrackingEvent(true)
        notifyTemplateVideoEnded()
        openMeasurementImpressionCallback.onImpressionNotifyVideoComplete()
    }

    override fun onVideoDisplayError(error: String) {
        d("onVideoDisplayError: $error")
        sendTrackingEvent(false)
        templateProxy?.callOnVideoFailedJSFunction(
            getWebView(),
            location,
            adTypeTraitsName,
        )
        // common error function in protocol that handles tracking and removes the template in case of an error
        // it is done this way to avoid crash and at this point surface view needs to be removed from the view
        detachVideoPlayer()
        onWebViewError(error)
    }

    override fun removeAssetOnError() {
        // on specific error, remove the asset
        videoRepository.run {
            val isRemoved = removeAsset(getVideoAsset(videoFilename))
            e("Video asset: $videoFilename was removed: $isRemoved")
        }
    }

    override fun onVideoBufferStart() {
        openMeasurementImpressionCallback.onImpressionNotifyVideoBuffer(true)
    }

    override fun onVideoBufferFinish() {
        openMeasurementImpressionCallback.onImpressionNotifyVideoBuffer(false)
    }

    private fun detachVideoPlayer() {
        videoPlayer?.stop()
        videoBase?.removeSurfaceView()
        videoPlayer = null
        videoBase = null
    }

    private fun notifyTemplateVideoStarted() {
        d("notifyTemplateVideoStarted() duration: $protocolVideoDuration")
        templateProxy?.callOnVideoStartedJSFunction(
            getWebView(),
            protocolVideoDuration / 1000f,
            location,
            adTypeTraitsName,
        )
    }

    private fun notifyTemplateVideoEnded() {
        templateProxy?.callOnVideoEndedJSFunction(
            getWebView(),
            location,
            adTypeTraitsName,
        )
    }

    private fun getWebView(): CBWebView? {
        return videoBase?.webView
    }

    private fun sendTrackingEvent(infoEvent: Boolean) {
        val trackingMsg = getAssetDownloadStateAtVideoStart.toString()
        if (infoEvent) {
            trackVideoFinishSuccess(trackingMsg)
        } else {
            trackVideoFinishFailure(trackingMsg)
        }
    }

    private fun trackVideoFinishSuccess(trackingMsg: String) {
        InfoEvent(
            TrackingEventName.Video.FINISH_SUCCESS,
            trackingMsg,
            adTypeTraitsName,
            location,
            mediation,
        ).apply {
            latency = (videoStartTimestamp - videoPlayTimestamp).toFloat()
            isLatencyEvent = true
            shouldCalculateLatency = false
        }.track()
    }

    private fun trackVideoFinishFailure(trackingMsg: String) {
        ErrorEvent(
            TrackingEventName.Video.FINISH_FAILURE,
            trackingMsg,
            adTypeTraitsName,
            location,
            mediation,
        ).apply {
            // check if video haven't started yet and produce negative value from the play event
            latency =
                if (videoStartTimestamp == 0L) {
                    (videoPlayTimestamp - now()).toFloat()
                } else {
                    (now() - videoStartTimestamp).toFloat()
                }
            isLatencyEvent = true
            shouldCalculateLatency = false
        }.track()
    }

    companion object {
        @JvmStatic
        fun instance(
            context: Context,
            location: String,
            mtype: MediaTypeOM,
            adUnitParameters: String,
            uiPoster: UiPoster,
            fileCache: FileCache,
            templateProxy: CBTemplateProxy,
            videoRepository: VideoRepository,
            videoFilename: String,
            mediation: Mediation?,
            adsVideoPlayerFactory: AdsVideoPlayerFactory,
            networkService: CBNetworkService,
            templateHtml: String,
            openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
            adUnitRendererImpressionCallback: AdUnitRendererImpressionCallback,
            impressionInterface: ImpressionInterface,
            webViewTimeoutInterface: WebViewTimeoutInterface,
            nativeBridgeCommand: NativeBridgeCommand,
            eventTracker: EventTrackerExtensions,
        ): VideoProtocol =
            VideoProtocol(
                context = context,
                location = location,
                mtype = mtype,
                adUnitParameters = adUnitParameters,
                uiPoster = uiPoster,
                fileCache = fileCache,
                templateProxy = templateProxy,
                videoRepository = videoRepository,
                videoFilename = videoFilename,
                mediation = mediation,
                adsVideoPlayerFactory = adsVideoPlayerFactory,
                networkService = networkService,
                templateHtml = templateHtml,
                openMeasurementImpressionCallback = openMeasurementImpressionCallback,
                adUnitRendererImpressionCallback = adUnitRendererImpressionCallback,
                impressionInterface = impressionInterface,
                webViewTimeoutInterface = webViewTimeoutInterface,
                nativeBridgeCommand = nativeBridgeCommand,
                eventTracker = eventTracker,
            )
    }
}

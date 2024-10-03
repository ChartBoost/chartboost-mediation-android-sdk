package com.chartboost.sdk.legacy

import android.content.Context
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.AdUnitManager.parsers.MediaTypeOM
import com.chartboost.sdk.internal.AdUnitManager.render.AdUnitRendererImpressionCallback
import com.chartboost.sdk.internal.Libraries.CBJSON
import com.chartboost.sdk.internal.Libraries.FileCache
import com.chartboost.sdk.internal.Model.CBError.Impression
import com.chartboost.sdk.internal.Networking.CBNetworkService
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.WebView.CBTemplateProxy
import com.chartboost.sdk.internal.WebView.CustomWebViewInterface
import com.chartboost.sdk.internal.impression.ImpressionTrackerRequest
import com.chartboost.sdk.internal.impression.ImpressionTrackerRequestFactory
import com.chartboost.sdk.internal.impression.WebViewTimeoutInterface
import com.chartboost.sdk.internal.logging.Logger.d
import com.chartboost.sdk.internal.logging.Logger.e
import com.chartboost.sdk.internal.measurement.OpenMeasurementImpressionCallback
import com.chartboost.sdk.internal.measurement.Quartile
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName
import com.chartboost.sdk.view.CBImpressionActivity
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import java.util.Collections

private const val WEB_VIEW_PAGE_LOAD_TIMEOUT_DELAY = 15_000L

internal abstract class CBViewProtocol(
    protected val context: Context,
    val location: String,
    private val adUnitMType: MediaTypeOM,
    val adTypeTraitsName: String,
    protected val uiPoster: UiPoster,
    private val fileCache: FileCache,
    private val networkRequestService: CBNetworkService?,
    protected val templateProxy: CBTemplateProxy?,
    private val mediation: Mediation?,
    private val templateHtml: String?,
    protected val openMeasurementImpressionCallback: OpenMeasurementImpressionCallback,
    private val adUnitRendererCallback: AdUnitRendererImpressionCallback,
    private val webViewTimeoutInterface: WebViewTimeoutInterface,
    private val eventTracker: EventTrackerExtensions,
    private val impressionTrackerRequestFactory: ImpressionTrackerRequestFactory = ::ImpressionTrackerRequest,
) : EventTrackerExtensions by eventTracker {
    var baseExternalPathURL: String? = null
        private set

    private var adWebViewIntializeTime: Long = 0

    private var adWebViewFinishTime: Long = 0
    private var isPageFinishedLoading = false

    // MRAID 2.0 related implementation
    // https://www.iab.com/wp-content/uploads/2015/08/IAB_MRAID_v2_FINAL.pdf
    @VisibleForTesting
    var screenWidth = 0
        private set

    @VisibleForTesting
    var screenHeight = 0
        private set

    private var maxContainerWidth = 0
    private var maxContainerHeight = 0
    private var contentViewTop = 0

    @VisibleForTesting
    var defaultXPos = 0

    @VisibleForTesting
    var defaultYPos = 0

    @VisibleForTesting
    var defaultWidth = 0

    @VisibleForTesting
    var defaultHeight = 0

    private var currentXPos = 0
    private var currentYPos = 0
    private var currentWidth = 0
    private var currentHeight = 0
    private var allowOrientationChange = true
    private var forceOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    // END MRAID

    private var state = PlayerState.PLAYING

    /**
     * return the view associated with this view protocol
     */
    var view: ViewBase? = null
        private set

    var videoDuration = 0f

    // TODO This is weird to have here if it's never accessed here
    var videoPosition = 0f

    private var templateVideoVolume = 0f

    /**
     * create the actual view object
     */
    abstract fun createViewObject(context: Context): ViewBase?

    abstract fun onConfigurationChange()

    /**
     * prepare this view to load the given server JSON response
     */
    fun prepare(): Impression? {
        val baseDir = fileCache.currentLocations().baseDir
        if (baseDir == null) {
            e("External Storage path is unavailable or media not mounted")
            return Impression.ERROR_LOADING_WEB_VIEW
        }

        baseExternalPathURL = "file://" + baseDir.absolutePath + "/"
        if (templateHtml?.isEmpty() == true) {
            e("Empty template being passed in the response")
            return Impression.ERROR_DISPLAYING_VIEW
        }
        return null
    }

    open fun onResume() {
        view?.run {
            webView?.let { wv ->
                templateProxy?.run {
                    callOnForegroundJSFunction(
                        wv,
                        location,
                        adTypeTraitsName,
                    )
                    wv.onResume()
                }
            }
        }
    }

    open fun onPause() {
        view?.webView?.let { wv ->
            templateProxy?.run {
                callOnBackgroundJSFunction(
                    wv,
                    location,
                    adTypeTraitsName,
                )
                wv.onPause()
            }
        }
    }

    open fun destroy() {
        openMeasurementImpressionCallback.onImpressionDestroyWebview()
        view?.apply {
            destroyWebview()
            removeAllViews()
        }
        view = null
    }

    fun tryCreatingViewOnHostView(hostView: ViewGroup?): Impression? {
        if (view == null) {
            if (hostView?.context != null) {
                view = createViewObject(hostView.context)
            } else {
                return Impression.ERROR_CREATING_VIEW
            }
        }
        return null
    }

    /**
     * Attempt to create the view associated with this view protocol's impression.
     * Fails if it is not time to create the view, or there was an error trying to
     * create the view (no current activity, for example).
     *
     * @return An error status code if unsuccessful, otherwise null.
     */
    @Suppress("ReturnCount")
    fun tryCreatingViewOnActivity(activity: CBImpressionActivity): Impression? {
        if (view == null) {
            view = createViewObject(activity.applicationContext)
        }
        adUnitRendererCallback.onImpressionViewCreated(context)
        return null
    }

    private fun trackImpression(
        name: TrackingEventName,
        message: String?,
    ) {
        CriticalEvent(
            name,
            message ?: "no message",
            adTypeTraitsName,
            location,
            mediation,
        ).track()
    }

    /**
     * Public function that template triggers when receives an error
     * @param error
     */
    fun onWebViewError(error: String): Impression {
        trackImpression(TrackingEventName.Show.WEBVIEW_ERROR, error)
        e(error)
        isPageFinishedLoading = true
        return Impression.WEB_VIEW_CLIENT_RECEIVED_ERROR
    }

    fun registerWebViewTimeout() {
        uiPoster(WEB_VIEW_PAGE_LOAD_TIMEOUT_DELAY) {
            if (!isPageFinishedLoading) {
                d(
                    "Webview seems to be taking more time loading the html content, so closing the view.",
                )
                trackImpression(TrackingEventName.Show.TIMEOUT_EVENT, "")
                webViewTimeoutInterface.onWebViewTimeoutReached()
            }
        }
    }

    open fun onPageFinishedWebView() {
        isPageFinishedLoading = true
        adWebViewFinishTime = System.currentTimeMillis()
        d(
            "Total web view load response time " + (adWebViewFinishTime - adWebViewIntializeTime) / 1000,
        )

        view?.context?.let {
            calcAndSetMaxScreenSize(it)
        }

        view?.webView?.let { webview ->
            calcAndSetDisplayableMaxSize(webview)
            calculatePosition()
        }
    }

    // MRAID 2.0
    internal fun calcAndSetMaxScreenSize(context: Context) {
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    /**
     * MRAID 2.0 According to the docs we need to calculate container size for mraid
     * Previous implementation was using window to calculate the size but context was never
     * Activity context hence values were never calculated. Currently, webview sets the size
     * as it is an container
     */
    internal fun calcAndSetDisplayableMaxSize(webView: WebView) {
        maxContainerWidth = webView.width
        maxContainerHeight = webView.height
    }

    // MRAID 2.0
    internal fun calculatePosition() {
        val view = view
        if (view == null || !isPageFinishedLoading) {
            currentXPos = defaultXPos
            currentYPos = defaultYPos
            currentWidth = defaultWidth
            currentHeight = defaultHeight
            return
        }

        // This is the default location regardless of the state of the AdView/Webview/container.
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val x = location[0] // number of DIP offset from left of getMaxSize()
        var y = location[1] // number of DIP offset from top of getMaxSize()
        y -= contentViewTop
        val width = view.width // width of container
        val height = view.height // height of container
        defaultXPos = x
        defaultYPos = y
        defaultWidth = x + width
        defaultHeight = y + height

        // we are not supporting resize so default = current as of now
        currentXPos = defaultXPos
        currentYPos = defaultYPos
        currentWidth = defaultWidth
        currentHeight = defaultHeight
        d("CalculatePosition: defaultXPos: $defaultXPos , currentXPos: $currentXPos")
    }

    // Create MAP options
    val orientationProperties: String
        get() {
            // Create MAP options
            val load =
                CBJSON.jsonObject(
                    CBJSON.JKV("allowOrientationChange", allowOrientationChange),
                    CBJSON.JKV("forceOrientation", forceOrientationString(forceOrientation)),
                )
            return load.toString()
        }

    fun forceOrientationString(orientation: Int): String {
        return when (orientation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> "portrait"
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> "landscape"
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> "none"
            else -> "error"
        }
    }

    fun forceOrientationFromString(name: String): Int =
        when (name) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

    fun setOrientationProperties(
        allowOrientationChange: Boolean,
        forceOrientationString: String,
    ) {
        this.allowOrientationChange = allowOrientationChange
        this.forceOrientation = forceOrientationFromString(forceOrientationString)
        adUnitRendererCallback.applyActivityOrientation(forceOrientation, allowOrientationChange)
    }

    fun restoreOriginalOrientation() {
        adUnitRendererCallback.restoreOriginalOrientation()
        allowOrientationChange = true
        forceOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /*getMaxSize method
    The getMaxSize method returns the maximum size (in density-independent pixel width and
    height) an ad can expand or resize to.*/
    val maxSize: String
        get() =
            CBJSON.jsonObject(
                CBJSON.JKV("width", maxContainerWidth),
                CBJSON.JKV("height", maxContainerHeight),
            ).toString()

    /*getScreenSize method
    The getScreenSize method returns the current actual pixel width and height, based on the
    current orientation, in density-independent pixels, of the device on which the ad is running.
     */
    val screenSize: String
        get() =
            CBJSON.jsonObject(
                CBJSON.JKV("width", screenWidth),
                CBJSON.JKV("height", screenHeight),
            ).toString()

    /**
     * getDefaultPosition method
     * The getDefaultPosition method returns the position and size of the default ad view,
     * measured in density-independent pixels, regardless of what state the calling view is in.
     */
    val defaultPosition: String
        get() {
            calculatePosition()
            return getPosition(defaultXPos, defaultYPos, defaultWidth, defaultHeight)
        }

    /**
     * getCurrentPosition method
     * The getCurrentPosition method will return the current position and size of the ad view,
     * measured in density-independent pixels.
     */
    val currentPosition: String
        get() {
            calculatePosition()
            return getPosition(currentXPos, currentYPos, currentWidth, currentHeight)
        }

    private fun getPosition(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): String {
        return CBJSON.jsonObject(
            CBJSON.JKV("x", x),
            CBJSON.JKV("y", y),
            CBJSON.JKV("width", width),
            CBJSON.JKV("height", height),
        ).toString()
    }

    // This is exclusively called from VAST impression tracking
    fun sendRequest(url: String?) {
        if (url.isNullOrEmpty() || networkRequestService == null) {
            d("###### Sending VAST Tracking Event Failed: $url")
            return
        }
        networkRequestService.submit(
            impressionTrackerRequestFactory(url, eventTracker),
        )
        d("###### Sending VAST Tracking Event: $url")
    }

    fun sendQuartileEvent(
        videoDuration: Float,
        currentPosition: Float,
    ) {
        val firstQuartile = videoDuration / 4
        val midpoint = videoDuration / 2
        val thirdQuartile = videoDuration * 3 / 4
        when {
            currentPosition >= firstQuartile && currentPosition < midpoint -> {
                sendWebViewVastOmEvent(VastVideoEvent.QUARTILE1)
            }

            currentPosition >= midpoint && currentPosition < thirdQuartile -> {
                sendWebViewVastOmEvent(VastVideoEvent.MIDPOINT)
            }

            currentPosition >= thirdQuartile -> {
                sendWebViewVastOmEvent(VastVideoEvent.QUARTILE3)
            }
        }
    }

    fun sendWebViewVastOmEvent(event: VastVideoEvent) {
        d("sendWebViewVastOmEvent: " + event.name)
        // send all the events for the video precache and webview but this should not be sent
        // with static, playables and banner
        if (adUnitMType != MediaTypeOM.VIDEO) {
            return
        }
        with(openMeasurementImpressionCallback) {
            when (event) {
                VastVideoEvent.START ->
                    onImpressionNotifyVideoStarted(
                        videoDuration,
                        templateVideoVolume,
                    )

                VastVideoEvent.RESUME ->
                    if (state == PlayerState.PAUSED) {
                        onImpressionNotifyVideoResumed()
                    }

                VastVideoEvent.PAUSE -> onImpressionNotifyVideoPaused()
                VastVideoEvent.BUFFER_START -> onImpressionNotifyVideoBuffer(true)
                VastVideoEvent.BUFFER_END -> onImpressionNotifyVideoBuffer(false)
                VastVideoEvent.QUARTILE1 -> onImpressionNotifyVideoProgress(Quartile.FIRST)
                VastVideoEvent.MIDPOINT -> onImpressionNotifyVideoProgress(Quartile.MIDDLE)
                VastVideoEvent.QUARTILE3 -> onImpressionNotifyVideoProgress(Quartile.THIRD)
                VastVideoEvent.COMPLETED -> onImpressionNotifyVideoComplete()
                VastVideoEvent.SKIP -> onImpressionNotifyVideoSkipped()
                VastVideoEvent.VOLUME_CHANGE -> onImpressionNotifyVolumeChanged(templateVideoVolume)
            }
        }
    }

    fun updatePlayerState(newState: PlayerState) {
        state = newState
    }

    fun isPlayerPlaying(): Boolean {
        return state == PlayerState.PLAYING
    }

    fun unmute() {
        templateVideoVolume = 1f
    }

    fun mute() {
        templateVideoVolume = 0f
    }

    internal val customWebViewInterface: CustomWebViewInterface =
        object : CustomWebViewInterface {
            override fun onPageStarted() {
                val webView = view?.webView
                if (adUnitMType != MediaTypeOM.VIDEO && webView != null) {
                    openMeasurementImpressionCallback.onImpressionOnWebviewPageStarted(
                        adUnitMType,
                        webView,
                        Collections.emptyList(),
                    )
                }
            }

            override fun onWebViewInit() {
                adWebViewIntializeTime = System.currentTimeMillis()
            }

            override fun onError(message: String) {
                onWebViewError(message)
            }

            override fun onRegisterWebViewTimeout() {
                registerWebViewTimeout()
            }

            override fun onRegisterFriendlyWebViewObstruction(obstructionView: View) {
                openMeasurementImpressionCallback.onImpressionNotifyFriendlyObstructionCreated(
                    obstructionView,
                )
            }

            override fun onPageFinished() {
                onPageFinishedWebView()
            }
        }

    fun passVerificationResourcesFromTemplate(verificationScriptResourceList: List<VerificationScriptResource>) {
        view?.webView?.let { webview ->
            openMeasurementImpressionCallback.onImpressionOnWebviewPageStarted(
                adUnitMType,
                webview,
                verificationScriptResourceList,
            )
        }
    }
}

enum class VastVideoEvent {
    START,
    RESUME,
    PAUSE,
    BUFFER_START,
    BUFFER_END,
    QUARTILE1,
    MIDPOINT,
    QUARTILE3,
    COMPLETED,
    SKIP,
    VOLUME_CHANGE,
}

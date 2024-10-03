package com.chartboost.sdk.internal.WebView

import android.content.ActivityNotFoundException
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.internal.Model.asList
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.WebView.NativeCommand.Companion.fromStringOrNull
import com.chartboost.sdk.internal.clickthrough.UrlParser
import com.chartboost.sdk.internal.impression.ImpressionInterface
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.logging.Logger.d
import com.chartboost.sdk.internal.logging.Logger.e
import com.chartboost.sdk.legacy.PlayerState
import com.chartboost.sdk.legacy.VastVideoEvent
import com.iab.omid.library.chartboost.adsession.VerificationScriptResource
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

internal interface HideCustomViewCallback {
    fun onHideCustomView()
}

internal class NativeBridgeCommand(
    private val uiPost: UiPoster,
    private val urlParser: UrlParser,
) {
    var hideViewCallback: HideCustomViewCallback? = null
    private var videoDuration: Float = 0f
    private var impressionInterface: ImpressionInterface? = null

    fun setImpressionInterface(impressionInterface: ImpressionInterface) {
        this.impressionInterface = impressionInterface
    }

    fun clearImpressionInterface() {
        this.impressionInterface = null
    }

    fun nativeFunctionWithArgs(
        args: JSONObject?,
        functionName: String,
    ): String {
        val nativeCmd = fromStringOrNull(functionName)
        if (nativeCmd == null) {
            Logger.w("Native event unknown: $functionName")
            return "Function name not recognized."
        }
        d("TEMPLATE EVENT: " + nativeCmd.cmdName)
        return nativeFunctionWithArgs(args, nativeCmd)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
    private fun nativeFunctionWithArgs(
        args: JSONObject?,
        nativeCmd: NativeCommand,
    ): String {
        when (nativeCmd) {
            NativeCommand.GET_PARAMETERS -> {
                d("JavaScript to native ${nativeCmd.cmdName} callback triggered.")
                return impressionInterface?.getAdUnitParameters() ?: ""
            }

            NativeCommand.GET_MAX_SIZE -> {
                d("JavaScript to native ${nativeCmd.cmdName} callback triggered.")
                return impressionInterface?.getProtocolMaxSize() ?: ""
            }

            NativeCommand.GET_SCREEN_SIZE -> {
                d("JavaScript to native ${nativeCmd.cmdName} callback triggered.")
                return impressionInterface?.getProtocolScreenSize() ?: ""
            }

            NativeCommand.GET_CURRENT_POSITION -> {
                d("JavaScript to native ${nativeCmd.cmdName} callback triggered.")
                return impressionInterface?.getProtocolCurrentPosition() ?: ""
            }

            NativeCommand.GET_DEFAULT_POSITION -> {
                d("JavaScript to native ${nativeCmd.cmdName} callback triggered.")
                return impressionInterface?.getProtocolDefaultPosition() ?: ""
            }

            NativeCommand.GET_ORIENTATION_PROPERTIES -> {
                d("JavaScript to native ${nativeCmd.cmdName} callback triggered.")
                return impressionInterface?.getProtocolOrientationProperties() ?: ""
            }
            // TODO investigate if would be possible to remove the uiHandler,
            //  at this point I don;t think we need to run it in the ui thread
            NativeCommand.CLICK ->
                uiPost {
                    impressionInterface?.onTemplateClickEvent(
                        urlParser.parseOpenUrlArgsObject(args),
                    )
                }

            NativeCommand.CLOSE ->
                uiPost {
                    impressionInterface?.templateCloseEvent()
                        ?: d("Impression interface is missing in template close")
                }
            NativeCommand.SKIPPED ->
                uiPost {
                    impressionInterface?.sendWebViewVastOmEvent(
                        VastVideoEvent.SKIP,
                    )
                }

            NativeCommand.VIDEO_COMPLETED -> uiPost { videoCompleted() }
            NativeCommand.VIDEO_RESUMED -> uiPost { runVideoResumedCommand() }
            NativeCommand.VIDEO_PAUSED -> uiPost { runVideoPausedCommand() }
            NativeCommand.VIDEO_REPLAY -> uiPost { e("Video replay command is run") }
            NativeCommand.CURRENT_VIDEO_DURATION -> uiPost { currentVideoDuration(args) }
            NativeCommand.TOTAL_VIDEO_DURATION -> uiPost { totalVideoDuration(args) }
            NativeCommand.SHOW ->
                uiPost {
                    impressionInterface?.onShowImpression()
                        ?: d("Impression interface is missing in template show")
                }
            NativeCommand.ERROR -> uiPost { error(args) }
            NativeCommand.WARNING -> uiPost { warning(args) }
            NativeCommand.DEBUG -> uiPost { debug(args) }
            NativeCommand.TRACKING -> uiPost { vastImpressionTracking(args) }
            NativeCommand.OPEN_URL -> uiPost { openUrl(args) }
            NativeCommand.SET_ORIENTATION_PROPERTIES -> uiPost { setOrientation(args) }
            NativeCommand.REWARD -> uiPost { impressionInterface?.templateRewardEvent() }
            NativeCommand.REWARDED_VIDEO_COMPLETED ->
                uiPost {
                    impressionInterface?.onRewardedVideoCompleted()
                        ?: d("Impression interface is missing in template rewarded video completed")
                }
            NativeCommand.PLAY_VIDEO ->
                uiPost {
                    impressionInterface?.playVideo()
                        ?: d("Impression interface is missing in template play video")
                }
            NativeCommand.PAUSE_VIDEO ->
                uiPost {
                    impressionInterface?.pauseVideo()
                        ?: d("Impression interface is missing in template pause video")
                }
            NativeCommand.CLOSE_VIDEO ->
                uiPost {
                    impressionInterface?.closeVideo()
                        ?: d("Impression interface is missing in template close video")
                }
            NativeCommand.MUTE_VIDEO ->
                uiPost {
                    impressionInterface?.mute()
                        ?: d("Impression interface is missing in template mute video")
                }
            NativeCommand.UNMUTE_VIDEO ->
                uiPost {
                    impressionInterface?.unmute()
                        ?: d("Impression interface is missing in template unmute video")
                }
            NativeCommand.OM_MEASUREMENT_RESOURCES -> uiPost { runOmResources(args) }
            NativeCommand.START -> uiPost { runStart(args) }
            NativeCommand.BUFFER_START -> uiPost { runBufferStart() }
            NativeCommand.BUFFER_END -> uiPost { runBufferEnd() }
            NativeCommand.VIDEO_FINISHED -> uiPost { runVideoFinished() }
            NativeCommand.VIDEO_STARTED, NativeCommand.ON_FOREGROUND, NativeCommand.VIDEO_ENDED,
            NativeCommand.VIDEO_FAILED, NativeCommand.PLAYBACK_TIME, NativeCommand.ON_BACKGROUND,
            -> {
            }
        }
        return "Native function successfully called."
    }

    private fun videoCompleted() {
        hideViewCallback?.onHideCustomView()
        impressionInterface?.run {
            updatePlayerState(PlayerState.IDLE)
            templateVideoCompletedEvent()
        } ?: d("Impression interface is missing in videoCompleted")
    }

    private fun runVideoResumedCommand() {
        impressionInterface?.run {
            sendWebViewVastOmEvent(VastVideoEvent.RESUME)
            updatePlayerState(PlayerState.PLAYING)
        } ?: d("Impression interface is missing in runVideoResumedCommand")
    }

    private fun runVideoPausedCommand() {
        impressionInterface?.run {
            updatePlayerState(PlayerState.PAUSED)
            sendWebViewVastOmEvent(VastVideoEvent.PAUSE)
        } ?: d("Impression interface is missing in runVideoResumedCommand")
    }

    private fun currentVideoDuration(args: JSONObject?) {
        try {
            val duration = args?.getDouble("duration")?.toFloat() ?: 0f
            if (duration > 0) {
                val currentInSec = duration * 1000
                d("######### JS->Native Video current player duration: $currentInSec")
                impressionInterface?.run {
                    setVideoPosition(currentInSec)
                    sendQuartileEvent(videoDuration, currentInSec)
                } ?: d("Impression interface is missing in currentVideoDuration")
            }
        } catch (e: Exception) {
            warning(
                JSONObject().put(
                    "message",
                    "Parsing exception unknown field for current player duration: $e",
                ),
            )
        }
    }

    private fun totalVideoDuration(args: JSONObject?) {
        try {
            val duration = args?.optDouble("duration", 0.0)?.toFloat() ?: 0f
            d("######### JS->Native Video total player duration" + duration * 1000)
            videoDuration = duration * 1000
            impressionInterface?.setVideoDuration(videoDuration)
                ?: d("Impression interface is missing in totalVideoDuration")
        } catch (e: Exception) {
            warning(
                JSONObject().put(
                    "message",
                    "Parsing exception unknown field for total player duration: $e",
                ),
            )
        }
    }

    private fun error(args: JSONObject?) {
        d("Javascript Error occurred $args")
        forceCrashSDKInSandbox(args)

        try {
            impressionInterface?.run {
                impressionNotifyDidCompleteRewardedOnError()
                onWebViewError(args.parseMessage("JS->Native Error message: "))
            } ?: d("Impression interface is missing in error")
        } catch (e: Exception) {
            e("Error message is empty")
            impressionInterface?.onWebViewError("")
        }
    }

    private fun forceCrashSDKInSandbox(args: JSONObject?) {
        if (SandboxBridgeSettings.isSandboxMode) {
            args?.optString("msg")?.let {
                if (it == "crash sdk") {
                    throw RuntimeException("test crash")
                }
            }
        }
    }

    private fun warning(args: JSONObject?) {
        d("Javascript warning occurred")
        try {
            val message = args?.getString("message") ?: "Missing message argument"
            d("JS->Native Warning message: $message")
            impressionInterface?.warning(message)
        } catch (e: Exception) {
            impressionInterface?.warning("Warning message is empty")
        }
    }

    private fun debug(args: JSONObject?) {
        try {
            val msg = args.parseMessage("JS->Native Debug message: ")
            d("Debug message: $msg")
        } catch (e: Exception) {
            e("Exception occurred while parsing the message for webview debug track event", e)
        }
    }

    // Currently we only receive VAST impression tracking events here
    private fun vastImpressionTracking(args: JSONObject?) {
        try {
            args?.getString("event")?.let {
                impressionInterface?.sendWebViewVASTTrackingEvents(it)
                    ?: d("JS->Native Track VAST event message: $it")
            } ?: e("Tracking command received but event is missing!")
        } catch (e: Exception) {
            e("Exception while parsing webview VAST tracking", e)
        }
    }

    private fun openUrl(args: JSONObject?) {
        try {
            impressionInterface?.onTemplateOpenURLEvent(
                urlParser.parseOpenUrlArgsObject(args),
            ) ?: d("Impression interface is missing in openUrl")
        } catch (e: ActivityNotFoundException) {
            e("ActivityNotFoundException occured when opening a url in a browser", e)
        } catch (e: Exception) {
            e("Exception while opening a browser view with MRAID url", e)
        }
    }

    private fun setOrientation(args: JSONObject?) {
        try {
            val allowOrientationChange = args?.optBoolean("allowOrientationChange", true) ?: true
            val forceOrientation = args?.optString("forceOrientation", "none") ?: "none"
            impressionInterface?.setOrientationProperties(allowOrientationChange, forceOrientation)
                ?: d("Impression interface is missing in setOrientation")
        } catch (e: Exception) {
            e("Invalid set orientation command")
        }
    }

    private fun runOmResources(args: JSONObject?) {
        try {
            args?.getString(RESOURCES_ARG)?.let { json ->
                if (json.isEmpty()) {
                    emptyList()
                } else {
                    JSONArray(json).asList<JSONObject>().map { resource ->
                        VerificationScriptResource.createVerificationScriptResourceWithParameters(
                            resource.getString("vendorKey"),
                            URL(resource.getString("url")),
                            resource.getString("params"),
                        )
                    }.toList()
                }.let {
                    impressionInterface?.passVerificationResourcesFromTemplate(it)
                        ?: d("Impression interface is missing in runOmResources")
                }
            } ?: e("Invalid om resources command: missing json")
        } catch (e: Exception) {
            e("Invalid om resources command", e)
        }
    }

    private fun runStart(args: JSONObject?) {
        try {
            val duration = args?.optDouble("duration", 0.0) ?: 0.0
            videoDuration = duration.toFloat()
            impressionInterface?.sendWebViewVastOmEvent(VastVideoEvent.START)
                ?: d("Impression interface is missing in runStart")
        } catch (e: Exception) {
            e("Invalid start command", e)
        }
    }

    private fun runBufferStart() {
        try {
            impressionInterface?.sendWebViewVastOmEvent(VastVideoEvent.BUFFER_START)
                ?: d("Impression interface is missing in runBufferStart")
        } catch (e: Exception) {
            e("Invalid bufer start command", e)
        }
    }

    private fun runBufferEnd() {
        try {
            impressionInterface?.sendWebViewVastOmEvent(VastVideoEvent.BUFFER_END)
                ?: d("Impression interface is missing in runBufferEnd")
        } catch (e: Exception) {
            e("Invalid buffer end command", e)
        }
    }

    private fun runVideoFinished() {
        try {
            impressionInterface?.sendWebViewVastOmEvent(VastVideoEvent.COMPLETED)
                ?: d("Impression interface is missing in runVideoFinished")
        } catch (e: Exception) {
            e("Invalid buffer end command", e)
        }
    }

    private fun JSONObject?.parseMessage(logMsg: String): String {
        val message = this?.getString("message") ?: ""
        d(logMsg + message)
        return message
    }
}

enum class NativeCommand(val cmdName: String) {
    // Events that return value back to the template
    GET_PARAMETERS("getParameters"),
    GET_MAX_SIZE("getMaxSize"),
    GET_SCREEN_SIZE("getScreenSize"),
    GET_CURRENT_POSITION("getCurrentPosition"),
    GET_DEFAULT_POSITION("getDefaultPosition"),
    GET_ORIENTATION_PROPERTIES("getOrientationProperties"),

    // Events that sdk processes natively
    CLICK("click"),
    CLOSE("close"),
    SKIPPED("skipped"),
    VIDEO_COMPLETED("videoCompleted"),
    VIDEO_RESUMED("videoResumed"),
    VIDEO_PAUSED("videoPaused"),
    VIDEO_REPLAY("videoReplay"),
    CURRENT_VIDEO_DURATION("currentVideoDuration"),
    TOTAL_VIDEO_DURATION("totalVideoDuration"),
    SHOW("show"),
    ERROR("error"),
    WARNING("warning"),
    DEBUG("debug"),
    TRACKING("tracking"),
    OPEN_URL("openUrl"),
    SET_ORIENTATION_PROPERTIES("setOrientationProperties"),
    REWARD("reward"),
    REWARDED_VIDEO_COMPLETED("rewardedVideoCompleted"),
    PLAY_VIDEO("playVideo"),
    PAUSE_VIDEO("pauseVideo"),
    CLOSE_VIDEO("closeVideo"),
    MUTE_VIDEO("mute"),
    UNMUTE_VIDEO("unmute"),
    OM_MEASUREMENT_RESOURCES("OMMeasurementResources"),
    START("start"),
    BUFFER_START("bufferStart"),
    BUFFER_END("bufferEnd"),
    VIDEO_FINISHED("videoFinished"),

    // Events sent to the template from the sdk
    VIDEO_STARTED("videoStarted"),
    VIDEO_ENDED("videoEnded"),
    VIDEO_FAILED("videoFailed"),
    PLAYBACK_TIME("playbackTime"),
    ON_BACKGROUND("onBackground"),
    ON_FOREGROUND("onForeground"),
    ;

    // TODO Remove this after CBWebChromeClient is refactored to kotlin
    companion object {
        private val map = entries.associateBy(NativeCommand::cmdName)

        @JvmStatic
        fun fromStringOrNull(cmdName: String): NativeCommand? = map[cmdName]
    }
}

private const val RESOURCES_ARG = "resources"

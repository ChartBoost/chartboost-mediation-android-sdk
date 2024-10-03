package com.chartboost.sdk.internal.WebView

import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import com.chartboost.sdk.internal.WebView.WebViewCorsErrorHandler.CorsErrorCallback
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.logging.Logger.e
import org.json.JSONException
import org.json.JSONObject

/**
 * This class serves as a WebChromeClient to be set to a WebView, allowing it to play video.
 * Video will play differently depending on target API level (in-line, fullscreen, or both).
 *
 *
 * It has been tested with the following video classes:
 * - android.widget.VideoView (typically API level <11)
 * - android.webkit.HTML5VideoFullScreen$VideoSurfaceView/VideoTextureView (typically API level 11-18)
 * - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView (typically API level 19+)
 *
 *
 * Important notes:
 * - For API level 11+, android:hardwareAccelerated="true" must be set in the application manifest.
 * - The invoking activity must call CBVideoEnabledWebChromeClient's onBackPressed() inside of its own onBackPressed().
 * - Tested in Android API levels 8-19. Only tested on http://m.youtube.com.
 */
internal class CBRichWebChromeClient(
    private val activityNonVideoView: View,
    private val cmd: NativeBridgeCommand,
    private val corsHandler: WebViewCorsErrorHandler?,
) : WebChromeClient(), CorsErrorCallback, HideCustomViewCallback {
    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     *
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    var isVideoFullscreen = false
        private set
    private var videoViewCallback: CustomViewCallback? = null

    init {
        cmd.hideViewCallback = this
    }

    override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
        val consoleMsg = cm.message()
        Logger.d(
            "Chartboost Rich Webview: $consoleMsg -- From line ${cm.lineNumber()} of ${cm.sourceId()}",
        )
        handleCorsError(consoleMsg)
        return true
    }

    private fun handleCorsError(consoleMsg: String) {
        corsHandler?.handleCorsError(consoleMsg, this)
    }

    /**
     * Tell the client to display a prompt dialog to the user.
     * If the client returns true, WebView will assume that the client will
     * handle the prompt dialog and call the appropriate JsPromptResult method.
     */
    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?,
    ): Boolean {
        message ?: return true
        val functionName: String
        val args: JSONObject
        try {
            val jsonObj = JSONObject(message)
            functionName = jsonObj.getString("eventType")
            args = jsonObj.getJSONObject("eventArgs")
        } catch (e: JSONException) {
            e("Exception caught parsing the function name from js to native")
            return true
        }
        val nativeResult = cmd.nativeFunctionWithArgs(args, functionName)
        result?.confirm(nativeResult)
        return true
    }

    override fun onShowCustomView(
        view: View?,
        callback: CustomViewCallback?,
    ) {
        if (view is FrameLayout) {
            // A video wants to be shown

            // Save video related variables
            isVideoFullscreen = true
            videoViewCallback = callback

            // Hide the non-video view, add the video view, and show it
            activityNonVideoView.visibility = View.INVISIBLE
        }
    }

    override fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        callback: CustomViewCallback?,
    ) { // Available in API level 14+, deprecated in API level 18+
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        // This method should be manually called on video end in all cases because it's not always called automatically.
        // This method must be manually called on back key press (from this class' onBackPressed() method).
        if (isVideoFullscreen) {
            // Hide the video view, remove it, and show the non-video view
            activityNonVideoView.visibility = View.VISIBLE

            // Call back (only in API level <19, because in API level 19+ with chromium webview it crashes)
            if (videoViewCallback?.javaClass?.name?.contains(".chromium.") == false) {
                videoViewCallback?.onCustomViewHidden()
            }

            // Reset video related variables
            isVideoFullscreen = false
            videoViewCallback = null
        }
    }

    override fun notifyCorsError(data: JSONObject?) {
        cmd.nativeFunctionWithArgs(data, NativeCommand.ERROR.cmdName)
    }

    /**
     * Notifies the class that the back key has been pressed by the user.
     * This must be called from the Activity's onBackPressed(), and if it returns false,
     * the activity itself should handle it. Otherwise don't do anything.
     *
     * @return Returns true if the event was handled, and false if was not (video view is not visible)
     */
    fun onBackPressed(): Boolean {
        return if (isVideoFullscreen) {
            onHideCustomView()
            true
        } else {
            false
        }
    }
}

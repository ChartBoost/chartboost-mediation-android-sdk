package com.chartboost.sdk.internal.WebView

import android.content.Context
import android.webkit.WebSettings
import com.chartboost.sdk.internal.di.getEventTracker
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.ErrorEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName

object UserAgentHelper : EventTrackerExtensions by getEventTracker() {
    var webViewUserAgentValue = "Invalid user-agent value"

    /*
    This function sets the SDK wide SdkSettings.webViewUserAgent which is sent in all the requests.
        if (device is not ancient) {
            use the static getter in WebSettings
        }
        else if (no WebView passed){
            if(persistent storage set){
                set the last used user-agent from persistent storage
            }
            else{
                create a new WebView (slow call!)
            }
        }
        else{
            set the user-agent from the web view
        }
        update the persistent storage.

        Note: Some custom ROMs may fail to get a user agent.
     */
    fun setUserAgent(context: Context) {
        var webViewUserAgent: String? = ""
        try {
            webViewUserAgent = System.getProperty("http.agent")
        } catch (e: Exception) {
            // Some custom ROMs may fail to get a user agent.
            sendUserAgentErrorTracking(e.toString())
        }

        try {
            webViewUserAgent = WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            // Some custom ROMs may fail to get a user agent.
            sendUserAgentErrorTracking(e.toString())
        }

        if (webViewUserAgent != null) {
            webViewUserAgentValue = webViewUserAgent
        }
    }

    private fun sendUserAgentErrorTracking(errorMsg: String) {
        try {
            ErrorEvent(
                TrackingEventName.Misc.USER_AGENT_UPDATE_ERROR,
                errorMsg,
            ).track()
        } catch (e: Exception) {
            Logger.e("sendUserAgentErrorTracking", e)
            // User agent setting can be done before SDK init but tracking works only with SDK initialised.
        }
    }
}

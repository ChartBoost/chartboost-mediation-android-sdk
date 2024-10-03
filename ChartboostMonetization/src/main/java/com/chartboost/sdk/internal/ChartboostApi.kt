package com.chartboost.sdk.internal

import android.content.Context
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.internal.Libraries.BidderTokenGenerator
import com.chartboost.sdk.internal.WebView.UserAgentHelper
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.initialization.SdkInitializer
import com.chartboost.sdk.internal.logging.Logger
import java.util.concurrent.ScheduledExecutorService

internal class ChartboostApi(
    private val context: Context,
    private val backgroundExecutor: ScheduledExecutorService,
    private val sdkInitializer: SdkInitializer,
    private val tokenGenerator: BidderTokenGenerator,
    private val identity: CBIdentity,
) {
    fun startWithAppId(
        appId: String,
        appSignature: String,
        onStarted: StartCallback,
    ) {
        backgroundExecutor.execute {
            startIdentity()
            UserAgentHelper.setUserAgent(context)
            sdkInitializer.initSdk(appId, appSignature, onStarted)
        }
    }

    fun getBidderToken(): String {
        return tokenGenerator.generateBidderToken()
    }

    /**
     * Launch identity coroutine as soon as possible during init and wait 100ms to return
     */
    private fun startIdentity() {
        try {
            // wait for identity coroutine to pass
            Thread.sleep(100)
            // This might feel weird but it is required to refresh the identity state
            identity.toIdentityBodyFields()
        } catch (e: Exception) {
            Logger.d("startIdentity error $e")
        }
    }
}

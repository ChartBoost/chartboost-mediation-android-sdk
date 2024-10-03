package com.chartboost.sdk.internal.initialization

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.BuildConfig
import com.chartboost.sdk.SandboxBridgeSettings
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.events.StartError
import com.chartboost.sdk.internal.AssetLoader.Downloader
import com.chartboost.sdk.internal.AssetLoader.Prefetcher
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.UiPoster
import com.chartboost.sdk.internal.WebView.UserAgentHelper.refresh
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.internal.measurement.OpenMeasurementManager
import com.chartboost.sdk.internal.video.repository.VideoCachePolicy
import com.chartboost.sdk.internal.video.repository.VideoRepository
import com.chartboost.sdk.legacy.CBConfig
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.privacy.model.COPPA.Companion.COPPA_STANDARD
import com.chartboost.sdk.tracking.Session
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

private const val VALID_APP_ID_LENGTH = 24
private const val VALID_SIGNATURE_LENGTH = 40

private val VALID_CHARS_REGEX = "[a-f0-9]+".toRegex()

// TODO Big constructor, probably wants to be refactored
internal class SdkInitializer(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val uiPoster: UiPoster,
    private val privacyApi: PrivacyApi,
    private val sdkConfig: AtomicReference<SdkConfiguration>,
    private val prefetcher: Prefetcher,
    private val downloader: Downloader,
    private val session: Session,
    private val videoCachePolicy: VideoCachePolicy,
    private val videoRepository: Lazy<VideoRepository>,
    private val initInstallRequest: InitInstallRequest,
    private val initConfigRequest: InitConfigRequest,
    private val reachability: CBReachability,
    private val providerInstallerHelper: ProviderInstallerHelper,
    private val identity: CBIdentity,
    private val openMeasurementManager: OpenMeasurementManager,
) : ConfigRequestCallback {
    @VisibleForTesting
    var isSDKInitialized = false

    @VisibleForTesting
    var isFirstSession = true

    private val startCallbackList = ConcurrentLinkedQueue<AtomicReference<StartCallback>>()
    private var isInitializing = false

    fun isSdkInitialized(): Boolean = isSDKInitialized

    @Synchronized
    fun initSdk(
        appId: String,
        appSignature: String,
        onStarted: StartCallback,
    ) {
        try {
            ExceptionHandler.setCustomExceptionHandler()
            startCallbackList.add(AtomicReference(onStarted))
            if (isInitializing) {
                Logger.e("Initialization already in progress")
                return
            }

            // Avoid considering new app launch as first session
            if (session.sessionCounter > 1) {
                isFirstSession = false
            }

            isInitializing = true

            // move session counter earlier as session id needs to be ready early for the token
            updateSessionCounter()

            if (isSDKInitialized) {
                onSdkInitialised()
            } else {
                onSdkNotInitialised(appId, appSignature)
            }

            checkForCoppa()
        } catch (e: Exception) {
            Logger.e("Cannot initialize Chartboost sdk due to internal error", e)
            callOnStartCompleted(StartError(StartError.Code.INTERNAL, e))
        }
    }

    private fun onSdkNotInitialised(
        appId: String,
        appSignature: String,
    ) {
        if (!CBConfig.validatePermissions(context)) {
            Logger.e("Permissions not set correctly")
            callOnStartCompleted(
                StartError(
                    StartError.Code.INVALID_CREDENTIALS,
                    Exception("Permissions not set correctly"),
                ),
            )
            return
        }

        if (appId.isEmpty() ||
            appSignature.isEmpty() ||
            appId.length != VALID_APP_ID_LENGTH ||
            appSignature.length != VALID_SIGNATURE_LENGTH ||
            !VALID_CHARS_REGEX.matches(appId) ||
            !VALID_CHARS_REGEX.matches(appSignature)
        ) {
            Logger.e("AppId or AppSignature is invalid. Please pass a valid id's")
            callOnStartCompleted(
                StartError(
                    StartError.Code.INVALID_CREDENTIALS,
                    Exception("AppId or AppSignature is invalid. Please pass a valid id's"),
                ),
            )
            return
        }

        providerInstallerHelper.installProviderIfPossible()
        downloader.reduceCacheSize()

        /**
         *  Get the config and update the list in shared preferences
         *  If config is already persisted, do the config update async
         */
        if (isConfigAlreadyPersisted()) {
            onSdkInitialised()
        } else {
            requestConfig()
        }
    }

    private fun onSdkInitialised() {
        callOnStartCompleted(null)
        isSDKInitialized = true
        requestConfig()
    }

    private fun checkForCoppa() {
        if (privacyApi.getPrivacyStandard(COPPA_STANDARD) == null && !isSDKInitialized) {
            Logger.w(
                "COPPA is not set. If this app is child directed, please use" +
                    " ´addDataUseConsent(android.content.Context, com.chartboost.sdk.Privacy.model.COPPA)´" +
                    " to set the correct value.",
            )
        }
    }

    private fun requestConfig() {
        initConfigRequest.execute(this)
    }

    private fun saveConfig(config: JSONObject?) {
        config?.let {
            if (CBConfig.updateConfig(sdkConfig, it)) {
                sharedPreferences
                    .edit()
                    .putString(CBConstants.CONFIG_KEY, it.toString())
                    .apply()
            }
        }
    }

    private fun isConfigAlreadyPersisted(): Boolean {
        return persistedConfig()?.isNotEmpty() ?: false
    }

    private fun persistedConfig(): String? = sharedPreferences.getString(CBConstants.CONFIG_KEY, "")

    private fun initialiseAfterConfigCall() {
        // init OM as soon possible but after config call
        openMeasurementManager.initialize()
        updateTrackingConfig()
        updateVideoCachePolicy()
        requestInstallOnConfigCallFinished()
        setSDKInitializedAfterConfigCall()
        isFirstSession = false
    }

    private fun updateSessionCounter() {
        if (session.sessionId == null) {
            session.addSession()
            Logger.e("Current session count: " + session.sessionCounter)
        }
    }

    /**
     * Set isSDKInitialized regardless of the outcome of the config call. Config call is not
     * mandatory for the function of the SDK cause we will fall into default settings.
     * Config will be updated at the next user entry to the application (this will depend on the
     * publisher implementation cause it may happen on application onCreate, on Activity onCreate,
     * on Activity onResume - depends where they will try to init SDK). SDK will call config call
     * each time publisher will try to init the SDK.
     */
    private fun setSDKInitializedAfterConfigCall() {
        if (!isSDKInitialized) {
            callOnStartCompleted(null)
            isSDKInitialized = true
        }
    }

    private fun updateTrackingConfig() {
        sdkConfig.get().trackingConfig?.refresh()
    }

    private fun updateVideoCachePolicy() {
        sdkConfig.get().precacheConfig?.run {
            videoCachePolicy.maxBytes = maxBytes
            videoCachePolicy.maxUnitsPerTimeWindow = maxUnitsPerTimeWindow
            videoCachePolicy.maxUnitsPerTimeWindowCellular = maxUnitsPerTimeWindowCellular
            videoCachePolicy.timeWindow = timeWindow
            videoCachePolicy.timeWindowCellular = maxUnitsPerTimeWindowCellular.toLong()
            videoCachePolicy.ttl = ttl
            videoCachePolicy.bufferSize = bufferSize
        }
        videoRepository.value.initialize(context)
    }

    private fun requestInstallOnConfigCallFinished() {
        logPrivacyWarningMessage()
        sdkConfig.get()?.let {
            privacyApi.setPrivacyConfig(it.privacyStandardsConfig)
        }
        initInstallRequest.execute()
        sendPrefetchRequestWithDelay()
    }

    private fun logPrivacyWarningMessage() {
        if (sdkConfig.get() != null && sdkConfig.get().publisherWarning != null) {
            Logger.w(sdkConfig.get().publisherWarning)
        }
    }

    /**
     * Added only for debug to handle espresso test and sync network calls
     */
    private fun sendPrefetchRequestWithDelay() {
        if (BuildConfig.DEBUG) {
            try {
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        prefetcher.prefetch()
    }

    private fun callOnStartCompleted(error: StartError?) {
        callOnStartCompletedSandbox()
        // TODO update this to coroutine
        while (true) {
            val callback = startCallbackList.poll()?.get() ?: break
            uiPoster {
                callback.onStartCompleted(error)
            }
        }

        isInitializing = false
    }

    private fun callOnStartCompletedSandbox() {
        if (SandboxBridgeSettings.isSandboxMode) {
            identity.toIdentityBodyFields().let {
                SandboxBridgeSettings.sendLogsToSandbox(
                    "SetId: ${it.setId}" +
                        " scope:${it.setIdScope}" +
                        " Tracking state: ${it.trackingState}" +
                        " Identifiers: ${it.identifiers}",
                )
            }
        }
    }

    override fun onConfigRequestSuccess(configJson: JSONObject) {
        saveConfig(configJson)
        initialiseAfterConfigCall()
        sandboxLogVideoPlayer(configJson)
    }

    override fun onConfigRequestFailure(errorMsg: String) {
        // To match iOS behaviour we return error if config failed on the first session MO-5079
        if (isFirstSession) {
            val startError =
                if (reachability.isNetworkAvailable) {
                    StartError(StartError.Code.SERVER_ERROR, Exception(errorMsg))
                } else {
                    StartError(StartError.Code.NETWORK_FAILURE, Exception(errorMsg))
                }
            callOnStartCompleted(startError)
        } else {
            initialiseAfterConfigCall()
        }
        sandboxLogVideoPlayer()
    }

    private fun sandboxLogVideoPlayer() {
        if (SandboxBridgeSettings.isSandboxMode) {
            sandboxLogVideoPlayer(
                JSONObject(
                    (persistedConfig() ?: "{}").ifEmpty { "{}" },
                ),
            )
        }
    }

    private fun sandboxLogVideoPlayer(configJson: JSONObject) {
        if (SandboxBridgeSettings.isSandboxMode) {
            val videoPlayer = SdkConfiguration(configJson).precacheConfig.videoPlayer
            SandboxBridgeSettings.sendLogsToSandbox("Video player: $videoPlayer")
        }
    }
}

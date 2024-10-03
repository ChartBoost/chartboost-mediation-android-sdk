package com.chartboost.sdk

import android.content.Context
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.events.StartError
import com.chartboost.sdk.internal.Libraries.CBConstants
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.initialization.InitializationPreconditions
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.privacy.model.DataUseConsent
import com.chartboost.sdk.tracking.Environment

/**
 * SDK class that provides ads from the Chartboost ad network.
 * Ensure to start Chartboost before requesting any ad.
 * Setting data use consent information beforehand is highly recommended,
 * otherwise Chartboost's ability to provide ads might be hindered.
 */
object Chartboost {
    /**
     * Initializes Chartboost with the required appId, appSignature, and callback. This method must be called
     * before using cache and show methods.
     *
     * @param context Application context.
     * @param appId The Chartboost application ID for this application.
     * @param appSignature The Chartboost application signature for this application.
     * @param onStarted Callback to be invoked when Chartboost has started.
     */
    @JvmStatic
    @Synchronized
    fun startWithAppId(
        context: Context,
        appId: String,
        appSignature: String,
        onStarted: StartCallback,
    ) {
        /**
         * Not an error. Do not pass back an error in this case since publishers might act on it.
         *
         * The in-progress check is handled in [com.chartboost.sdk.internal.initialization.SdkInitializer].
         */
        if (isSdkStarted()) {
            Logger.i(
                "Chartboost startWithAppId skipped due to SDK already " +
                    "being initialized. This method only needs to be called once per app session.",
            )
            onStarted.onStartCompleted(null)
            return
        }

        val preconditions = InitializationPreconditions(context)

        if (!preconditions.satisfied() && !isSdkStarted()) {
            Logger.e(
                "Chartboost startWithAppId failed due to preconditions " +
                    "not being met. Check the logs for more information.",
            )
            onStarted.onStartCompleted(StartError(StartError.Code.INTERNAL, Exception("Initialization preconditions not met")))
            return
        }

        preconditions.cleanup()

        initContainer(context)
        Environment.ContextProvider.context = context
        if (ChartboostDependencyContainer.initialized) {
            if (!isSdkStarted()) {
                // Update credentials only during the first session in the init
                ChartboostDependencyContainer.start(appId, appSignature)
            }
            ChartboostDependencyContainer.trackerComponent.eventTracker
            ChartboostDependencyContainer.sdkComponent.chartboostApi.startWithAppId(
                appId,
                appSignature,
                onStarted,
            )
        } else {
            Logger.e("Chartboost startWithAppId failed due to DI not being initialized.")
            onStarted.onStartCompleted(StartError(StartError.Code.INTERNAL, Exception("DI not initialized")))
        }
    }

    /**
     * Restricts Chartboost's ability to collect personal data from the user.
     *
     * This method can be called multiple times to set consent for different privacy standards.
     * If consent has already been set for a privacy standard, adding a new consent object will overwrite the previous value.
     *
     * It is recommended to call this method before starting the Chartboost SDK with startWithAppId.
     * The added consents are persisted, so you only need to call this when the consent status needs to be updated.
     *
     * For valid values, refer to:
     * [Chartboost Android Privacy Methods](https://answers.chartboost.com/en-us/child_article/android-privacy-methods)
     *
     * @param context Application context.
     * @param dataUseConsent A data use consent object such as GDPR.
     */
    @JvmStatic
    fun addDataUseConsent(
        context: Context,
        dataUseConsent: DataUseConsent,
    ) {
        initContainer(context)
        if (ChartboostDependencyContainer.initialized) {
            ChartboostDependencyContainer.privacyComponent.privacyApi.putPrivacyStandard(
                dataUseConsent,
            )
        }
    }

    /**
     * Retrieves stored data use consent. This method returns only the base class of the DataUseConsent,
     * even if it was added as a specific class like GDPR or CCPA. Might return null if DataUseConsent
     * is not found or was never stored.
     *
     * @param context Application context.
     * @param privacyStandard The privacy standard such as GDPR.GDPR_STANDARD or CCPA.CCPA_STANDARD or custom string.
     * @return The stored DataUseConsent object, or null if not found.
     */
    @JvmStatic
    fun getDataUseConsent(
        context: Context,
        privacyStandard: String,
    ): DataUseConsent? {
        initContainer(context)
        if (ChartboostDependencyContainer.initialized) {
            return ChartboostDependencyContainer.privacyComponent.privacyApi.getPrivacyStandard(
                privacyStandard,
            )
        }
        return null
    }

    /**
     * Clears the previously added consent for the specified privacy standard.
     *
     * Chartboost persists the added consents, so you'll need to call this method
     * if you want to delete a previously added consent.
     *
     * If no consent was available for the indicated standard, nothing will happen.
     *
     * @param context Application context.
     * @param privacyStandard The privacy standard for which you want to clear the consent.
     */
    @JvmStatic
    fun clearDataUseConsent(
        context: Context,
        privacyStandard: String,
    ) {
        initContainer(context)
        if (ChartboostDependencyContainer.initialized) {
            ChartboostDependencyContainer.privacyComponent.privacyApi.removePrivacyStandard(
                privacyStandard,
            )
        }
    }

    /**
     * Checks whether the SDK was started successfully.
     *
     * @return true if the SDK was started successfully, false otherwise.
     */
    @JvmStatic
    fun isSdkStarted(): Boolean {
        var isSDKInitialized = false
        // Only access the component when the container is initialized; otherwise, it means the SDK was not initialized.
        if (ChartboostDependencyContainer.initialized && ChartboostDependencyContainer.started) {
            try {
                isSDKInitialized =
                    ChartboostDependencyContainer.sdkComponent.sdkInitializer.isSdkInitialized()
            } catch (e: Exception) {
                // This could provoke a crash if the SDK was not initialized properly.
            }
        }
        return isSDKInitialized
    }

    /**
     * Sets the Chartboost SDK logging level.
     *
     * @param level The logging level.
     */
    @JvmStatic
    fun setLoggingLevel(level: LoggingLevel) {
        Logger.level = level
    }

    /**
     * Retrieves the current version of the Chartboost SDK.
     *
     * @return The current SDK version.
     */
    @JvmStatic
    fun getSDKVersion(): String {
        return CBConstants.SDK_VERSION
    }

    /**
     * Gets the bidder token for the Chartboost SDK.
     *
     * @return The bidder token, or null if the SDK is not initialized.
     */
    @JvmStatic
    fun getBidderToken(): String? {
        return if (isSdkStarted()) {
            ChartboostDependencyContainer.sdkComponent.chartboostApi.getBidderToken()
        } else {
            Logger.e("Chartboost getBidderToken failed due to SDK not being initialized.")
            null
        }
    }

    private fun initContainer(context: Context) {
        if (!ChartboostDependencyContainer.initialized) {
            ChartboostDependencyContainer.initialize(context)
        }
    }
}

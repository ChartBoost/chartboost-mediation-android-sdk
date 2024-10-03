/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.core.consent.googleusermessagingplatform

import android.app.Activity
import android.content.Context
import com.chartboost.core.ChartboostCoreLogger
import com.chartboost.core.Utils
import com.chartboost.core.consent.ConsentAdapter
import com.chartboost.core.consent.ConsentAdapterListener
import com.chartboost.core.consent.ConsentDialogType
import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentSource
import com.chartboost.core.consent.ConsentValue
import com.chartboost.core.error.ChartboostCoreError
import com.chartboost.core.error.ChartboostCoreException
import com.chartboost.core.initialization.Module
import com.chartboost.core.initialization.ModuleConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

class GoogleUserMessagingPlatformAdapter() : ConsentAdapter, Module {

    companion object {
        const val GEOGRAPHY_KEY: String = "geography"
        const val TEST_DEVICE_IDENTIFIERS_KEY: String = "testDeviceIdentifiers"
        const val moduleId = "google_user_messaging_platform"
        const val moduleVersion =
            BuildConfig.CHARTBOOST_CORE_GOOGLE_USER_MESSAGING_PLATFORM_ADAPTER_VERSION

        var consentDebugSettings: ConsentDebugSettings? = null
    }

    constructor(
        consentDebugSettings: ConsentDebugSettings,
    ) : this() {
        GoogleUserMessagingPlatformAdapter.consentDebugSettings = consentDebugSettings
    }

    override fun updateCredentials(context: Context, credentials: JSONObject) {
        val geographyInt = credentials.optInt(GEOGRAPHY_KEY)
        val consentDebugSettingsBuilder = ConsentDebugSettings.Builder(context)
        if (geographyInt != 0) {
            consentDebugSettingsBuilder.setDebugGeography(geographyInt)
        }
        credentials.optJSONArray(TEST_DEVICE_IDENTIFIERS_KEY)?.let { jsonArray ->
            (0 until jsonArray.length()).forEach {
                consentDebugSettingsBuilder.addTestDeviceHashedId(jsonArray.optString(it))
            }
        }
        consentDebugSettings = consentDebugSettingsBuilder.build()
    }

    override val moduleId: String = Companion.moduleId

    override val moduleVersion: String = Companion.moduleVersion

    override var shouldCollectConsent: Boolean = true
        private set

    override val consents: Map<ConsentKey, ConsentValue>
        get() = mutableConsents

    private val mutableConsents = mutableMapOf<ConsentKey, ConsentValue>()

    override val sharedPreferencesIabStrings: MutableMap<String, String> = mutableMapOf()

    override val sharedPreferenceChangeListener: ConsentAdapter.IabSharedPreferencesListener =
        ConsentAdapter.IabSharedPreferencesListener(sharedPreferencesIabStrings)

    override var listener: ConsentAdapterListener? = null
        set(value) {
            field = value
            sharedPreferenceChangeListener.listener = value
        }

    override suspend fun showConsentDialog(
        activity: Activity, dialogType: ConsentDialogType
    ): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            when (dialogType) {
                ConsentDialogType.CONCISE -> {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                        loadAndShowError?.let {
                            continuation.resume(
                                Result.failure(
                                    ChartboostCoreException(
                                        ChartboostCoreError.ConsentError.DialogShowError
                                    )
                                )
                            )
                        } ?: run {
                            shouldCollectConsent = false
                            mutableConsents.putAll(sharedPreferencesIabStrings)
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }

                ConsentDialogType.DETAILED -> {
                    UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                        formError?.let {
                            continuation.resume(
                                Result.failure(
                                    ChartboostCoreException(
                                        ChartboostCoreError.ConsentError.DialogShowError
                                    )
                                )
                            )
                        } ?: run {
                            shouldCollectConsent = false
                            mutableConsents.putAll(sharedPreferencesIabStrings)
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }

                else -> {
                    continuation.resume(Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.DialogShowError)))
                }
            }
        }
    }

    override suspend fun grantConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.ActionNotAllowed))
    }

    override suspend fun denyConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.ActionNotAllowed))
    }

    override suspend fun resetConsent(context: Context): Result<Unit> {
        resetConsentsAndNotify()
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation.reset()
        stopObservingSharedPreferencesIabStrings(context)
        return initializeUserMessagingPlatform(context)
    }

    override suspend fun initialize(
        context: Context,
        moduleConfiguration: ModuleConfiguration
    ): Result<Unit> {
        return initializeUserMessagingPlatform(context)
    }

    private suspend fun initializeUserMessagingPlatform(context: Context): Result<Unit> {
        val params = ConsentRequestParameters
            .Builder()
            .setConsentDebugSettings(consentDebugSettings)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        if (context !is Activity) {
            ChartboostCoreLogger.d("Cannot request consent info update due to no activity.")
            return Result.failure(ChartboostCoreException(ChartboostCoreError.InitializationError.ActivityRequired))
        }
        return suspendCancellableCoroutine { continuation ->
            consentInformation.requestConsentInfoUpdate(
                context,
                params,
                {
                    ChartboostCoreLogger.d("Google User Messaging Platform consent fetch succeeded")
                    startObservingSharedPreferencesIabStrings(context)
                    mutableConsents.putAll(sharedPreferencesIabStrings)
                    shouldCollectConsent = true
                    continuation.resume(Result.success(Unit))
                },
                { requestConsentError ->
                    ChartboostCoreLogger.d("Unable to get consent information: $requestConsentError")
                    continuation.resume(Result.failure(ChartboostCoreException(ChartboostCoreError.InitializationError.Exception)))
                })
        }
    }

    private fun resetConsentsAndNotify() {
        consents.forEach {
            Utils.safeExecute {
                listener?.onConsentChange(it.key)
            }
        }
        mutableConsents.clear()
    }
}

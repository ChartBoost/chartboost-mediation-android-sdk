/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.core.consent.unmanaged

import android.app.Activity
import android.content.Context
import com.chartboost.core.ChartboostCoreLogger
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
import org.json.JSONObject

/**
 * This adapter forwards all consent information directly and does no checks whatsoever. It is not
 * recommended to use this adapter. There is no underlying consent management platform associated
 * with this adapter, and the adapter is completely reliant on the publisher to provide the consent values.
 */
class UnmanagedAdapter() : ConsentAdapter, Module {

    companion object {
        /**
         * The unmanaged adapter module ID.
         */
        const val moduleId = "unmanaged_consent_adapter"

        /**
         * The unmanaged adapter module version from the gradle file.
         */
        const val moduleVersion = BuildConfig.CHARTBOOST_CORE_UNMANAGED_CONSENT_ADAPTER_VERSION
    }

    override fun updateCredentials(context: Context, credentials: JSONObject) {
        if (credentials.has("shouldUseIabStringsFromSharedPreferences")) {
            // This defaults false
            shouldUseIabStringsFromSharedPreferences =
                credentials.optBoolean("shouldUseIabStringsFromSharedPreferences")
        }
    }

    override val moduleId: String = Companion.moduleId

    override val moduleVersion: String = Companion.moduleVersion

    override var shouldCollectConsent: Boolean = false

    override var consents: MutableMap<ConsentKey, ConsentValue> = mutableMapOf()
        set(value) {
            // Keep a reference to the old field
            val oldConsents = field
            field = value
            if (shouldUseIabStringsFromSharedPreferences) {
                field.putAll(sharedPreferencesIabStrings)
            }
            // Notify changed values
            field.forEach {
                if (oldConsents[it.key] != field[it.key]) {
                    listener?.onConsentChange(it.key)
                }
            }
        }

    override val sharedPreferencesIabStrings: MutableMap<String, String> = mutableMapOf()

    override val sharedPreferenceChangeListener: ConsentAdapter.IabSharedPreferencesListener =
        ConsentAdapter.IabSharedPreferencesListener(sharedPreferencesIabStrings)

    override var listener: ConsentAdapterListener? = null
        set(value) {
            field = value
            sharedPreferenceChangeListener.listener = object : ConsentAdapterListener {
                override fun onConsentChange(consentKey: ConsentKey) {
                    if (shouldUseIabStringsFromSharedPreferences) {
                        sharedPreferencesIabStrings[consentKey]?.let {
                            consents[consentKey] = it
                        } ?: consents.remove(consentKey)
                    }
                    value?.onConsentChange(consentKey)
                }

            }
        }

    /**
     * Use this to toggle whether or not the unmanaged adapter automatically uses the IAB consents
     * from shared preferences. It is recommended to set this before initializing ChartboostCore.
     */
    var shouldUseIabStringsFromSharedPreferences = false

    /**
     * Do not call this method. Call the equivalent directly on the consent management platform that you're using.
     */
    override suspend fun showConsentDialog(
        activity: Activity, dialogType: ConsentDialogType
    ): Result<Unit> {
        // It is expected some other consent management platform is taking care of the dialogs.
        return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.ActionNotAllowed))
    }

    /**
     * Do not call this method. Call the equivalent directly on the consent management platform that you're using.
     */
    override suspend fun grantConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        ChartboostCoreLogger.w("Attempting to grant consent on the unmanaged adapter does nothing.")
        return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.ActionNotAllowed))
    }

    /**
     * Do not call this method. Call the equivalent directly on the consent management platform that you're using.
     */
    override suspend fun denyConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        ChartboostCoreLogger.w("Attempting to deny consent on the unmanaged adapter does nothing.")
        return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.ActionNotAllowed))
    }

    /**
     * Do not call this method. Call the equivalent directly on the consent management platform that you're using.
     */
    override suspend fun resetConsent(context: Context): Result<Unit> {
        ChartboostCoreLogger.w("Attempting to reset the unmanaged adapter does nothing.")
        return Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.ActionNotAllowed))
    }

    override suspend fun initialize(context: Context, moduleConfiguration: ModuleConfiguration): Result<Unit> {
        if (shouldUseIabStringsFromSharedPreferences) {
            startObservingSharedPreferencesIabStrings(context)
            consents.putAll(sharedPreferencesIabStrings)
        }
        return Result.success(Unit)
    }
}

/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.core.consent.reference

import android.app.Activity
import android.content.Context
import com.chartboost.core.ChartboostCoreLogger
import com.chartboost.core.consent.ConsentAdapter
import com.chartboost.core.consent.ConsentAdapterListener
import com.chartboost.core.consent.ConsentDialogType
import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentKeys
import com.chartboost.core.consent.ConsentSource
import com.chartboost.core.consent.ConsentValue
import com.chartboost.core.consent.reference.sdk.ReferenceConsentSdk
import com.chartboost.core.error.ChartboostCoreError
import com.chartboost.core.error.ChartboostCoreException
import com.chartboost.core.initialization.Module
import com.chartboost.core.initialization.ModuleConfiguration
import org.json.JSONObject

/**
 * FOR DEMO AND TESTING PURPOSES ONLY. DO NOT USE DIRECTLY.
 *
 * An adapter that is used for reference purposes. It is designed to showcase and test the
 * ConsentAdapter contract of the Chartboost Core SDK.
 *
 * Implementations of the ConsentAdapter interface may roughly model their own design after this class,
 * but do NOT call this adapter directly.
 */
class ReferenceConsentAdapter() : ConsentAdapter, Module {

    companion object {
        /**
         * Please use a unique module identifier.
         */
        const val moduleId = "reference_consent_adapter"

        /**
         * The consent adapter version.
         *
         * You may version the adapter using any preferred convention, but it is recommended to apply the
         * following format if the adapter will be published by Chartboost Core:
         *
         * Chartboost Core.CMP.Adapter
         *
         * "Chartboost Core" represents the Chartboost Core SDK’s major version that is compatible with this adapter. This must be 1 digit.
         * "CMP" represents the consent management platform SDK’s major.minor.patch.x (where x is optional) version that is compatible with this adapter. This can be 3-4 digits.
         * "Adapter" represents this adapter’s version (starting with 0), which resets to 0 when the CMP SDK’s version changes. This must be 1 digit.
         */
        const val moduleVersion = BuildConfig.CHARTBOOST_CORE_REFERENCE_CONSENT_ADAPTER_VERSION

        /**
         * This is some arbitrary [ConsentKey] for demonstration purposes.
         */
        const val REFERENCE_CONSENT_STATUS_KEY: ConsentKey = "reference_consent_status"
    }

    override fun updateCredentials(context: Context, credentials: JSONObject) {
        // Use this method to update any state that you expect to get from the server.
    }

    override val moduleId: String = Companion.moduleId

    override val moduleVersion: String = Companion.moduleVersion

    /**
     * It is generally advised to ping the underlying SDK for this value.
     */
    override val shouldCollectConsent: Boolean
        get() = ReferenceConsentSdk.shouldShowConsentDialog

    /**
     * The getter for the read-only verson of the map of consents.
     */
    override val consents: Map<ConsentKey, ConsentValue>
        get() = mutableConsents

    /**
     * It is generally recommended to keep a private backing mutable version of the consent map.
     */
    private val mutableConsents = mutableMapOf<ConsentKey, ConsentValue>()

    /**
     * These are for automatically grabbing the [ConsentKeys] from shared preferences.
     */
    override val sharedPreferencesIabStrings: MutableMap<String, String> = mutableMapOf()

    /**
     * This listener takes advantage of the default implementation of automatically grabbing IAB
     * consent values from shared preferences.
     */
    override val sharedPreferenceChangeListener: ConsentAdapter.IabSharedPreferencesListener =
        ConsentAdapter.IabSharedPreferencesListener(sharedPreferencesIabStrings)

    /**
     * This is the external listener that ChartboostCore takes and aggregates consent changes.
     */
    override var listener: ConsentAdapterListener? = null
        set(value) {
            field = value
            // Also set the listener in the shared preferences change listener
            sharedPreferenceChangeListener.listener = object : ConsentAdapterListener {
                override fun onConsentChange(consentKey: ConsentKey) {
                    sharedPreferencesIabStrings[consentKey]?.let {
                        mutableConsents[consentKey] = it
                    } ?: mutableConsents.remove(consentKey)
                    value?.onConsentChange(consentKey)
                }

            }
        }

    /**
     * Make sure to show the concise and detailed dialogs.
     */
    override suspend fun showConsentDialog(
        activity: Activity, dialogType: ConsentDialogType
    ): Result<Unit> {
        return when (dialogType) {
            ConsentDialogType.CONCISE -> {
                ReferenceConsentSdk.showConciseDialog(activity)
                Result.success(Unit)
            }

            ConsentDialogType.DETAILED -> {
                ReferenceConsentSdk.showDetailedDialog(activity)
                Result.success(Unit)
            }

            else -> {
                // This shouldn't happen, so this returns a failure.
                ChartboostCoreLogger.d("Unexpected consent dialog type: $dialogType")
                Result.failure(ChartboostCoreException(ChartboostCoreError.ConsentError.DialogShowError))
            }
        }
    }

    /**
     * This is for publisher-specified granting of consent that bypasses a consent dialog. Not all
     * consent management platforms allow this.
     */
    override suspend fun grantConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        val previousConsentStatus = ReferenceConsentSdk.referenceConsentStatus
        ReferenceConsentSdk.grantConsent()
        mutableConsents[REFERENCE_CONSENT_STATUS_KEY] = ReferenceConsentSdk.referenceConsentStatus
        // Only notify if anything changed
        if (previousConsentStatus != ReferenceConsentSdk.referenceConsentStatus) {
            listener?.onConsentChange(REFERENCE_CONSENT_STATUS_KEY)
        }
        return Result.success(Unit)
    }

    /**
     * This is for publisher-specified denying of consent that bypasses a consent dialog. Not all
     * consent management platforms allow this.
     */
    override suspend fun denyConsent(
        context: Context, statusSource: ConsentSource
    ): Result<Unit> {
        val previousConsentStatus = ReferenceConsentSdk.referenceConsentStatus
        ReferenceConsentSdk.denyConsent()
        mutableConsents[REFERENCE_CONSENT_STATUS_KEY] = ReferenceConsentSdk.referenceConsentStatus
        // Only notify if anything changed
        if (previousConsentStatus != ReferenceConsentSdk.referenceConsentStatus) {
            listener?.onConsentChange(REFERENCE_CONSENT_STATUS_KEY)
        }
        return Result.success(Unit)
    }

    /**
     * It is recommended to clear consents and re-initialize the underlying consent management platform.
     */
    override suspend fun resetConsent(context: Context): Result<Unit> {
        val previousConsentStatus = ReferenceConsentSdk.referenceConsentStatus
        // Some consent management platforms use this to clear all consents and re-fetch them.
        // Sometimes it's also a good idea to automatically re-initialize the CMP.
        ReferenceConsentSdk.reset()
        // Make sure to remove the old observer
        stopObservingSharedPreferencesIabStrings(context)
        // Here we automatically re-initialize the adapter
        initializeReferenceSdkAndSetConsents(context)
        // If the consent status changed, then we notify ChartboostCore of the change.
        if (previousConsentStatus != ReferenceConsentSdk.referenceConsentStatus) {
            listener?.onConsentChange(REFERENCE_CONSENT_STATUS_KEY)
        }
        return Result.success(Unit)
    }

    /**
     * Initializes the consent management platform and fetches initial consent.
     */
    override suspend fun initialize(context: Context, moduleConfiguration: ModuleConfiguration): Result<Unit> {
        initializeReferenceSdkAndSetConsents(context)
        return Result.success(Unit)
    }

    /**
     * This makes it easier to re-initialize after a reset.
     */
    private fun initializeReferenceSdkAndSetConsents(context: Context) {
        // Initialize the underlying consent management platform
        ReferenceConsentSdk.initialize()
        mutableConsents[REFERENCE_CONSENT_STATUS_KEY] = ReferenceConsentSdk.referenceConsentStatus
        // It's recommended that an adapter automatically fetches the IAB consents from shared preferences
        startObservingSharedPreferencesIabStrings(context)
        // As a convenience, it is also recommended to update the consents map with the IAB consents
        mutableConsents.putAll(sharedPreferencesIabStrings)
    }
}

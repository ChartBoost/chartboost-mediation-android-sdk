/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.controllers

import android.content.Context
import com.chartboost.heliumsdk.PartnerConsents
import com.chartboost.heliumsdk.utils.LogController
import org.json.JSONException
import org.json.JSONObject

/**
 * @suppress
 *
 * This class manages privacy settings for the Helium SDK.
 */
class PrivacyController(context: Context, private val partnerConsents: PartnerConsents) {
    /**
     * Internal keys used for reading/writing privacy settings to the SharedPreferences.
     */
    companion object {
        private const val heliumPrivacyIdentifier = "helium_privacy_id"
        private const val heliumCoppaKey = "helium_coppa"
        private const val heliumGdprKey = "helium_GDPR"
        private const val heliumUserConsentKey = "helium_user_consent"
        private const val heliumCcpaConsentKey = "helium_ccpa_consent"
        private const val HELIUM_PARTNER_CONSENTS_MAP_KEY = "helium_partner_consents_map"
    }

    init {
        partnerConsents.addPartnerConsentsObserver(object :
            PartnerConsents.PartnerConsentsObserver {
            override fun onPartnerConsentsUpdated() {
                savePartnerConsentsToDisk()
            }
        })
    }

    private val sharedPreferences = context.getSharedPreferences(
        heliumPrivacyIdentifier,
        Context.MODE_PRIVATE
    )

    /**
     * Whether or not the disk fetch for PrivacyConsents has happened.
     */
    private var hasUpdatedFromDisk = false

    /**
     * SharedPreferences can only store primitives. We need to keep track of tri-state statuses.
     */
    enum class PrivacySetting(val value: Int) {
        TRUE(1),
        FALSE(0),
        UNSET(-1)
    }

    /**
     * Privacy string for CCPA.
     */
    enum class PrivacyString(val consentString: String) {
        GRANTED("1YN-"),
        DENIED("1YY-"),
    }

    /**
     * Is this user covered under the COPPA regulations?
     * It is 0 or 1 for not covered (no problem) or covered (bunch of rules)
     * If the developer has not specifically set COPPA it should default to 0.
     */
    var coppa: Boolean?
        get() = intToBoolean(sharedPreferences.getInt(heliumCoppaKey, PrivacySetting.FALSE.value))
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putInt(heliumCoppaKey, booleanToInt(value ?: return))
            editor.apply()
        }

    /**
     * Is this user covered under GDPR?
     * 1 for covered. 0 for not.
     * If the developer did not specifically set it, do not send the node at all.
     */
    var gdpr: Int
        get() = sharedPreferences.getInt(heliumGdprKey, PrivacySetting.UNSET.value)
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putInt(heliumGdprKey, booleanToInt(value == PrivacySetting.TRUE.value))
            editor.apply()
        }

    /**
     * This represents a user consent when GDPR applies (int 0 or 1).
     * This is only sent if GDPR is specifically set to 1.
     * If GDPR is not set, or it is set to 0, do not send the node at all.
     * TODO: What if user consent is not set?
     */
    var userConsent: Boolean?
        get() = intToBoolean(
            sharedPreferences.getInt(
                heliumUserConsentKey,
                PrivacySetting.FALSE.value
            )
        )
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putInt(heliumUserConsentKey, booleanToInt(value ?: return))
            editor.apply()
        }

    /**
     * Handles getting and setting the CCPA consent.
     * Only get if the CCPA consent exists, else return null.
     * Only set if CCPA consent is true/false. If null, return.
     */
    var ccpaConsent: Boolean?
        get() = if (sharedPreferences.contains(heliumCcpaConsentKey)) {
            sharedPreferences.getBoolean(heliumCcpaConsentKey, false)
        } else null
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(heliumCcpaConsentKey, value ?: return)
            editor.apply()
        }

    /**
     * Serializes partner consents to shared preferences.
     */
    private fun savePartnerConsentsToDisk() {
        val editor = sharedPreferences.edit()
        editor.putString(
            HELIUM_PARTNER_CONSENTS_MAP_KEY,
            JSONObject(partnerConsents.getPartnerIdToConsentGivenMapCopy()).toString()
        )
        editor.apply()
    }

    /**
     * Reads a serialized form of partner consents from shared preferences. This can only happen once.
     */
    internal fun updatePartnerConsentsFromDisk() {
        if (hasUpdatedFromDisk) {
            return
        }
        hasUpdatedFromDisk = true
        val partnerConsentsMapString =
            sharedPreferences.getString(HELIUM_PARTNER_CONSENTS_MAP_KEY, "")
        if (partnerConsentsMapString.isNullOrEmpty()) {
            LogController.d("No partner consents map saved.")
            return
        }
        try {
            val jsonObject = JSONObject(partnerConsentsMapString)
            val consentMap: MutableMap<String, Boolean> = mutableMapOf()
            jsonObject.keys().forEach {
                consentMap[it] = jsonObject.getBoolean(it)
            }
            partnerConsents.mergePartnerConsentsFromDisk(consentMap)
            savePartnerConsentsToDisk()
        } catch (e: JSONException) {
            LogController.d("Unable to recreate partner consents map.")
        }
    }

    private fun booleanToInt(bool: Boolean): Int {
        return when (bool) {
            true -> PrivacySetting.TRUE.value
            false -> PrivacySetting.FALSE.value
        }
    }

    private fun intToBoolean(int: Int): Boolean? {
        return when (int) {
            PrivacySetting.TRUE.value -> true
            PrivacySetting.FALSE.value -> false
            else -> null
        }
    }
}

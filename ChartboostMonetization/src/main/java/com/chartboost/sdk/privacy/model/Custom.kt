package com.chartboost.sdk.privacy.model

import com.chartboost.sdk.privacy.model.GDPR.Companion.GDPR_STANDARD

/**
 * Custom class implements DataUseConsent. Allows to provide custom privacy data to comply
 * with any type of required privacy law. User can provide any type of privacy standard and
 * consent as Strings.
 * - Values should be less than 100 characters.
 * - Values will be checked and evaluated by server side.
 * - Cannot pass null values
 * - Cannot pass empty values
 * - Cannot use restricted privacyStandard
 */
class Custom(
    private val customPrivacyStandard: String,
    private val customConsent: String,
) : GenericDataUseConsent() {
    init {
        customInit()
    }

    private fun customInit() {
        if (customPrivacyStandard.isEmpty() || customConsent.isEmpty()) {
            handleException("Invalid Custom privacy standard name. Values cannot be null")
            return
        }

        if (isCustomPrivacyStandardReserved(customPrivacyStandard)) {
            handleException("Invalid Custom privacy standard name. Cannot use GDPR as privacy standard")
            return
        }

        if (!isValidConsent(customPrivacyStandard) || !isValidConsent(customConsent)) {
            handleException(
                "Invalid Custom consent values." +
                    " Use valid values between 1 and 100 characters." +
                    " privacyStandard: " + customPrivacyStandard + " consent: " + customConsent,
            )
            return
        }

        this.privacyStandardName = customPrivacyStandard
        this.consentValue = customConsent
    }

    override val consent: String
        get() = consentValue as String

    private fun isValidConsent(consent: String): Boolean {
        return consent.length in 1..99
    }

    /**
     * GDPR is not allowed as Custom value because,
     * it has already defined and stable values expected by the server
     * @param privacyStandard
     * @return
     */
    private fun isCustomPrivacyStandardReserved(privacyStandard: String?): Boolean {
        return GDPR_STANDARD == privacyStandard?.trim()?.lowercase()
    }
}

package com.chartboost.sdk.privacy.model

/**
 * Interface for DataUseConsent which will be implemented by GDPR, CCPA, COPPA, LGPD, Custom classes
 * and quite possibly any new upcoming privacy laws. This is public interface which will provide
 * data to the user.
 */
interface DataUseConsent {
    /**
     * @return Privacy Standard for GDPR, CCPA, COPPA, LGPD or Custom
     */
    val privacyStandard: String

    /**
     * @return Consent set via GDPR, CCPA, COPPA, LGPD or Custom class
     */
    val consent: Any
}

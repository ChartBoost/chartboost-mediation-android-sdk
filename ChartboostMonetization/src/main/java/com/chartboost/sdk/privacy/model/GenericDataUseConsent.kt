package com.chartboost.sdk.privacy.model

import com.chartboost.sdk.internal.di.getEventTracker
import com.chartboost.sdk.tracking.CriticalEvent
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.TrackingEventName

/**
 * Internal class which implemented DataUseConsent consent for the clarity and cleaner code.
 * Every specific law extends this interface implementation.
 */
abstract class GenericDataUseConsent(
    eventTracker: EventTrackerExtensions = getEventTracker(),
) : DataUseConsent, EventTrackerExtensions by eventTracker {
    /**
     * Value not visible to public
     * @return Privacy Standard for GDPR, CCPA, COPPA, LGPD or Custom
     */
    protected var privacyStandardName: String = ""

    override val privacyStandard: String
        get() = privacyStandardName

    /**
     * Value not visible to public
     * @return Consent set via GDPR, CCPA, COPPA, LGPD or Custom class
     */
    protected var consentValue: Any = ""

    protected fun handleException(msg: String?) {
        try {
            CriticalEvent(
                TrackingEventName.Consent.CREATION_ERROR,
                msg ?: "no message",
                "",
                "",
            ).track()
            throw Exception(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

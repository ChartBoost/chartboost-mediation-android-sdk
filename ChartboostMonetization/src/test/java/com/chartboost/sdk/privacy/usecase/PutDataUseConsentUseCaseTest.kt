package com.chartboost.sdk.privacy.usecase

import com.chartboost.sdk.privacy.PrivacyStandardRepository
import com.chartboost.sdk.privacy.model.Custom
import com.chartboost.sdk.privacy.model.DataUseConsent
import com.chartboost.sdk.privacy.model.GDPR
import com.chartboost.sdk.privacy.model.LGPD
import com.chartboost.sdk.test.relaxedMockk
import com.chartboost.sdk.tracking.EventTrackerExtensions
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PutDataUseConsentUseCaseTest {
    private val repository: PrivacyStandardRepository = mockk(relaxed = true)
    private val eventTrackerMock: EventTrackerExtensions = relaxedMockk()

    private val useCase: PutDataUseConsentUseCase =
        PutDataUseConsentUseCaseImpl(
            repository,
            eventTrackerMock,
        )

    @ParameterizedTest(name = "put DataUseConsentTest item:{index} ")
    @MethodSource("consents")
    fun putDataUseConsentTest(input: DataUseConsent) {
        val captor = slot<DataUseConsent>()
        useCase.execute(input)
        coVerify(exactly = 1) { repository.put(capture(captor)) }
        Assertions.assertEquals(input, captor.captured)
    }

    companion object {
        @JvmStatic
        fun consents() =
            listOf(
                Custom("test", "teat"),
                GDPR(GDPR.GDPR_CONSENT.BEHAVIORAL),
                GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL),
                LGPD(false),
                LGPD(true),
            ).map { Arguments.of(it) }
    }

    @Test
    fun putDataUseConsentTest() {
        val consentCapture = slot<DataUseConsent>()
        val custom: DataUseConsent = Custom("test", "teat")
        useCase.execute(custom)
        coVerify(exactly = 1) { repository.put(capture(consentCapture)) }
        Assertions.assertEquals(custom, consentCapture.captured)
    }
}

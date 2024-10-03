package com.chartboost.sdk.privacy.usecase

import com.chartboost.sdk.privacy.PrivacyStandardRepository
import com.chartboost.sdk.privacy.model.*
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class RemoveDataUseConsentUseCaseTest {
    private val repository: PrivacyStandardRepository = mockk(relaxed = true)
    private val useCase = RemoveDataUseConsentUseCase(repository)

    @ParameterizedTest(name = "remove DataUseConsentTest item:{index} ")
    @MethodSource("consents")
    fun putDataUseConsentTest(input: String) {
        val captor = slot<String>()
        useCase.execute(input)
        coVerify(exactly = 1) { repository.remove(capture(captor)) }
        Assertions.assertEquals(input, captor.captured)
    }

    companion object {
        @JvmStatic
        fun consents() =
            listOf(
                "test",
                GDPR.GDPR_STANDARD,
                LGPD.LGPD_STANDARD,
                COPPA.COPPA_STANDARD,
                CCPA.CCPA_STANDARD,
            ).map { Arguments.of(it) }
    }
}

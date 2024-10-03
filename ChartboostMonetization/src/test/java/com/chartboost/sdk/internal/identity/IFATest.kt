package com.chartboost.sdk.internal.identity

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class IFATest {
    private val googleAdvertisingIdMock = mockk<GoogleAdvertisingId>()
    private val amazonAdvertisingIdMock = mockk<AmazonAdvertisingId>()

    @Test
    fun `get advertising id for google device`() {
        val ifa =
            IFA(
                googleAdvertisingIdMock,
                amazonAdvertisingIdMock,
                "Google",
            )

        every { googleAdvertisingIdMock.getAdvertisingIdHolder() } returns
            AdvertisingIDHolder(
                TrackingState.TRACKING_ENABLED,
                "0000-0000-0000-000",
            )

        with(ifa.getAdvertisingIdHolder()) {
            assertEquals(TrackingState.TRACKING_ENABLED, advertisingIDState)
            assertEquals(advertisingID, "0000-0000-0000-000")
        }
    }

    @Test
    fun `get advertising id for amazon device`() {
        val ifa =
            IFA(
                googleAdvertisingIdMock,
                amazonAdvertisingIdMock,
                "Amazon",
            )

        every { amazonAdvertisingIdMock.getAdvertisingIdHolder() } returns
            AdvertisingIDHolder(
                TrackingState.TRACKING_LIMITED,
                "0000-0000-0000-000",
            )

        with(ifa.getAdvertisingIdHolder()) {
            assertEquals(TrackingState.TRACKING_LIMITED, advertisingIDState)
            assertEquals(advertisingID, "0000-0000-0000-000")
        }
    }
}

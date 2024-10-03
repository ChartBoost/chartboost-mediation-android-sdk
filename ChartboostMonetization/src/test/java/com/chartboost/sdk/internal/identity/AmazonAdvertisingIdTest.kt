package com.chartboost.sdk.internal.identity

import android.app.Application
import android.content.ContentResolver
import com.chartboost.sdk.internal.di.ChartboostDependencyContainer
import com.chartboost.sdk.internal.di.PrivacyComponent
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.privacy.model.COPPA
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class AmazonAdvertisingIdTest {
    private val contextMock = mockk<Application>()
    private val contentResolver = mockk<ContentResolver>()
    private val amazonAdvertisingId = AmazonAdvertisingId(contextMock, contentResolver)

    @Test
    fun `get advertising id holder coppa true`() {
        mockkObject(ChartboostDependencyContainer)
        val privacyComponentMock = mockk<PrivacyComponent>()
        val privacyApiMock = mockk<PrivacyApi>()
        every { privacyApiMock.getPrivacyStandard(any()) } returns COPPA(true)
        every { privacyComponentMock.privacyApi } returns privacyApiMock
        every { ChartboostDependencyContainer.privacyComponent } returns privacyComponentMock
        every { contextMock.applicationContext } returns contextMock

        with(amazonAdvertisingId.getAdvertisingIdHolder()) {
            assertEquals(TrackingState.TRACKING_LIMITED, advertisingIDState)
            assertNull(advertisingID)
        }
        every { privacyApiMock.getPrivacyStandard(any()) } returns null
        unmockkObject(ChartboostDependencyContainer)
    }

    @Test
    fun `get advertising id holder`() {
        every { contextMock.applicationContext } returns contextMock
        with(amazonAdvertisingId.getAdvertisingIdHolder()) {
            assertEquals(TrackingState.TRACKING_ENABLED, advertisingIDState)
            assertNull(advertisingID)
        }
    }
}

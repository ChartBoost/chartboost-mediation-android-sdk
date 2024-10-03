package com.chartboost.sdk.internal

import android.content.Context
import com.chartboost.sdk.callbacks.StartCallback
import com.chartboost.sdk.internal.Libraries.BidderTokenGenerator
import com.chartboost.sdk.internal.identity.CBIdentity
import com.chartboost.sdk.internal.initialization.SdkInitializer
import com.chartboost.sdk.privacy.model.*
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.ScheduledExecutorService

@RunWith(MockitoJUnitRunner::class)
class ChartboostApiTest {
    private val contextMock = mockk<Context>()
    private val backgroundExecutorMock = mockk<ScheduledExecutorService>()
    private val initializerMock = mockk<SdkInitializer>()
    private val tokenGeneratorMock = mockk<BidderTokenGenerator>()
    private val identityMock = mockk<CBIdentity>()
    val runnableCaptor = slot<Runnable>()

    private val chartboostApi =
        ChartboostApi(
            contextMock,
            backgroundExecutorMock,
            initializerMock,
            tokenGeneratorMock,
            identityMock,
        )

    @Before
    fun setup() {
        every { initializerMock.initSdk(any(), any(), any()) } just Runs
        every { tokenGeneratorMock.generateBidderToken() } returns "tokenBase64"
        every { identityMock.toIdentityBodyFields() } returns mockk()
        every { backgroundExecutorMock.execute(capture(runnableCaptor)) } just runs
    }

    @Test
    fun `start with the app id`() {
        val startCallbackMock = mockk<StartCallback>()
        chartboostApi.startWithAppId(
            "test",
            "test",
            startCallbackMock,
        )
        verify(exactly = 1) { backgroundExecutorMock.execute(capture(runnableCaptor)) }
        assertNotNull(runnableCaptor.captured)
        runnableCaptor.captured.run()
        io.mockk.verify(exactly = 1) { identityMock.toIdentityBodyFields() }
        io.mockk.verify(exactly = 1) { initializerMock.initSdk("test", "test", startCallbackMock) }
    }

    @Test
    fun `get token valid`() {
        val token = chartboostApi.getBidderToken()
        assertNotNull(token)
        assertEquals("tokenBase64", token)
    }

    @Test
    fun `get token invalid`() {
        every { tokenGeneratorMock.generateBidderToken() } answers { "" }
        val token = chartboostApi.getBidderToken()
        assertNotNull(token)
        assertEquals("", token)
    }
}

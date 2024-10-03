package com.chartboost.sdk.internal.Networking

import android.content.Context
import com.chartboost.sdk.internal.Networking.requests.NetworkType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test

class CBReachabilityTestKotlin {
    @MockK
    private lateinit var mockContext: Context

    private val reachability
        get() = CBReachability(mockContext)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `when calling openRTBConnectionType it should return value from Context#openRTBConnectionType`() {
        mockkStatic(Context::openRTBConnectionType)
        NetworkType.values().forEach {
            every { any<Context>().openRTBConnectionType() } returns it
            reachability.openRTBConnectionType() shouldBe it
        }
    }

    @Test
    fun `when calling connectionTypeAsString it should return string name from Context#openRTBConnectionType`() {
        mockkStatic(Context::openRTBConnectionType)
        NetworkType.values().forEach {
            every { any<Context>().openRTBConnectionType() } returns it
            reachability.connectionTypeAsString() shouldBe it.asString
        }
    }

    @Test
    fun `when calling cellularConnectionType it should return value from Context#networkConnectionType`() {
        mockkStatic(Context::networkConnectionType)
        (0..10).forEach {
            every { any<Context>().networkConnectionType() } returns it
            reachability.cellularConnectionType() shouldBe it
        }
    }

    @Test
    fun `when calling isConnectionCellular it should return true if connection is mobile`() {
        mockkStatic(Context::isNetworkConnected)
        every { any<Context>().isNetworkConnected() } returns true
        every { any<Context>().isWifiConnected() } returns false
        every { any<Context>().isMobileConnected() } returns true
        reachability.isConnectionCellular().shouldBeTrue()
    }

    @Test
    fun `when calling isConnectionCellular it should return false if connection is wifi`() {
        mockkStatic(Context::isNetworkConnected)
        every { any<Context>().isNetworkConnected() } returns true
        every { any<Context>().isWifiConnected() } returns true
        every { any<Context>().isMobileConnected() } returns true
        reachability.isConnectionCellular().shouldBeFalse()
    }

    @Test
    fun `when calling isConnectionCellular it should return false if no connection`() {
        mockkStatic(Context::isNetworkConnected)
        every { any<Context>().isNetworkConnected() } returns false
        every { any<Context>().isWifiConnected() } returns true
        every { any<Context>().isMobileConnected() } returns true
        reachability.isConnectionCellular().shouldBeFalse()
    }
}

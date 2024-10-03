package com.chartboost.sdk.internal.Networking

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import com.chartboost.sdk.internal.Networking.requests.NetworkType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectivityTest {
    private val networkInfoMock = mockk<NetworkInfo>(relaxed = true)
    private val connectivityManagerMock = mockk<ConnectivityManager>(relaxed = true)
    private val contextMock = mockk<Context>(relaxed = true)

    @BeforeEach
    fun setup() {
        every { contextMock.getSystemService(CONNECTIVITY_SERVICE) } returns connectivityManagerMock
        every { connectivityManagerMock.activeNetworkInfo } returns networkInfoMock
    }

    @Test
    fun isConnectedTrueTest() {
        every { networkInfoMock.isConnected } returns true
        contextMock.isNetworkConnected().shouldBeTrue()
    }

    @Test
    fun isConnectedFalseTest() {
        every { networkInfoMock.isConnected } returns false
        contextMock.isNetworkConnected().shouldBeFalse()
    }

    @Test
    fun isConnectedMobileTest() {
        every { networkInfoMock.isConnected } returns true
        every { networkInfoMock.type } returns ConnectivityManager.TYPE_MOBILE
        contextMock.isMobileConnected().shouldBeTrue()
    }

    @Test
    fun isConnectedWifiTest() {
        every { networkInfoMock.isConnected } returns true
        every { networkInfoMock.type } returns ConnectivityManager.TYPE_WIFI
        contextMock.isWifiConnected().shouldBeTrue()
    }

    @Test
    fun isConnectedVPNTest() {
        every { networkInfoMock.isConnected } returns true
        every { networkInfoMock.type } returns ConnectivityManager.TYPE_VPN
        contextMock.isWifiConnected().shouldBeFalse()
        contextMock.isMobileConnected().shouldBeFalse()
    }

    @Test
    fun getActiveNetworkInfoTest() {
        contextMock.activeNetworkInfo().shouldNotBeNull()
    }

    @Test
    fun getActiveNetworkInfoNoServiceTest() {
        every { contextMock.getSystemService(CONNECTIVITY_SERVICE) } returns null
        contextMock.activeNetworkInfo().shouldBeNull()
    }

    @Test
    fun getConnectivityManagerTest() {
        contextMock.connectivityManager().shouldNotBeNull()
    }

    @Test
    fun getConnectivityManagerNoServiceTest() {
        every { contextMock.getSystemService(CONNECTIVITY_SERVICE) } returns null
        contextMock.connectivityManager().shouldBeNull()
    }

    @Test
    fun getNetworkCapabilitiesTest() {
        val networkMock = mockk<Network>()
        every { connectivityManagerMock.getNetworkCapabilities(networkMock) } returns mockk()
        contextMock.networkCapabilities(networkMock).shouldNotBeNull()
    }

    @Test
    fun getNetworkCapabilitiesNetworkNullTest() {
        val networkCapabilitiesMock = mockk<NetworkCapabilities>()
        val networkMock = mockk<Network>()
        every { connectivityManagerMock.getNetworkCapabilities(networkMock) } returns networkCapabilitiesMock
        every { connectivityManagerMock.activeNetwork } returns networkMock
        contextMock.networkCapabilities(null).shouldNotBeNull()
    }

    @Test
    fun getNetworkCapabilitiesNoServiceTest() {
        every { contextMock.getSystemService(CONNECTIVITY_SERVICE) } returns null
        contextMock.networkCapabilities(null).shouldBeNull()
    }

    @Test
    fun getConnectionTypeConnectedTest() {
        every { connectivityManagerMock.activeNetworkInfo } returns networkInfoMock
        every { networkInfoMock.isConnected } returns true
        every { networkInfoMock.subtype } returns TelephonyManager.NETWORK_TYPE_LTE
        contextMock.networkConnectionType() shouldBe TelephonyManager.NETWORK_TYPE_LTE
    }

    @Test
    fun getConnectionTypeNotConnectedTest() {
        every { connectivityManagerMock.activeNetworkInfo } returns networkInfoMock
        every { networkInfoMock.isConnected } returns false
        contextMock.networkConnectionType() shouldBe TelephonyManager.NETWORK_TYPE_UNKNOWN
    }

    @Test
    fun getConnectionTypeUnknownTest() {
        every { networkInfoMock.isConnected } returns false
        contextMock.networkConnectionType() shouldBe TelephonyManager.NETWORK_TYPE_UNKNOWN
    }

    @Test
    fun getOpenRTBConnectionNotConnectedType() {
        every { connectivityManagerMock.activeNetworkInfo } returns networkInfoMock
        every { networkInfoMock.isConnected } returns false
        contextMock.openRTBConnectionType() shouldBe NetworkType.UNKNOWN
    }

    @Test
    fun getOpenRTBConnectionConnectedWifiType() {
        every { connectivityManagerMock.activeNetworkInfo } returns networkInfoMock
        every { networkInfoMock.isConnected } returns true
        every { networkInfoMock.type } returns ConnectivityManager.TYPE_WIFI
        every { networkInfoMock.subtype } returns TelephonyManager.NETWORK_TYPE_IDEN
        contextMock.openRTBConnectionType() shouldBe NetworkType.WIFI
    }

    @Test
    fun getOpenRTBConnectionConnectedMobile2GType() {
        val networks2g =
            listOf(
                TelephonyManager.NETWORK_TYPE_IDEN,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS,
            )
        checkOpenRTBConnectionConnectedByType(networks2g, NetworkType.CELLULAR_2G)
    }

    @Test
    fun getOpenRTBConnectionConnectedMobile3GType() {
        val networks3g =
            listOf(
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_HSPAP,
            )
        checkOpenRTBConnectionConnectedByType(networks3g, NetworkType.CELLULAR_3G)
    }

    @Test
    fun getOpenRTBConnectionConnectedMobile4GType() {
        val networks4g = listOf(TelephonyManager.NETWORK_TYPE_LTE)
        checkOpenRTBConnectionConnectedByType(networks4g, NetworkType.CELLULAR_4G)
    }

    @Test
    fun getOpenRTBConnectionConnectedMobile5GType() {
        val networks5g = listOf(TelephonyManager.NETWORK_TYPE_NR)
        checkOpenRTBConnectionConnectedByType(networks5g, NetworkType.CELLULAR_5G)
    }

    @Test
    fun getOpenRTBConnectionConnectedMobileUnknownType() {
        val unknown = listOf(TelephonyManager.NETWORK_TYPE_UNKNOWN)
        checkOpenRTBConnectionConnectedByType(unknown, NetworkType.CELLULAR_UNKNOWN)
    }

    @Test
    fun getOpenRTBConnectionConnectedMobileInvalidType() {
        val unknown = listOf(-1)
        checkOpenRTBConnectionConnectedByType(unknown, NetworkType.CELLULAR_UNKNOWN)
    }

    @Test
    fun getOpenRTBConnectionConnectedMobileEmptyTypes() {
        val unknown = emptyList<Int>()
        checkOpenRTBConnectionConnectedByType(unknown, NetworkType.CELLULAR_UNKNOWN)
    }

    private fun checkOpenRTBConnectionConnectedByType(
        networks: List<Int>,
        expectedNetworkType: NetworkType?,
    ) {
        every { connectivityManagerMock.activeNetworkInfo } returns networkInfoMock
        every { networkInfoMock.isConnected } returns true
        every { networkInfoMock.type } returns ConnectivityManager.TYPE_MOBILE
        networks.forEach { value ->
            every { networkInfoMock.subtype } returns value
            contextMock.openRTBConnectionType() shouldBe expectedNetworkType
        }
    }
}

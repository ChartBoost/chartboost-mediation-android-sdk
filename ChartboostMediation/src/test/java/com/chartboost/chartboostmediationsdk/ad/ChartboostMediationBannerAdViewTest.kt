/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

import android.view.View
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdLoadRequest
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdLoadResult
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdViewListener
import com.chartboost.chartboostmediationsdk.controllers.banners.BannerController
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

class ChartboostMediationBannerAdViewTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockBannerController: BannerController
    private lateinit var mockBannerListener: ChartboostMediationBannerAdViewListener
    private val request = mockk<ChartboostMediationBannerAdLoadRequest>()
    private val result = mockk<ChartboostMediationBannerAdLoadResult>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock the BannerController
        mockBannerController = mockk()

        // Mock the BannerListener
        mockBannerListener = mockk(relaxed = true)

        every { request.placement } returns "testPlacement"
        every { request.size } returns ChartboostMediationBannerSize.STANDARD
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `load`() =
        runBlocking {
            val ad =
                ChartboostMediationBannerAdView(
                    mockk(relaxed = true),
                    "testPlacement",
                    ChartboostMediationBannerSize.STANDARD,
                    mockBannerListener,
                )
            setBannerController(ad, mockBannerController)

            coEvery { mockBannerController.load(request) } returns result

            val res = ad.load(request)

            coVerify { mockBannerController.load(request) }

            assertEquals(res, result)
        }

    @Test
    fun `load with new placement`() =
        runBlocking {
            // Given a new placement
            val newPlacement = "newPlacement"
            every { request.placement } returns newPlacement

            val ad =
                ChartboostMediationBannerAdView(
                    mockk(relaxed = true),
                    "testPlacement",
                    ChartboostMediationBannerSize.STANDARD,
                    mockBannerListener,
                )
            setBannerController(ad, mockBannerController)

            coEvery { mockBannerController.load(request) } returns result

            coEvery { mockBannerController.renewCachedAd() } returns result

            // When the ad is loaded with the new placement name
            val res = ad.load(request)

            // Then the placement name of the ad should be updated
            assertEquals(newPlacement, ad.placement)
            assertEquals(ChartboostMediationBannerSize.STANDARD, ad.getSize())
            assertEquals(res, result)
        }

    @Test
    fun `load with new size`() =
        runBlocking {
            // Given a new size
            val newSize = ChartboostMediationBannerSize.MEDIUM
            every { request.size } returns newSize

            val ad =
                ChartboostMediationBannerAdView(
                    mockk(relaxed = true),
                    "testPlacement",
                    ChartboostMediationBannerSize.STANDARD,
                    mockBannerListener,
                )
            setBannerController(ad, mockBannerController)

            coEvery { mockBannerController.load(request) } returns result

            coEvery { mockBannerController.renewCachedAd() } returns result

            // When the ad is loaded with the new size
            val res = ad.load(request)

            // Then the size of the ad should be updated
            assertEquals("testPlacement", ad.placement)
            assertEquals(newSize, ad.getSize())
            assertEquals(res, result)
        }

    @Test
    fun `load with new placement and new size`() =
        runBlocking {
            // Given a new placement and size
            val newPlacement = "newPlacement"
            every { request.placement } returns newPlacement
            val newSize = ChartboostMediationBannerSize.MEDIUM
            every { request.size } returns newSize

            val ad =
                ChartboostMediationBannerAdView(
                    mockk(relaxed = true),
                    "testPlacement",
                    ChartboostMediationBannerSize.STANDARD,
                    mockBannerListener,
                )
            setBannerController(ad, mockBannerController)

            coEvery { mockBannerController.load(request) } returns result

            coEvery { mockBannerController.renewCachedAd() } returns result

            // When the ad is loaded with the new placement name and size
            val res = ad.load(request)

            // Then the placement name and size of the ad should be updated
            assertEquals(newPlacement, ad.placement)
            assertEquals(newSize, ad.getSize())
            assertEquals(res, result)
        }

    @Test
    fun `onViewAdded calls onAdViewAdded on listener`() =
        runBlocking {
            // Given an ad
            val ad =
                ChartboostMediationBannerAdView(
                    mockk(relaxed = true),
                    "testPlacement",
                    ChartboostMediationBannerSize.STANDARD,
                    mockBannerListener,
                )
            setBannerController(ad, mockBannerController)
            val mockChild = mockk<View>()
            every { mockBannerListener.onAdViewAdded(ad.placement, mockChild) } just runs

            // When onViewAdded is called
            ad.onViewAdded(mockChild)

            // Then onAdViewAdded on the listener is triggered with correct parameters
            verify { mockBannerListener.onAdViewAdded(ad.placement, mockChild) }
        }

    @Test
    fun `destroy sets chartboostMediationBannerAdListener to null`() =
        runBlocking {
            // Given an ad
            val ad =
                ChartboostMediationBannerAdView(
                    mockk(relaxed = true),
                    "testPlacement",
                    ChartboostMediationBannerSize.STANDARD,
                    mockBannerListener,
                )
            setBannerController(ad, mockBannerController)

            coEvery { mockBannerController.destroy() } just runs

            // When destroy is called
            ad.destroy()

            // Then chartboostMediationBannerAdListener is set to null
            assertNull(ad.chartboostMediationBannerAdViewListener)
        }

    private fun setBannerController(
        ad: ChartboostMediationBannerAdView,
        mockBannerController: BannerController,
    ) {
        try {
            val field: Field =
                ChartboostMediationBannerAdView::class.java.getDeclaredField("bannerController")
            field.isAccessible = true
            field.set(ad, mockBannerController)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

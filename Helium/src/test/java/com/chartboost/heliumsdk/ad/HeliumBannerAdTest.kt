/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

import android.view.View
import com.chartboost.heliumsdk.ad.HeliumBannerAd
import com.chartboost.heliumsdk.ad.HeliumBannerAd.HeliumBannerSize
import com.chartboost.heliumsdk.ad.HeliumBannerAdListener
import com.chartboost.heliumsdk.controllers.banners.BannerController
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

class HeliumBannerAdTest {

    private lateinit var mockBannerController: BannerController
    private lateinit var mockBannerListener: HeliumBannerAdListener

    @Before
    fun setUp() {
        // Mock the BannerController
        mockBannerController = mockk()

        // Mock the BannerListener
        mockBannerListener = mockk(relaxed = true)
    }

    @Test
    fun `load`() {
        // Given a new placement name
        val newPlacementName = "newPlacementName"
        val ad = HeliumBannerAd(
            mockk(relaxed = true),
            "testPlacement",
            HeliumBannerSize.STANDARD,
            mockBannerListener
        )
        setBannerController(ad, mockBannerController)

        every { mockBannerController.load() } just runs

        ad.load()

        verify { mockBannerController.load() }
    }

    @Test
    fun `load with new placement`() {
        // Given a new placement name
        val newPlacementName = "newPlacementName"
        val ad = HeliumBannerAd(
            mockk(relaxed = true),
            "testPlacement",
            HeliumBannerSize.STANDARD,
            mockBannerListener
        )
        setBannerController(ad, mockBannerController)

        every { mockBannerController.renewCachedAd() } just runs

        // When the ad is loaded with the new placement name
        ad.load(newPlacementName, HeliumBannerSize.STANDARD)

        // Then the placement name of the ad should be updated
        assertEquals(newPlacementName, ad.placementName)
        assertEquals(HeliumBannerSize.STANDARD, ad.getSize())
    }

    @Test
    fun `load with new size`() {
        // Given a new size
        val newSize = HeliumBannerSize.MEDIUM
        val ad = HeliumBannerAd(
            mockk(relaxed = true),
            "testPlacement",
            HeliumBannerSize.STANDARD,
            mockBannerListener
        )
        setBannerController(ad, mockBannerController)

        every { mockBannerController.renewCachedAd() } just runs

        // When the ad is loaded with the new size
        ad.load("testPlacement", newSize)

        // Then the size of the ad should be updated
        assertEquals("testPlacement", ad.placementName)
        assertEquals(newSize, ad.getSize())
    }

    @Test
    fun `load with new placement and new size`() {
        // Given a new placement name and size
        val newPlacementName = "newPlacementName"
        val newSize = HeliumBannerSize.MEDIUM
        val ad = HeliumBannerAd(
            mockk(relaxed = true),
            "testPlacement",
            HeliumBannerSize.STANDARD,
            mockBannerListener
        )
        setBannerController(ad, mockBannerController)

        every { mockBannerController.renewCachedAd() } just runs

        // When the ad is loaded with the new placement name and size
        ad.load(newPlacementName, newSize)

        // Then the placement name and size of the ad should be updated
        assertEquals(newPlacementName, ad.placementName)
        assertEquals(newSize, ad.getSize())
    }

    @Test
    fun `onViewAdded calls onAdViewAdded on listener`() {
        // Given an ad
        val ad = HeliumBannerAd(
            mockk(relaxed = true),
            "testPlacement",
            HeliumBannerSize.STANDARD,
            mockBannerListener
        )
        setBannerController(ad, mockBannerController)
        val mockChild = mockk<View>()
        every { mockBannerListener.onAdViewAdded(ad.placementName, mockChild) } just runs

        // When onViewAdded is called
        ad.onViewAdded(mockChild)

        // Then onAdViewAdded on the listener is triggered with correct parameters
        verify { mockBannerListener.onAdViewAdded(ad.placementName, mockChild) }
    }

    @Test
    fun `destroy sets heliumBannerAdListener to null`() {
        // Given an ad
        val ad = HeliumBannerAd(
            mockk(relaxed = true),
            "testPlacement",
            HeliumBannerSize.STANDARD,
            mockBannerListener
        )
        setBannerController(ad, mockBannerController)

        every { mockBannerController.destroy() } just runs

        // When destroy is called
        ad.destroy()

        // Then heliumBannerAdListener is set to null
        assertNull(ad.heliumBannerAdListener)
    }


    fun setBannerController(ad: HeliumBannerAd, mockBannerController: BannerController) {
        try {
            val field: Field = HeliumBannerAd::class.java.getDeclaredField("bannerController")
            field.isAccessible = true
            field.set(ad, mockBannerController)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

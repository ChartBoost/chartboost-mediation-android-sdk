/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk

import android.content.Context
import com.chartboost.heliumsdk.ad.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

open class HeliumKeywordsTest {
    private lateinit var heliumInterstitialAd: HeliumInterstitialAd
    private lateinit var heliumRewardedAd: HeliumRewardedAd
    private lateinit var heliumBannerAd: HeliumBannerAd

    @MockK
    private lateinit var mockedi12AdListener: HeliumFullscreenAdListener

    @MockK
    private lateinit var mockedRewardedAdListener: HeliumFullscreenAdListener

    @MockK
    private lateinit var mockedBannerAdListener: HeliumBannerAdListener

    @MockK
    private lateinit var mockedContext: Context

    @Before
    fun setUp() {
        // Create mocks
        mockedi12AdListener = mockk()
        mockedRewardedAdListener = mockk()
        mockedBannerAdListener = mockk()
        mockedContext = mockk()
        every { mockedContext.applicationContext } returns mockk()

        // Create Helium Ad Objects
        heliumInterstitialAd = HeliumInterstitialAd(mockedContext, "sample", mockedi12AdListener)
        heliumRewardedAd = HeliumRewardedAd(mockedContext, "sample", mockedRewardedAdListener)
        heliumBannerAd =
            HeliumBannerAd(
                mockedContext,
                "sample",
                HeliumBannerAd.HeliumBannerSize.MEDIUM,
                mockedBannerAdListener,
            )
    }

    @Test
    fun keywords_withDifferentAdFormats_shouldCreateDifferentKeywordClasses() {
        // Add a keyword to the Helium Interstitial Ad object only.
        heliumInterstitialAd.keywords["hello"] = "hi"

        // Assert that the Helium Interstitial Ad object only has keywords added and not the other objects.
        assertEquals(1, heliumInterstitialAd.keywords.get().size)
        assertEquals(0, heliumRewardedAd.keywords.get().size)
        assertEquals(0, heliumBannerAd.keywords.get().size)

        // Add keywords to the rest of the ad objects.
        heliumRewardedAd.keywords["hello"] = "hi"
        heliumBannerAd.keywords["banner"] = "hello"

        // Assert that all of the objects now have keywords.
        assertEquals(1, heliumInterstitialAd.keywords.get().size)
        assertEquals(1, heliumRewardedAd.keywords.get().size)
        assertEquals(1, heliumBannerAd.keywords.get().size)

        // Make sure that the keywords ad objects are not the same.
        assertNotEquals(heliumInterstitialAd.keywords, heliumRewardedAd.keywords)
        assertNotEquals(heliumInterstitialAd.keywords, heliumBannerAd.keywords)
    }

    @Test
    fun keywords_whenRemovingKeys_shouldReturnValueAndBeRemoved() {
        // Adding keywords.
        heliumInterstitialAd.keywords.apply {
            set("i12", "yes")
            set("12344", "ok")
            set("i12", "alright")
            set("Meta", "NotFacebook")
            set("Chartboost", "Rocks")
        }

        // There should be 4 elements added.
        assertEquals(4, heliumInterstitialAd.keywords.get().size)

        // Removing a keyword. Removed keyword should return value.
        assertEquals("NotFacebook", heliumInterstitialAd.keywords.remove("Meta"))

        // There should be 3 elements added.
        assertEquals(3, heliumInterstitialAd.keywords.get().size)

        // Meta keywords should no longer be in the list.
        heliumInterstitialAd.keywords.get().forEach {
            assertTrue(it.key != "Meta")
        }
    }
}

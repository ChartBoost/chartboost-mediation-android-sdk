/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.content.Context
import com.chartboost.chartboostmediationsdk.ad.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

open class ChartboostMediationKeywordsTest {
    private lateinit var chartboostMediationBannerAdView: ChartboostMediationBannerAdView

    @MockK
    private lateinit var mockedBannerAdListener: ChartboostMediationBannerAdViewListener

    @MockK
    private lateinit var mockedContext: Context

    @Before
    fun setUp() {
        // Create mocks
        mockedBannerAdListener = mockk()
        mockedContext = mockk()
        every { mockedContext.applicationContext } returns mockk()

        chartboostMediationBannerAdView =
            ChartboostMediationBannerAdView(
                mockedContext,
                "sample",
                ChartboostMediationBannerAdView.ChartboostMediationBannerSize.MEDIUM,
                mockedBannerAdListener,
            )
    }

    @Test
    fun keywords_withDifferentAdFormats_shouldCreateDifferentKeywordClasses() {
        // Assert that the Chartboost Mediation Interstitial Ad object only has keywords added and not the other objects.
        assertEquals(0, chartboostMediationBannerAdView.keywords.get().size)

        // Add keywords to the rest of the ad objects.
        chartboostMediationBannerAdView.keywords["banner"] = "hello"

        // Assert that all of the objects now have keywords.
        assertEquals(1, chartboostMediationBannerAdView.keywords.get().size)
    }
}

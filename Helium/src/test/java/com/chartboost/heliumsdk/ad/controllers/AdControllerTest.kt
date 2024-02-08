/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad.controllers

import com.chartboost.heliumsdk.controllers.AdController
import com.chartboost.heliumsdk.domain.Ad
import com.chartboost.heliumsdk.domain.AdFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdControllerTest {
    @Test
    fun `adTypeToAdFormat should convert BANNER to BANNER format`() {
        assertEquals(AdFormat.BANNER, AdController.adTypeToAdFormat(Ad.AdType.BANNER))
    }

    @Test
    fun `adTypeToAdFormat should convert ADAPTIVE_BANNER to ADAPTIVE_BANNER format`() {
        assertEquals(AdFormat.ADAPTIVE_BANNER, AdController.adTypeToAdFormat(Ad.AdType.ADAPTIVE_BANNER))
    }

    @Test
    fun `adTypeToAdFormat should convert INTERSTITIAL to INTERSTITIAL format`() {
        assertEquals(AdFormat.INTERSTITIAL, AdController.adTypeToAdFormat(Ad.AdType.INTERSTITIAL))
    }

    @Test
    fun `adTypeToAdFormat should convert REWARDED to REWARDED format`() {
        assertEquals(AdFormat.REWARDED, AdController.adTypeToAdFormat(Ad.AdType.REWARDED))
    }

    @Test
    fun `adTypeToAdFormat should convert REWARDED_INTERSTITIAL to REWARDED_INTERSTITIAL format`() {
        assertEquals(AdFormat.REWARDED_INTERSTITIAL, AdController.adTypeToAdFormat(Ad.AdType.REWARDED_INTERSTITIAL))
    }

    @Test
    fun `adTypeToAdFormat should throw IllegalArgumentException for unknown AdType`() {
        val unknownAdType = 9999 // assuming this value doesn't correspond to any known Ad.AdType
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                AdController.adTypeToAdFormat(unknownAdType)
            }
        assertEquals("Unknown AdType value: $unknownAdType", exception.message)
    }
}

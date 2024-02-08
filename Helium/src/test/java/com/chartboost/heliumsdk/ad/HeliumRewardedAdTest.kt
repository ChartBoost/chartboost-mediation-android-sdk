/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.content.Context
import com.chartboost.heliumsdk.domain.CachedAd
import io.mockk.*
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HeliumRewardedAdTest {
    lateinit var subject: HeliumRewardedAd
    lateinit var listener: HeliumFullscreenAdListener
    lateinit var cachedAd: CachedAd
    lateinit var customDataInCachedAdSlot: CapturingSlot<String>

    @Before
    fun setUp() {
        listener = mockk()
        cachedAd = mockk()
        customDataInCachedAdSlot = slot()
        every { cachedAd.customData = capture(customDataInCachedAdSlot) } just runs
        val context: Context = mockk()
        every { context.applicationContext } returns mockk()
        subject = HeliumRewardedAd(context, "placement", listener)
        HeliumFullscreenAd::class.java.getDeclaredField("cachedAd").apply {
            isAccessible = true
            set(subject, cachedAd)
        }
    }

    @Test
    fun `setCustomData with normal data should set the customData property and in the cachedAd`() {
        subject.customData = "abc"

        assertEquals("abc", subject.customData)
        assertEquals("abc", customDataInCachedAdSlot.captured)
    }

    @Test
    fun `setCustomData with too many characters should set the customData property to null and empty string in the cachedAd`() {
        subject.customData = generateRandomString(HeliumRewardedAd.Constants.MAX_CUSTOM_DATA_LENGTH + 1)

        assertEquals(null, subject.customData)
        assertEquals("", customDataInCachedAdSlot.captured)
    }

    private fun generateRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

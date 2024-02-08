/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.os.SystemClock
import io.mockk.every
import io.mockk.mockkStatic
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoadRateLimiterTest {
    companion object {
        private const val PLACEMENT = "placement"
        private const val DURATION = 2
    }

    private lateinit var subject: LoadRateLimiter

    @Before
    fun setUp() {
        subject = LoadRateLimiter()
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } returns 12345
    }

    @Test
    fun setLoadRateLimit_setsLoadRateLimitSecondsAndMillisUntilNextLoadIsAllowed() {
        subject.setLoadRateLimit(PLACEMENT, DURATION)

        assertEquals(DURATION, subject.getLoadRateLimitSeconds(PLACEMENT))
        assertEquals(DURATION * 1000L, subject.millisUntilNextLoadIsAllowed(PLACEMENT))
    }

    @Test
    fun setLoadRateLimit_withAdvancedClock_setsLoadRateLimitSecondsAndMillisUntilNextLoadIsAllowed() {
        subject.setLoadRateLimit(PLACEMENT, DURATION)

        every { SystemClock.uptimeMillis() } returns 13345

        assertEquals(DURATION, subject.getLoadRateLimitSeconds(PLACEMENT))
        assertEquals(1000L, subject.millisUntilNextLoadIsAllowed(PLACEMENT))
    }

    @Test
    fun setLoadRateLimit_withTimeElapsedGreaterThanLoadLimit_setsLoadRateLimitSecondsAndMillisUntilNextLoadIsAllowed() {
        subject.setLoadRateLimit(PLACEMENT, DURATION)

        every { SystemClock.uptimeMillis() } returns 999999

        assertEquals(DURATION, subject.getLoadRateLimitSeconds(PLACEMENT))
        assertEquals(0, subject.millisUntilNextLoadIsAllowed(PLACEMENT))
    }
}

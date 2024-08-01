/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class EnvironmentTest {
    @Before
    fun setUp() {
        mockkStatic("com.chartboost.chartboostmediationsdk.ChartboostMediationSdk")
        mockkStatic("com.chartboost.chartboostmediationsdk.utils.Environment")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.chartboost.chartboostmediationsdk.ChartboostMediationSdk")
        unmockkStatic("com.chartboost.chartboostmediationsdk.utils.Environment")
        unmockkAll()
        clearAllMocks()
    }

    @Test
    fun onEnvironment_getLanguage_should_returnLanguage() {
        assertEquals(Locale.getDefault().language, Environment.language)
    }

    @Test
    fun onEnvironment_getLanguage_withDifferentLanguage_should_returnLanguage() {
        Locale.setDefault(Locale("kotlin"))

        assertEquals("kotlin", Environment.language)

        Locale.setDefault(Locale("en"))

        assertEquals("en", Environment.language)
    }
}

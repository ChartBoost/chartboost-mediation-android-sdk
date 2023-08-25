/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.chartboost.heliumsdk.HeliumSdk
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
        mockkStatic("com.chartboost.heliumsdk.HeliumSdk")
        mockkStatic("com.chartboost.heliumsdk.utils.Environment")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.chartboost.heliumsdk.HeliumSdk")
        unmockkStatic("com.chartboost.heliumsdk.utils.Environment")
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

    @Test
    fun onEnvironment_getManufacturer_should_returnManufacturer() {
        assertEquals(Build.MANUFACTURER, Environment.manufacturer)
    }

    @Test
    fun onEnvironment_getModel_should_returnModel() {
        assertEquals(Build.MODEL, Environment.model)
    }

    @Test
    fun onEnvironment_getOS_should_returnOS() {
        assertEquals("Android", Environment.operatingSystem)
    }

    @Test
    fun onEnvironment_getOSV_should_returnOSV() {
        assertEquals(Build.VERSION.RELEASE, Environment.operatingSystemVersion)
    }
}

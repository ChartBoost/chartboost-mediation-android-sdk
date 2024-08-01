/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import android.content.Context
import android.content.SharedPreferences
import com.chartboost.chartboostmediationsdk.controllers.PartnerController
import com.chartboost.chartboostmediationsdk.controllers.PrivacyController
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChartboostMediationInternalTest {
    private var mockedContext: Context = mockk()

    private var mockedSharedPreferences: SharedPreferences = mockk()

    private var mockedPrivacyController: PrivacyController = mockk()

    private var mockedPartnerController: PartnerController = mockk()

    private var subject: ChartboostMediationInternal = ChartboostMediationInternal()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockedContext = mockk()
        mockedPrivacyController = mockk()
        mockedPartnerController = mockk()
        subject = ChartboostMediationInternal(mockedPartnerController)
        subject.privacyController = mockedPrivacyController

        every { mockedContext.getSharedPreferences(any(), any()) } returns mockedSharedPreferences
        every { mockedSharedPreferences.getString(any(), any()) } returns ""

        every { mockedPrivacyController.gdpr } returns null
        every { mockedPartnerController.setIsUserUnderage(any(), any()) } just Runs
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialize_whenAlreadyInitialized_shouldReturnFailure() =
        runTest {
            subject.initializationStatus = ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZED
            val result = subject.initialize(mockedContext, "appid", null)
            assertTrue(result.isSuccess)
        }
}

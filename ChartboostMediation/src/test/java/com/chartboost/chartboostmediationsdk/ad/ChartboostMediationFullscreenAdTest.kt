/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.app.Activity
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.controllers.AdController
import com.chartboost.chartboostmediationsdk.domain.CachedAd
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationAdException
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationError
import com.chartboost.chartboostmediationsdk.domain.Keywords
import com.chartboost.chartboostmediationsdk.utils.FullscreenAdShowingState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance

class ChartboostMediationFullscreenAdTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = StandardTestDispatcher()
    private val activity = mockk<Activity>()
    private val cachedAd = mockk<CachedAd>()
    private val listener = mockk<ChartboostMediationFullscreenAdListener>()
    private val request = mockk<ChartboostMediationFullscreenAdLoadRequest>()
    private val adController = mockk<AdController>()
    private val loadId = "test_load_id"
    private val winningBidInfo = mockk<Map<String, String>>()

    private lateinit var fullscreenAd: ChartboostMediationFullscreenAd

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { cachedAd.customData = any() } just Runs
        every { cachedAd.customData } returns ""

        fullscreenAd =
            ChartboostMediationFullscreenAd(
                cachedAd,
                listener,
                request,
                loadId,
                winningBidInfo,
                adController,
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show success`() =
        runTest {
            val mockFullscreenAdShowingState = mockk<FullscreenAdShowingState>()
            val chartboostMediationSdkCompanion = ChartboostMediationSdk::class.companionObjectInstance

            val chartboostMediationInternalField = ChartboostMediationSdk::class.java.getDeclaredField("chartboostMediationInternal")
            chartboostMediationInternalField.isAccessible = true
            val chartboostMediationInternal = chartboostMediationInternalField.get(chartboostMediationSdkCompanion)

            val fullscreenAdShowingStateField = chartboostMediationInternal.javaClass.getDeclaredField("fullscreenAdShowingState")
            fullscreenAdShowingStateField.isAccessible = true
            fullscreenAdShowingStateField.set(chartboostMediationInternal, mockFullscreenAdShowingState)

            val showResult = ChartboostMediationAdShowResult(JSONObject(), null)

            coEvery { adController.show(activity, cachedAd) } returns showResult
            coEvery { listener.onAdImpressionRecorded(any()) } just Runs
            coEvery { mockFullscreenAdShowingState.notifyFullscreenAdShown() } just Runs

            val result = fullscreenAd.show(activity)

            assertEquals(showResult, result)
            coVerify { adController.show(activity, cachedAd) }

            verify(exactly = 1) { listener.onAdImpressionRecorded(fullscreenAd) }
            verify(exactly = 1) { mockFullscreenAdShowingState.notifyFullscreenAdShown() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure ad not ready`() =
        runTest {
            fullscreenAd.cachedAd = null

            val result = fullscreenAd.show(activity)

            assertEquals(ChartboostMediationError.ShowError.AdNotReady, result.error)

            verify(exactly = 0) { listener.onAdImpressionRecorded(fullscreenAd) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure with exception`() =
        runTest {
            val exception =
                ChartboostMediationAdException(ChartboostMediationError.ShowError.Exception)

            coEvery { adController.show(activity, cachedAd) } throws exception

            val result = fullscreenAd.show(activity)

            assertEquals(ChartboostMediationError.ShowError.Exception, result.error)

            verify(exactly = 0) { listener.onAdImpressionRecorded(fullscreenAd) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure null adcontroller`() =
        runTest {
            fullscreenAd =
                ChartboostMediationFullscreenAd(
                    cachedAd,
                    listener,
                    request,
                    loadId,
                    winningBidInfo,
                    null,
                )

            val result = fullscreenAd.show(activity)

            assertEquals(ChartboostMediationError.ShowError.NotInitialized, result.error)

            verify(exactly = 0) { listener.onAdImpressionRecorded(fullscreenAd) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure show in progress`() =
        runTest {
            // Given
            val showResult = ChartboostMediationAdShowResult(JSONObject(), null)

            coEvery { adController.show(activity, cachedAd) } returns showResult
            coEvery { listener.onAdImpressionRecorded(any()) } just Runs

            fullscreenAd.show(activity)
            val result = fullscreenAd.show(activity)

            assertEquals(ChartboostMediationError.ShowError.ShowInProgress, result.error)

            verify(exactly = 1) { listener.onAdImpressionRecorded(fullscreenAd) }
        }

    @Test
    fun invalidate() {
        fullscreenAd.invalidate()

        val currentListener = fullscreenAd.listener
        val currentCachedAd = fullscreenAd.cachedAd

        assertNull(currentListener)
        assertNull(currentCachedAd)
    }

    @Test
    fun `ad load result cached ad is set`() {
        val ad =
            ChartboostMediationFullscreenAd(
                cachedAd = null,
                listener = null,
                request =
                    ChartboostMediationFullscreenAdLoadRequest(
                        placement = "test_placement",
                        keywords = Keywords(),
                    ),
                loadId = "test_load_id",
                winningBidInfo = mapOf(),
                adController = adController,
            )

        val loadResult =
            ChartboostMediationFullscreenAdLoadResult(
                ad = ad,
                loadId = "",
                metrics = JSONObject(),
                error = null,
            )

        assertEquals(ad, loadResult.ad)
    }

    @Test
    fun `ad load result request id is set`() {
        val loadId = UUID.randomUUID().toString()
        val loadResult =
            ChartboostMediationFullscreenAdLoadResult(
                ad = null,
                loadId = loadId,
                metrics = JSONObject(),
                error = null,
            )

        assertEquals(loadId, loadResult.loadId)
    }

    @Test
    fun `ad load result metrics is set`() {
        val metrics =
            JSONObject().apply {
                put("auction_id", "ef28e4a2124648ca82518716d6b776ef9266649c")
                put(
                    "metrics",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("network_type", "mediation")
                                put("line_item_id", "68d83ae9-3c9e-4737-9371-6c09a99f6e89")
                                put("partner_placement", "ca-app-pub-6548817822928201/6752711386")
                                put("partner", "admob")
                                put("start", 1679599980090)
                                put("end", 1679599982425)
                                put("duration", 2336)
                                put("is_success", true)
                            },
                        )
                    },
                )
            }

        val result =
            ChartboostMediationFullscreenAdLoadResult(
                ad = null,
                loadId = "",
                metrics = metrics,
                error = null,
            )

        assertEquals(metrics, result.metrics)
    }

    @Test
    fun `ad load result error is set`() {
        val error = ChartboostMediationError.OtherError.Unknown
        val result =
            ChartboostMediationFullscreenAdLoadResult(
                ad = null,
                loadId = "",
                metrics = JSONObject(),
                error = error,
            )

        assertEquals(error, result.error)
    }
}

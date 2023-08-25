/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.ad

import android.content.Context
import com.chartboost.heliumsdk.controllers.AdController
import com.chartboost.heliumsdk.domain.CachedAd
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.heliumsdk.domain.Keywords
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class ChartboostMediationFullscreenAdTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = StandardTestDispatcher()
    private val context = mockk<Context>()
    private val cachedAd = mockk<CachedAd>()
    private val listener = mockk<ChartboostMediationFullscreenAdListener>()
    private val request = mockk<ChartboostMediationAdLoadRequest>()
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

        fullscreenAd = ChartboostMediationFullscreenAd(
            cachedAd,
            listener,
            request,
            loadId,
            winningBidInfo,
            adController
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
    fun `show success`() = runTest {
        val showResult = ChartboostMediationAdShowResult(JSONObject(), null)

        coEvery { adController.show(context, cachedAd) } returns showResult

        val result = fullscreenAd.show(context)

        assertEquals(showResult, result)
        coVerify { adController.show(context, cachedAd) }

        verify(exactly = 1) { listener.onAdImpressionRecorded(fullscreenAd) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure ad not ready`() = runTest {
        fullscreenAd.cachedAd = null

        val result = fullscreenAd.show(context)

        assertEquals(ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_READY, result.error)

        verify(exactly = 0) { listener.onAdImpressionRecorded(fullscreenAd) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure with exception`() = runTest {
        val exception =
            ChartboostMediationAdException(ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION)

        coEvery { adController.show(context, cachedAd) } throws exception

        val result = fullscreenAd.show(context)

        assertEquals(ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION, result.error)

        verify(exactly = 0) { listener.onAdImpressionRecorded(fullscreenAd) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure null adcontroller`() = runTest {
        fullscreenAd = ChartboostMediationFullscreenAd(
            cachedAd,
            listener,
            request,
            loadId,
            winningBidInfo,
            null
        )

        val result = fullscreenAd.show(context)

        assertEquals(ChartboostMediationError.CM_SHOW_FAILURE_NOT_INITIALIZED, result.error)

        verify(exactly = 0) { listener.onAdImpressionRecorded(fullscreenAd) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `show failure show in progress`() = runTest {
        // Given
        val showResult = ChartboostMediationAdShowResult(JSONObject(), null)

        coEvery { adController.show(context, cachedAd) } returns showResult

        fullscreenAd.show(context)
        val result = fullscreenAd.show(context)

        assertEquals(ChartboostMediationError.CM_SHOW_FAILURE_SHOW_IN_PROGRESS, result.error)

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
        val ad = ChartboostMediationFullscreenAd(
            cachedAd = null,
            listener = null,
            request = ChartboostMediationAdLoadRequest(
                placementName = "test_placement",
                keywords = Keywords(),
            ),
            loadId = "test_load_id",
            winningBidInfo = mapOf(),
            adController = adController
        )

        val loadResult = ChartboostMediationFullscreenAdLoadResult(
            ad = ad,
            loadId = "",
            metrics = JSONObject(),
            error = null
        )

        assertEquals(ad, loadResult.ad)
    }

    @Test
    fun `ad load result request id is set`() {
        val loadId = UUID.randomUUID().toString()
        val loadResult = ChartboostMediationFullscreenAdLoadResult(
            ad = null,
            loadId = loadId,
            metrics = JSONObject(),
            error = null
        )

        assertEquals(loadId, loadResult.loadId)
    }

    @Test
    fun `ad load result metrics is set`() {
        val metrics = JSONObject().apply {
            put("auction_id", "ef28e4a2124648ca82518716d6b776ef9266649c")
            put("metrics", JSONArray().apply {
                put(JSONObject().apply {
                    put("network_type", "mediation")
                    put("line_item_id", "68d83ae9-3c9e-4737-9371-6c09a99f6e89")
                    put("partner_placement", "ca-app-pub-6548817822928201/6752711386")
                    put("partner", "admob")
                    put("start", 1679599980090)
                    put("end", 1679599982425)
                    put("duration", 2336)
                    put("is_success", true)
                })
            })
        }

        val result = ChartboostMediationFullscreenAdLoadResult(
            ad = null,
            loadId = "",
            metrics = metrics,
            error = null
        )

        assertEquals(metrics, result.metrics)
    }

    @Test
    fun `ad load result error is set`() {
        val error = ChartboostMediationError.CM_UNKNOWN_ERROR
        val result = ChartboostMediationFullscreenAdLoadResult(
            ad = null,
            loadId = "",
            metrics = JSONObject(),
            error = error
        )

        assertEquals(error, result.error)
    }
}

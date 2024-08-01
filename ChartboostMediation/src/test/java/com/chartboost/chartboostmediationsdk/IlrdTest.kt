/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk

import io.mockk.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
open class IlrdTest {
    private lateinit var subject: Ilrd
    private lateinit var testJson: JSONObject
    private lateinit var mockObserver: ChartboostMediationIlrdObserver
    private lateinit var impressionDataSlot: CapturingSlot<ChartboostMediationImpressionData>

    @Before
    fun setUp() {
        subject = Ilrd()
        testJson = JSONObject()
        testJson.put("test1", "value1")
        testJson.put("test2", "value2")
        mockObserver = mockk()
        impressionDataSlot = slot()
        every { mockObserver.onImpression(capture(impressionDataSlot)) } returns Unit
        subject.subscribe(mockObserver)
        ShadowLooper.runUiThreadTasks()
    }

    @After
    fun tearDown() {
        subject.unsubscribe(mockObserver)
    }

    @Test
    fun onIlrdReceived_shouldFireOnImpressionForAllObservers() {
        val mockObserver2 = mockk<ChartboostMediationIlrdObserver>()
        val impressionDataSlot2 = slot<ChartboostMediationImpressionData>()
        every { mockObserver2.onImpression(capture(impressionDataSlot2)) } returns Unit
        subject.subscribe(mockObserver2)

        subject.onIlrdReceived("banner", testJson)

        ShadowLooper.runUiThreadTasks()
        assertEquals("banner", impressionDataSlot.captured.placementId)
        assertEquals(testJson, impressionDataSlot.captured.ilrdInfo)
        assertEquals("banner", impressionDataSlot2.captured.placementId)
        assertEquals(testJson, impressionDataSlot2.captured.ilrdInfo)
        subject.unsubscribe(mockObserver2)
    }

    @Test
    fun onIlrdReceived_withUnsubscribe_shouldFireOnImpressionForOnlyValidObservers() {
        val mockObserver2 = mockk<ChartboostMediationIlrdObserver>()
        subject.subscribe(mockObserver2)
        subject.unsubscribe(mockObserver2)

        subject.onIlrdReceived("banner", testJson)

        ShadowLooper.runUiThreadTasks()
        assertEquals("banner", impressionDataSlot.captured.placementId)
        assertEquals(testJson, impressionDataSlot.captured.ilrdInfo)
        verify(exactly = 0) { mockObserver2.onImpression(allAny()) }
    }
}

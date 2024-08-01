/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.ad

import android.content.Context
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChartboostMediationFullscreenAdQueueTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = StandardTestDispatcher()

    @MockK private lateinit var mockedContext: Context

    private lateinit var fullscreenAdQueue: ChartboostMediationFullscreenAdQueue

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)
        mockkObject(ChartboostMediationSdk.chartboostMediationInternal)
        every {
            ChartboostMediationSdk.chartboostMediationInternal.initializationStatus
        } returns ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZED
        fullscreenAdQueue = ChartboostMediationFullscreenAdQueueManager.queue(mockedContext, "test_placement_1")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun `start queue should not run before SDK has initialized`() {
        every {
            ChartboostMediationSdk.chartboostMediationInternal.initializationStatus
        } returns ChartboostMediationSdk.ChartboostMediationInitializationStatus.IDLE

        fullscreenAdQueue.start()

        assertTrue(fullscreenAdQueue.isRunning)
        assertTrue(fullscreenAdQueue.isPaused)

        every {
            ChartboostMediationSdk.chartboostMediationInternal.initializationStatus
        } returns ChartboostMediationSdk.ChartboostMediationInitializationStatus.INITIALIZED

        fullscreenAdQueue.start()

        assertTrue(fullscreenAdQueue.isRunning)
        assertFalse(fullscreenAdQueue.isPaused)
    }

    @Test
    fun `start queue multiple times should already be running `() {
        assertFalse(fullscreenAdQueue.isRunning)
        assertFalse(fullscreenAdQueue.isPaused)

        fullscreenAdQueue.start()

        assertTrue(fullscreenAdQueue.isRunning)
        assertFalse(fullscreenAdQueue.isPaused)

        fullscreenAdQueue.start()

        assertTrue(fullscreenAdQueue.isRunning)
        assertFalse(fullscreenAdQueue.isPaused)
    }

    @Test
    fun `start queue should be running`() {
        fullscreenAdQueue.start()

        assertTrue(fullscreenAdQueue.isRunning)
    }

    @Test
    fun `stop queue after start should no longer be running`() {
        fullscreenAdQueue.start()

        assertTrue(fullscreenAdQueue.isRunning)

        fullscreenAdQueue.stop()

        assertFalse(fullscreenAdQueue.isRunning)
    }

    @Test
    fun `queues should be singleton per placement`() {
        fullscreenAdQueue = ChartboostMediationFullscreenAdQueueManager.queue(mockedContext, "test_placement_1")

        val queueA = ChartboostMediationFullscreenAdQueueManager.queue(mockedContext, "test_placement_A")
        val queueB = ChartboostMediationFullscreenAdQueueManager.queue(mockedContext, "test_placement_B")

        // Should return the queue that was already created for queue A.
        val queueC = ChartboostMediationFullscreenAdQueueManager.queue(mockedContext, "test_placement_A")

        // Should return the queue created during setUp.
        val queueD = ChartboostMediationFullscreenAdQueueManager.queue(mockedContext, "test_placement_1")

        assertNotEquals(queueA, queueB)
        assertNotEquals(queueB, queueC)
        assertEquals(queueA, queueC)
        assertEquals(fullscreenAdQueue, queueD)
    }
}

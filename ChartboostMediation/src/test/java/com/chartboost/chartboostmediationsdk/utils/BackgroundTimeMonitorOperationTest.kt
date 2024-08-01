/*
 * Copyright 2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class BackgroundTimeMonitorOperationTest : LifecycleOwner {
    @Test
    fun uneventful_monitoring() {
        val monitor = BackgroundTimeMonitor()
        val operation = monitor.startMonitoringOperation()
        assertEquals(0, operation.backgroundTimeUntilNow())

        Thread.sleep(Random.nextLong(100, 1000))

        assertEquals(0, operation.backgroundTimeUntilNow())
    }

    @Test
    fun eventful_monitoring() {
        val operation = BackgroundTimeMonitorOperation()
        Thread.sleep(Random.nextLong(100, 1000))

        assertEquals(0, operation.backgroundTimeUntilNow())
        operation.onStop(this)
        Thread.sleep(Random.nextLong(100, 1000))
        val backgroundTime1 = operation.backgroundTimeUntilNow()
        assertTrue(backgroundTime1 >= 100)
        assertTrue(backgroundTime1 <= 1000)

        operation.onStart(this)
        Thread.sleep(Random.nextLong(100, 1000))
        val backgroundTime2 = operation.backgroundTimeUntilNow()
        assertEquals(backgroundTime1, backgroundTime2)

        operation.onStop(this)
        Thread.sleep(Random.nextLong(100, 1000))
        operation.onStart(this)
        val backgroundTime3 = operation.backgroundTimeUntilNow()
        assertTrue(backgroundTime3 >= 200)
        assertTrue(backgroundTime3 <= 2000)

        operation.onStop(this)
        Thread.sleep(Random.nextLong(100, 1000))
        operation.onStart(this)
        Thread.sleep(Random.nextLong(100, 1000))
        val backgroundTime4 = operation.backgroundTimeUntilNow()
        assertTrue(backgroundTime4 >= 300)
        assertTrue(backgroundTime4 <= 3000)
    }

    override val lifecycle: Lifecycle
        get() = ProcessLifecycleOwner.get().lifecycle
}

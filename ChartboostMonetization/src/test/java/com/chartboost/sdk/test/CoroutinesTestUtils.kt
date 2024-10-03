package com.chartboost.sdk.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

@ExperimentalCoroutinesApi
fun <T : TestDispatcher> T.advanceTimeBy(millis: Long): T =
    apply {
        scheduler.advanceTimeBy(millis)
    }

@ExperimentalCoroutinesApi
fun <T : TestDispatcher> T.advanceUntilIdle(): T =
    apply {
        scheduler.advanceUntilIdle()
    }

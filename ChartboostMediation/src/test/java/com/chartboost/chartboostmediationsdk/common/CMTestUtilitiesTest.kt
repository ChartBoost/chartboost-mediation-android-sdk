/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.common

import com.chartboost.chartboostmediationsdk.ChartboostMediationInternal
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import kotlinx.coroutines.CoroutineDispatcher
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object CMTestUtilitiesTest {
    /**
     * Currently using the same instance for testing. Not a good idea to test singletons...
     * But, this allows to reset the ChartboostMediationSDK singleton for some tests.
     */
    fun resetChartboostMediationInternal(ioDispatcher: CoroutineDispatcher) {
        val className = ChartboostMediationSdk::class.java
        val chartboostMediationInternal = className.getDeclaredField("chartboostMediationInternal")
        chartboostMediationInternal.isAccessible = true
        setFinalStatic(chartboostMediationInternal, ChartboostMediationInternal(ioDispatcher = ioDispatcher))
    }

    @Throws(Exception::class)
    fun setFinalStatic(
        field: Field,
        newValue: Any?,
    ) {
        field.isAccessible = true
        val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
        modifiersField.setAccessible(true)
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
        field.set(null, newValue)
    }
}

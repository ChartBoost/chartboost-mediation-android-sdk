/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import android.content.Context
import kotlin.math.roundToInt

/**
 * @suppress
 */
object Dips {
    fun pixelsToIntDips(
        length: Int,
        context: Context,
    ): Int {
        getDensity(context).let {
            return if (it != 0f) {
                (length / it).roundToInt()
            } else {
                0
            }
        }
    }

    fun dipsToPixelsInt(
        length: Int,
        context: Context,
    ): Int = (length * getDensity(context)).roundToInt()

    private fun getDensity(context: Context) = context.resources.displayMetrics.density
}

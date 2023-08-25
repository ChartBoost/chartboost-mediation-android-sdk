/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

/**
 * Namespaced exception that wraps a particular [ChartboostMediationError]
 */
class ChartboostMediationAdException(val chartboostMediationError: ChartboostMediationError) : Exception() {
    override val message: String
        get() = chartboostMediationError.message

    override fun toString(): String {
        return chartboostMediationError.toString()
    }
}

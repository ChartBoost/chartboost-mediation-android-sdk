package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.Model.ImpressionState

interface ImpressionStateInterface {
    fun getImpressionState(): ImpressionState

    fun setImpressionState(newState: ImpressionState)
}

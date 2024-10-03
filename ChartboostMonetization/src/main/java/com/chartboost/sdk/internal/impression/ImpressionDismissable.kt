package com.chartboost.sdk.internal.impression

import com.chartboost.sdk.internal.Model.ImpressionState

interface ImpressionDismissable {
    fun onClose(state: ImpressionState)

    fun callImpressionDismissCallback()

    fun setClosed(close: Boolean)
}

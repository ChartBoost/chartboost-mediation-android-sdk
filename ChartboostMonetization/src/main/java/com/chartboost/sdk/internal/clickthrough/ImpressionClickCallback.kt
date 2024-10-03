package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.Model.CBError

internal interface ImpressionClickCallback {
    fun callDismissAfterClick()

    fun setImpressionClick(click: Boolean)

    fun callImpressionClickFailureCallback(
        url: String?,
        error: CBError.Click,
    )

    fun callImpressionClickSuccessCallback()

    fun setImpressionClosed(close: Boolean)
}

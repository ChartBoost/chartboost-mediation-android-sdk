package com.chartboost.sdk.internal.clickthrough

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.ImpressionState

interface ImpressionClickable {
    var click: Boolean

    fun submitClickRequest(
        location: String,
        videoPosition: Float?,
        videoDuration: Float?,
    )

    fun onOpenURL(cbUrl: CBUrl)

    fun onOpenNonClickURL(cbUrl: CBUrl)

    fun clickTriggeredBeforeLoadFinished(cbUrl: CBUrl)

    fun onClick(
        shouldDismiss: Boolean?,
        impressionState: ImpressionState,
    ): Boolean

    fun callImpressionClickSuccessCallback()

    fun callImpressionClickFailureCallback(
        url: String?,
        error: CBError.Click,
    )
}

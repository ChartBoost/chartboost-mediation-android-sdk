package com.chartboost.sdk.internal.AdUnitManager.render

import android.content.Context
import com.chartboost.sdk.internal.AdUnitManager.data.AppRequest
import com.chartboost.sdk.internal.Model.CBError

internal interface AdUnitRendererImpressionCallback {
    fun onImpressionReadyToBeDisplayed()

    fun onImpressionCloseTriggered(appRequest: AppRequest)

    fun onImpressionClicked(impressionId: String)

    fun onImpressionClickedFailed(
        impressionId: String,
        url: String?,
        error: CBError.Click,
    )

    fun onImpressionRewarded(
        impressionId: String?,
        reward: Int,
    )

    fun onImpressionDismissed(impressionId: String?)

    fun onImpressionShownFully(appRequest: AppRequest)

    fun onImpressionError(
        appRequest: AppRequest,
        error: CBError.Impression,
    )

    fun onImpressionViewCreated(context: Context)

    fun closeActivity()

    fun applyActivityOrientation(
        forceOrientation: Int,
        allowOrientationChange: Boolean,
    )

    fun restoreOriginalOrientation()
}

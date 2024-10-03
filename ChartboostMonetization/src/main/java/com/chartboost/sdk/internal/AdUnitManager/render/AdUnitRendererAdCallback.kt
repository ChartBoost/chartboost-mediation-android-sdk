package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.internal.Model.CBError

interface AdUnitRendererAdCallback {
    fun onReward(
        impressionId: String?,
        reward: Int,
    )

    fun onRequestedToShow(impressionId: String?)

    fun onImpressionSuccess(impressionId: String?)

    fun onShowSuccess(impressionId: String?)

    fun onShowFailure(
        impressionId: String?,
        error: CBError.Impression,
    )

    fun onImpressionClicked(impressionId: String?)

    fun onImpressionClickedFailed(
        impressionId: String?,
        url: String?,
        error: CBError.Click,
    )

    fun onImpressionDismissed(impressionId: String?)
}

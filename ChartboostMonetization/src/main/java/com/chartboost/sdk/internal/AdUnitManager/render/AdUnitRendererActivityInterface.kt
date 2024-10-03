package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.view.CBImpressionActivity

internal interface AdUnitRendererActivityInterface {
    fun onActivityIsReadyToDisplay(activity: CBImpressionActivity)

    fun impressionOnStart()

    fun impressionOnResume()

    fun impressionOnPause()

    fun impressionOnDestroy()

    fun onConfigurationChange()

    fun failure(error: CBError.Impression)

    fun onBackPressed(): Boolean
}

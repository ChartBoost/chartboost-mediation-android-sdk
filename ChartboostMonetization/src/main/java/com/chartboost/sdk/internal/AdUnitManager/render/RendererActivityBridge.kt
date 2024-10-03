package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.internal.Model.CBError.Impression
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.view.CBImpressionActivity

internal interface RendererActivityBridge {
    fun setActivityRendererInterface(
        activityInterface: ActivityRendererInterface,
        activity: CBImpressionActivity,
    )

    fun displayViewOnActivity(viewBase: ViewBase)

    fun startActivity(adUnitRendererActivityInterface: AdUnitRendererActivityInterface)

    fun onStart()

    fun onResume()

    fun onPause()

    fun onDestroy()

    fun onConfigurationChange()

    fun failure(error: Impression)

    fun onBackPressed(): Boolean

    fun finishActivity()

    fun applyOrientationProperties(
        forceOrientation: Int,
        allowOrientationChange: Boolean,
    )

    fun restoreOriginalOrientation()
}

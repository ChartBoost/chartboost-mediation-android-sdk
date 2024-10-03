package com.chartboost.sdk.internal.AdUnitManager.render

import com.chartboost.sdk.internal.View.ViewBase

interface ActivityRendererInterface {
    fun displayViewOnActivity(viewBase: ViewBase)

    fun finishActivity()

    fun applyOrientationProperties(
        forceOrientation: Int,
        allowOrientationChange: Boolean,
    )

    fun restoreOriginalOrientation()
}

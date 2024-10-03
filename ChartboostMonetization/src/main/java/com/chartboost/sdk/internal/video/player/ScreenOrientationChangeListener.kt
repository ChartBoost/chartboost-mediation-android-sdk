package com.chartboost.sdk.internal.video.player

fun interface ScreenOrientationChangeListener {
    fun onScreenOrientationChange(
        width: Int,
        height: Int,
    )
}

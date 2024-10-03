package com.chartboost.sdk.internal.video.player

import android.view.Gravity
import android.view.SurfaceView
import android.widget.FrameLayout

/**
 * Adjusts layout aspect ratio for portrait and horizontal mode, centers the video
 */
internal fun SurfaceView?.handleAspectRatio(
    videoWidth: Int,
    videoHeight: Int,
    surfaceWidth: Int,
    surfaceHeight: Int,
) {
    this?.run {
        val ratioWidth = surfaceWidth / videoWidth.toFloat()
        val ratioHeight = surfaceHeight / videoHeight.toFloat()
        val aspectRatio = videoWidth / videoHeight.toFloat()
        layoutParams =
            (layoutParams as? FrameLayout.LayoutParams)?.also { layoutParams ->
                if (ratioWidth > ratioHeight) {
                    layoutParams.width = (surfaceHeight * aspectRatio).toInt()
                    layoutParams.height = surfaceHeight
                } else {
                    layoutParams.width = surfaceWidth
                    layoutParams.height = (surfaceWidth / aspectRatio).toInt()
                }
                layoutParams.gravity = Gravity.CENTER
            }
    }
}

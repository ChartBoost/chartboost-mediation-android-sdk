package com.chartboost.sdk.internal.Libraries

import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import com.chartboost.sdk.internal.logging.Logger

internal class DisplayMeasurement(
    private val windowManager: WindowManager,
    private val displayMetrics: DisplayMetrics,
    private val androidVersion: () -> Int = { Build.VERSION.SDK_INT },
    private val realDisplayMetrics: DisplayMetrics = DisplayMetrics(),
) {
    val displayMetricsDensity: Float = displayMetrics.density
    val displayMetricsDensityDpi: Int = displayMetrics.densityDpi

    fun getDeviceSize(): DisplaySize {
        return try {
            if (androidVersion() >= Build.VERSION_CODES.R) {
                deviceSizeFromWindowMetrics(windowManager)
            } else {
                DisplaySize(displayMetrics.widthPixels, displayMetrics.heightPixels)
            }
        } catch (e: Exception) {
            Logger.e("Cannot create device size", e)
            DisplaySize(0, 0)
        }
    }

    fun getSize(): DisplaySize {
        return try {
            if (androidVersion() >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.let {
                    DisplaySize(it.width(), it.height())
                }
            } else {
                realDisplayMetrics.setTo(displayMetrics)
                windowManager.defaultDisplay?.getRealMetrics(realDisplayMetrics)
                DisplaySize(realDisplayMetrics.widthPixels, realDisplayMetrics.heightPixels)
            }
        } catch (e: Exception) {
            Logger.e("Cannot create size", e)
            DisplaySize(0, 0)
        }
    }

    private fun deviceSizeFromWindowMetrics(windowManager: WindowManager): DisplaySize {
        val metrics: WindowMetrics = windowManager.currentWindowMetrics

        // Gets all excluding insets
        val windowInsets = metrics.windowInsets
        val insets: Insets =
            windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars()
                    or WindowInsets.Type.displayCutout(),
            )

        val insetsWidth: Int = insets.right + insets.left
        val insetsHeight: Int = insets.top + insets.bottom

        // Legacy size that Display#getSize reports
        val bounds = metrics.bounds
        return DisplaySize(
            bounds.width() - insetsWidth,
            bounds.height() - insetsHeight,
        )
    }
}

data class DisplaySize(val width: Int, val height: Int)

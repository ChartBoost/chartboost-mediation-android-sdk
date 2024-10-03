package com.chartboost.sdk.internal.View

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Libraries.Orientation
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.logging.Logger

internal fun Context?.isScreenPortrait(displayMeasurement: DisplayMeasurement): Boolean {
    val orientation = getOrientation(displayMeasurement)
    return orientation == Orientation.PORTRAIT ||
        orientation == Orientation.PORTRAIT_REVERSE ||
        orientation == Orientation.PORTRAIT_LEFT ||
        orientation == Orientation.PORTRAIT_RIGHT
}

fun Activity?.unlockOrientation(sdkConfig: SdkConfiguration?) {
    if (this == null || shouldSkipOrientationChange()) return
    if (sdkConfig?.isWebviewEnabled == true && sdkConfig.isWebviewLockOrientation) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

@SuppressLint("SourceLockedOrientationActivity")
internal fun Activity?.lockOrientation(
    sdkConfig: SdkConfiguration?,
    displayMeasurement: DisplayMeasurement,
) {
    if (this == null || shouldSkipOrientationChange()) return

    if (sdkConfig?.isWebviewEnabled == true && sdkConfig.isWebviewLockOrientation) {
        // Locking the orientation
        getOrientation(displayMeasurement).let {
            requestedOrientation =
                when (it) {
                    Orientation.PORTRAIT, Orientation.PORTRAIT_RIGHT -> {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }

                    Orientation.PORTRAIT_REVERSE, Orientation.PORTRAIT_LEFT -> {
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    }

                    Orientation.LANDSCAPE, Orientation.LANDSCAPE_LEFT -> {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }

                    else -> {
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }
                }
        }
    }
}

/**
 * MO-3243: Orientation change should be skipped for the Android O devices running CB-Sdk with
 * targetSdkVersion 27 or more and activity theme includes translucent background
 */
fun Activity?.shouldSkipOrientationChange(): Boolean =
    this?.let { activity ->
        Build.VERSION.SDK_INT == Build.VERSION_CODES.O &&
            activity.applicationInfo.targetSdkVersion > Build.VERSION_CODES.O &&
            // On Android O, only opaque [alpha = 255 (0xFF)] theme allowed to change the orientation
            activity.window?.decorView?.background?.alpha != 0xFF
    } ?: true

private fun Context?.getOrientation(displayMeasurement: DisplayMeasurement): Orientation {
    if (this == null) {
        return Orientation.PORTRAIT
    }

    try {
        val deviceSize = displayMeasurement.getDeviceSize()
        val rotation = getRotationFromWindow(this)
        val isPortrait =
            if (deviceSize.width == deviceSize.height) {
                // if the screen is square but its hardware returned a different answer
                // use the hardware's answer, it probably is how the device is naturally held
                val defOrientation = resources.configuration.orientation
                defOrientation != Configuration.ORIENTATION_LANDSCAPE
            } else {
                deviceSize.width < deviceSize.height
            }

        // figure out the natural orientation of this device
        val naturalOrientationIsPortrait =
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                isPortrait
            } else {
                !isPortrait
            }

        return if (naturalOrientationIsPortrait) {
            when (rotation) {
                Surface.ROTATION_90 -> Orientation.LANDSCAPE_LEFT
                Surface.ROTATION_180 -> Orientation.PORTRAIT_REVERSE
                Surface.ROTATION_270 -> Orientation.LANDSCAPE_RIGHT
                Surface.ROTATION_0 -> Orientation.PORTRAIT
                else -> Orientation.PORTRAIT
            }
        } else {
            when (rotation) {
                Surface.ROTATION_90 -> Orientation.PORTRAIT_LEFT
                Surface.ROTATION_180 -> Orientation.LANDSCAPE_REVERSE
                Surface.ROTATION_270 -> Orientation.PORTRAIT_RIGHT
                Surface.ROTATION_0 -> Orientation.LANDSCAPE
                else -> Orientation.LANDSCAPE
            }
        }
    } catch (e: Exception) {
        Logger.e("Cannot getOrientation", e)
        return Orientation.LANDSCAPE
    }
}

/**
 * Deprecated but there is no other way to retrieve rotation without Activity and we request
 * this value before Ad is displayed
 */
private fun getRotationFromWindow(context: Context): Int =
    (context.getSystemService(WINDOW_SERVICE) as WindowManager?)?.defaultDisplay?.rotation ?: 0

internal fun Context?.getOrientationAsString(displayMeasurement: DisplayMeasurement): String {
    return when (getOrientation(displayMeasurement)) {
        Orientation.LANDSCAPE,
        Orientation.LANDSCAPE_REVERSE,
        Orientation.LANDSCAPE_LEFT,
        Orientation.LANDSCAPE_RIGHT,
        -> "landscape"

        Orientation.PORTRAIT,
        Orientation.PORTRAIT_REVERSE,
        Orientation.PORTRAIT_LEFT,
        Orientation.PORTRAIT_RIGHT,
        -> "portrait"
    }
}

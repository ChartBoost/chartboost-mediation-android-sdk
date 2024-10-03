package com.chartboost.sdk.view

import android.content.pm.ActivityInfo
import com.chartboost.sdk.internal.AdUnitManager.render.ActivityRendererInterface
import com.chartboost.sdk.internal.AdUnitManager.render.RendererActivityBridge
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Model.CBError
import com.chartboost.sdk.internal.Model.SdkConfiguration
import com.chartboost.sdk.internal.View.ViewBase
import com.chartboost.sdk.internal.View.lockOrientation
import com.chartboost.sdk.internal.View.shouldSkipOrientationChange
import com.chartboost.sdk.internal.View.unlockOrientation
import com.chartboost.sdk.internal.logging.Logger

internal class ImpressionActivityPresenter(
    private val view: ImpressionActivityContract.ImpressionActivityView,
    private val rendererActivityBridge: RendererActivityBridge,
    private val sdkConfiguration: SdkConfiguration,
    private val displayMeasurement: DisplayMeasurement,
) : ImpressionActivityContract.ImpressionActivityPresenter,
    ActivityRendererInterface {
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    override fun displayViewOnActivity(viewBase: ViewBase) {
        view.attachViewToActivity(viewBase)
    }

    override fun finishActivity() {
        view.finishActivity()
    }

    override fun onCreate() {
        rendererActivityBridge.setActivityRendererInterface(this, view.getActivity())
        view.setFullscreen()
        saveOriginalOrientation()
    }

    override fun onStart() {
        try {
            rendererActivityBridge.onStart()
        } catch (e: Exception) {
            Logger.d("Cannot perform onResume", e)
        }
    }

    override fun onResume() {
        try {
            rendererActivityBridge.setActivityRendererInterface(this, view.getActivity())
        } catch (e: Exception) {
            Logger.d("Cannot setActivityRendererInterface", e)
        }

        try {
            rendererActivityBridge.onResume()
        } catch (e: Exception) {
            Logger.d("Cannot perform onResume", e)
        }

        view.setFullscreen()
        try {
            view.getActivity().lockOrientation(
                sdkConfiguration,
                displayMeasurement,
            )
        } catch (e: Exception) {
            Logger.d("Cannot lock the orientation in activity", e)
        }
    }

    override fun onPause() {
        try {
            rendererActivityBridge.onPause()
        } catch (e: Exception) {
            Logger.d("Cannot perform onPause", e)
        }

        try {
            view.getActivity().unlockOrientation(sdkConfiguration)
        } catch (e: java.lang.Exception) {
            Logger.d("Cannot lock the orientation in activity", e)
        }
    }

    override fun onDestroy() {
        try {
            rendererActivityBridge.onDestroy()
        } catch (e: Exception) {
            Logger.d("Cannot perform onStop", e)
        }
    }

    override fun onConfigurationChange() {
        try {
            rendererActivityBridge.onConfigurationChange()
        } catch (e: Exception) {
            Logger.d("Cannot perform onStop", e)
        }
    }

    override fun onViewAttached() {
        try {
            if (!view.isActivityHardwareAccelerated()) {
                Logger.e(
                    "The activity passed down is not hardware accelerated, so Chartboost cannot show ads",
                )
                rendererActivityBridge.failure(CBError.Impression.HARDWARE_ACCELERATION_DISABLED)
                view.finishActivity()
            }
        } catch (e: Exception) {
            Logger.e("onAttachedToWindow", e)
        }
    }

    override fun onBackPressed(): Boolean {
        try {
            return rendererActivityBridge.onBackPressed()
        } catch (e: Exception) {
            Logger.e("onBackPressed", e)
        }
        return false
    }

    override fun applyOrientationProperties(
        forceOrientation: Int,
        allowOrientationChange: Boolean,
    ) {
        try {
            val activity = view.getActivity()
            if (activity.shouldSkipOrientationChange()) {
                return
            }

            saveOriginalOrientation()

            activity.requestedOrientation =
                when (forceOrientation) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    else -> {
                        if (allowOrientationChange) {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            activity.resources.configuration.orientation
                        }
                    }
                }
        } catch (e: Exception) {
            Logger.e("applyOrientationProperties: ", e)
        }
    }

    override fun restoreOriginalOrientation() {
        try {
            view.getActivity().apply {
                if (!shouldSkipOrientationChange() &&
                    requestedOrientation != originalOrientation
                ) {
                    Logger.e("restoreOriginalOrientation: $originalOrientation")
                    requestedOrientation = originalOrientation
                }
            }
        } catch (e: Exception) {
            Logger.e("restoreOriginalOrientation: ", e)
        }
    }

    private fun saveOriginalOrientation() {
        try {
            originalOrientation = view.getActivity().requestedOrientation
        } catch (e: Exception) {
            Logger.e("saveOriginalOrientation: ", e)
        }
    }
}

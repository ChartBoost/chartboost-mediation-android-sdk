package com.chartboost.sdk.internal.initialization

import android.content.Context
import android.os.Build
import com.chartboost.sdk.BuildConfig
import com.chartboost.sdk.internal.Networking.NetworkStateChecker
import com.chartboost.sdk.internal.logging.Logger

/**
 * Preconditions to be met before initializing the SDK.
 *
 * @param context The application context.
 */
internal class InitializationPreconditions(context: Context) {
    /**
     * Network state checker to monitor internet connectivity.
     */
    private val networkStateChecker: NetworkStateChecker =
        NetworkStateChecker(context).apply { startChecking() }

    /**
     * List of checks to be performed before initializing the SDK.
     * Append additional checks here.
     *
     * @return true if all checks pass, false otherwise
     */
    private val preconditions: List<() -> Boolean> =
        listOf(
            ::isDeviceCompatible,
            ::isInternetAvailable,
        )

    /**
     * Check if all preconditions are met. This method should be called before initializing the SDK.
     *
     * @return true if all checks pass, false otherwise.
     */
    internal fun satisfied(): Boolean {
        for (precondition in preconditions) {
            if (!precondition()) {
                return false
            }
        }
        return true
    }

    /**
     * Cleanup resources used by the preconditions.
     */
    internal fun cleanup() {
        networkStateChecker.stopChecking()
    }

    /**
     * Check if the Android version is supported.
     *
     * @return true if the device is compatible, false otherwise.
     */
    private fun isDeviceCompatible(): Boolean {
        val requiredVersion = Build.VERSION_CODES.LOLLIPOP
        val currentVersion = Build.VERSION.SDK_INT

        return if (currentVersion >= requiredVersion) {
            true
        } else {
            Logger.e(
                "Device is not compatible. Required Android version: " +
                    "$requiredVersion. Current version: $currentVersion",
            )
            false
        }
    }

    /**
     * Check if the device has an active internet connection.
     *
     * @return true if the device has an active internet connection, false otherwise.
     */
    private fun isInternetAvailable(): Boolean {
        // Bypass network check if the build is a debug build to allow testing without internet.
        if (BuildConfig.DEBUG || BuildConfig.BYPASS_NETWORK_CHECK) {
            return true
        }

        return if (networkStateChecker.isInternetAvailable()) {
            true
        } else {
            Logger.e("Internet connection is not available.")
            false
        }
    }
}

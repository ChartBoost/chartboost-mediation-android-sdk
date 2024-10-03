@file:JvmName("Extensions")
@file:JvmMultifileClass

package com.chartboost.sdk.internal.Model

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.chartboost.sdk.internal.Libraries.TimeSource
import com.chartboost.sdk.internal.Networking.CBReachability
import com.chartboost.sdk.internal.logging.Logger
import org.json.JSONArray

@Suppress("UNCHECKED_CAST")
fun <T> JSONArray.asList(): List<T> = (0 until length()).map { get(it) as T }

fun <T> JSONArray.asListSkipNull(): List<T> = (0 until length()).mapNotNull { get(it) as? T }

fun TimeSource.toBodyFields(): TimeSourceBodyFields {
    return TimeSourceBodyFields(
        currentTimeMillis(),
        nanoTime(),
        uptimeMillis(),
    )
}

fun CBReachability.toReachabilityBodyFields(): ReachabilityBodyFields {
    return ReachabilityBodyFields(
        this.cellularConnectionType(),
        this.connectionTypeFromActiveNetwork().value,
        this.connectionTypeAsString(),
        this.openRTBConnectionType(),
    )
}

fun PackageManager.getPackageVersionName(packageName: String): String =
    try {
        getPackageInfoCompat(packageName, PackageManager.GET_META_DATA).versionName
    } catch (e: Exception) {
        Logger.e("Exception raised getting package manager object", e)
        ""
    }

fun PackageManager.getPackageInfoCompat(
    packageName: String,
    flags: Int = 0,
): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getPackageInfo(packageName, flags)
    }

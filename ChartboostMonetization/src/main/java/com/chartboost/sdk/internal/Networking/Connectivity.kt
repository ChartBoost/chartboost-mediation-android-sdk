package com.chartboost.sdk.internal.Networking

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.VisibleForTesting
import com.chartboost.sdk.internal.Networking.requests.NetworkType
import com.chartboost.sdk.internal.logging.Logger.d

/**
 * Helper functions to manage some of the most common features of CONNECTIVITY_SERVICE
 */

@VisibleForTesting
fun Context?.connectivityManager(): ConnectivityManager? =
    this?.runCatching {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
    }?.onFailure { e ->
        d(
            "Cannot retrieve connectivity manager",
            e,
        )
    }?.getOrNull()

@SuppressLint("MissingPermission")
@VisibleForTesting
fun Context?.activeNetworkInfo(): NetworkInfo? =
    this?.connectivityManager()
        ?.runCatching { activeNetworkInfo }
        ?.onFailure { e ->
            d(
                "Cannot retrieve active network info",
                e,
            )
        }?.getOrNull()

@SuppressLint("MissingPermission")
@VisibleForTesting
fun Context?.networkCapabilities(network: Network? = null): NetworkCapabilities? =
    this?.connectivityManager()?.runCatching {
        getNetworkCapabilities(network ?: activeNetwork)
    }?.onFailure { e ->
        d(
            "Cannot retrieve network capabilities",
            e,
        )
    }?.getOrNull()

/**
 * Check if there is any connectivity
 */
fun Context?.isNetworkConnected(): Boolean {
    activeNetworkInfo().let { info ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Network null means we check current network
            networkCapabilities()?.let { capabilities ->
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        return info != null && info.isConnected
    }
}

/**
 * Check if there is any connectivity to a Wifi network
 */
fun Context?.isWifiConnected(): Boolean =
    activeNetworkInfo()?.let { info ->
        info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
    } ?: false

/**
 * Check if there is any connectivity to a mobile network
 */
fun Context?.isMobileConnected(): Boolean =
    activeNetworkInfo()?.let { info ->
        info.isConnected && info.type == ConnectivityManager.TYPE_MOBILE
    } ?: false

fun Context?.networkConnectionType(): Int =
    activeNetworkInfo()?.let { info ->
        if (info.isConnected) {
            info.subtype
        } else {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
    } ?: TelephonyManager.NETWORK_TYPE_UNKNOWN

fun Context?.openRTBConnectionType(): NetworkType =
    activeNetworkInfo()?.let { info ->
        if (info.isConnected) {
            openRTBConnectionType(info.type, info.subtype)
        } else {
            NetworkType.UNKNOWN
        }
    } ?: NetworkType.UNKNOWN

/*
* Above API level 7, make sure to set android:targetSdkVersion to appropriate level to use these
*/
private fun openRTBConnectionType(
    type: Int,
    subType: Int,
): NetworkType =
    when (type) {
        ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
        ConnectivityManager.TYPE_MOBILE -> {
            when (subType) {
                TelephonyManager.NETWORK_TYPE_IDEN, // API level 8 ~25 kbps
                TelephonyManager.NETWORK_TYPE_1xRTT, // ~50-100 kbps
                TelephonyManager.NETWORK_TYPE_CDMA, // ~14-64 kbps
                TelephonyManager.NETWORK_TYPE_EDGE, // ~50-100 kbps
                TelephonyManager.NETWORK_TYPE_GPRS,
                -> // ~100 kbps
                    NetworkType.CELLULAR_2G

                TelephonyManager.NETWORK_TYPE_EVDO_0, // ~400-1000 kbps
                TelephonyManager.NETWORK_TYPE_EVDO_A, // ~600-1400 kbps
                TelephonyManager.NETWORK_TYPE_HSDPA, // ~2-14 Mbps
                TelephonyManager.NETWORK_TYPE_HSPA, // ~700-1700 kbps
                TelephonyManager.NETWORK_TYPE_HSUPA, // ~1-23 Mbps
                TelephonyManager.NETWORK_TYPE_UMTS, // ~400-7000 kbps
                TelephonyManager.NETWORK_TYPE_EHRPD, // API level 11 ~1-2 Mbps
                TelephonyManager.NETWORK_TYPE_EVDO_B, // API level 9 ~5 Mbps
                TelephonyManager.NETWORK_TYPE_HSPAP,
                -> // API level 13 ~10-20 Mbps
                    NetworkType.CELLULAR_3G

                // Note that 5G NSA will return this
                TelephonyManager.NETWORK_TYPE_LTE -> // API level 11 ~ 10+ Mbps
                    NetworkType.CELLULAR_4G

                // Only 5G NR will return this
                TelephonyManager.NETWORK_TYPE_NR -> // API level 29 ~ 100+ Mbps
                    NetworkType.CELLULAR_5G

                else -> NetworkType.CELLULAR_UNKNOWN
            }
        }
        else -> NetworkType.UNKNOWN
    }

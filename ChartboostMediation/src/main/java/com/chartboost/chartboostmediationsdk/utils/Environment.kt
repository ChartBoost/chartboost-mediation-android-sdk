/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import java.time.OffsetTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import kotlin.math.sqrt

/**
 * @suppress
 *
 * An object that gives information about a device's environment.
 */
object Environment {
    /**
     * Stores the app context after initialization.
     */
    internal var appContext: Context? = null

    /**
     * Get the device's carrier name.
     */
    var carrierName: String? = null
        get() {
            appContext?.let { context ->
                val manager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                manager?.networkOperatorName?.let {
                    field = it
                }
            }
            return field
        }
        private set

    /**
     * Get the device's locale language.
     */
    var language: String? = null
        get() {
            return Locale.getDefault().language
        }
        private set

    /**
     * Get the device's type.
     */
    var deviceType: Int? = null
        get() {
            appContext?.let { context ->
                context.resources?.let {
                    val metrics = it.displayMetrics
                    val yInches = metrics.heightPixels / metrics.ydpi
                    val xInches = metrics.widthPixels / metrics.xdpi
                    val diagonalInches =
                        sqrt((xInches * xInches + yInches * yInches).toDouble())
                    field =
                        if (diagonalInches >= 6.5) {
                            5 // Tablet
                        } else {
                            4 // Phone
                        }
                }
            }
            return field
        }
        private set

    /**
     * Returns the device's connection type.
     * For a list of what connection types are available:
     * https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/master/AdCOM%20v1.0%20FINAL.md#list--connection-types-
     * @return a numerical value representing the connection type.
     */
    val connectionType: Int
        get() {
            appContext?.let { context ->
                // Handle connection type for Android M (API 23) & up.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Get a ConnectivityManager
                    val connectivityManager =
                        context.applicationContext.getSystemService(ConnectivityManager::class.java)
                    connectivityManager?.let { connectivity ->
                        // Get the active network.
                        val activeNetwork =
                            connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                        // Check the network's capability, determine its connection, and return the
                        // numerical value we need for our BidRequest. Otherwise, continue.
                        activeNetwork?.let {
                            when {
                                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return 1
                                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return 2
                                else -> {} // continue
                            }
                        }
                    }
                } else {
                    // Handle connection type for ethernet and wifi for Android L (API 22) & lower.
                    val connectivityManager =
                        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    connectivityManager?.activeNetworkInfo?.let { info ->
                        if (info.isConnected) {
                            when (info.type) {
                                ConnectivityManager.TYPE_ETHERNET -> return 1
                                ConnectivityManager.TYPE_WIFI -> return 2
                            }
                        }
                    }
                }
            }

            var networkType = 0

            appContext?.let { context ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val telephonyManager =
                        context
                            .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

                    telephonyManager?.let {
                        networkType =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                it.dataNetworkType
                            } else {
                                it.networkType
                            }
                    }
                }
            }

            return when (networkType) {
                // WIFI
                TelephonyManager.NETWORK_TYPE_IWLAN -> 2

                // 2G
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_GSM,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN,
                -> 4

                // 3G
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                -> 5

                // 4G
                TelephonyManager.NETWORK_TYPE_LTE -> 6

                // 5G
                TelephonyManager.NETWORK_TYPE_NR -> 7

                // Unknown connection type
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> 3

                else -> 3
            }
        }

    /**
     * Returns the Mobile Country Code and Mobile Network Code in mcc-mnc format.
     */
    val mccmnc: String? = null
        get() {
            appContext?.let { context ->
                /*
                If we do not care about phone permissions, this is another way of doing the same.
                    val mcc = context.resources.configuration.mcc
                    val mnc = context.resources.configuration.mnc
                    if (mcc != 0 && mnc != Configuration.MNC_ZERO)
                        return "$mcc-$mnc"
                 */
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // User granted phone state permissions.
                    val telephonyManager =
                        context
                            .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

                    telephonyManager?.let {
                        // If the telephony manager is not null and the SIM state is ready, then
                        // get the networkOperator and reformat the MCCMNC
                        if (it.simState == TelephonyManager.SIM_STATE_READY) {
                            // The simOperator returns string numbers for MCCMNC (i.e. 123456 or 12345)
                            val netOp = it.simOperator
                            return netOp?.let { mccMnc ->
                                // We replace the returned MCCMNC with MCC-MNC (i.e. 123-456 or 123-45)
                                // from using a thread-safe StringBuffer.
                                // Note: MCC will always be 3 decimal digits per their standard
                                // and therefore, append '-' after it.
                                return StringBuffer(mccMnc).insert(3, "-").toString()
                            }
                        }
                    }
                }
            }
            return field
        }

    /**
     * Gets the UTC offset in minutes.
     * **See:** [Open-RTB](https://iabtechlab.com/wp-content/uploads/2022/04/OpenRTB-2-6_FINAL.pdf)
     * For example: a UTC of -0700 will be -360.
     *
     * Android APIs < 26 do not support java.time API. Thus, a different approach is used.
     * Future reference: java.time is part of
     * [desugaring](https://developer.android.com/studio/write/java8-support?hl=en#library-desugaring)
     */
    val utcOffsetTime: Int
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OffsetTime.now(ZoneId.systemDefault()).offset.totalSeconds / 60
            } else {
                // Android Devices that don't support the above java APIs.
                // See https://developer.android.com/reference/java/util/Date#getTimezoneOffset()
                val calendar = Calendar.getInstance(Locale.getDefault())
                return (
                    calendar.get(Calendar.ZONE_OFFSET) +
                        calendar.get(Calendar.DST_OFFSET)
                ) /
                    (60 * 1000)
            }
        }
}

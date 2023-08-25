/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import com.chartboost.heliumsdk.HeliumSdk
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSet
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.OffsetTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

/**
 * @suppress
 *
 * An object that gives information about a device's environment.
 */
object Environment {
    private const val HELIUM_ENV_SHARED_PREFS_KEY = "com.chartboost.helium"
    private const val HELIUM_UA_IDENTIFIER_KEY = "HELIUM_UA_IDENTIFIER"
    private const val TC_STRING = "IABTCF_TCString"

    /**
     * Get the device's carrier name.
     */
    var carrierName: String? = null
        get() {
            HeliumSdk.context?.let {
                val manager =
                    it.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
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
     * Get the device's screen density.
     */
    var pxRatio: Float? = null
        get() {
            HeliumSdk.context?.let {
                field = it.resources.displayMetrics.density
            }
            return field
        }
        private set

    /**
     * Get the device's type.
     */
    var deviceType: Int? = null
        get() {
            HeliumSdk.context?.let { context ->
                context.resources?.let {
                    val metrics = it.displayMetrics
                    val yInches = metrics.heightPixels / metrics.ydpi
                    val xInches = metrics.widthPixels / metrics.xdpi
                    val diagonalInches =
                        sqrt((xInches * xInches + yInches * yInches).toDouble())
                    field = if (diagonalInches >= 6.5) {
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
     * Get the device's lmt.
     */
    var lmt: Int? = null
        get() {
            advertisingIdClient()?.let {
                return if (it.isLimitAdTrackingEnabled) 1 else 0
            }

            // Devices that don't have Google Play services
            advertisingIdNonGooglePlay()?.let {
                return it.lmt
            }

            return null
        }
        private set

    /**
     * Get the device's ifa.
     */
    var ifa: String? = null
        get() {
            advertisingIdClient()?.let {
                return it.id
            }

            // Devices that don't have Google Play services
            advertisingIdNonGooglePlay()?.let {
                return it.id
            }

            return null
        }
        private set

    /**
     *  A private function that returns a data class with lmt and an advertising id.
     *  Returns null if an error is found when retrieving the info.
     */
    private fun advertisingIdNonGooglePlay(): NonGooglePlayAdvertisingClient? {
        try {
            // Get a context.
            HeliumSdk.context?.let { context ->
                // Get a Content Resolver
                val contentResolver = context.contentResolver
                // Query the advertising_id and limit_ad_tracking of the device and set it to the
                // NonGooglePlayAdvertisingClient data object.
                contentResolver?.let {
                    // Settings.Secure.getInt throws an error exception if value is not found.
                    val lmt: Int = try {
                        (Settings.Secure.getInt(it, "limit_ad_tracking"))
                    } catch (ex: Settings.SettingNotFoundException) {
                        LogController.e("Exception raised while retrieving lmt ${ex.message}")
                        return null
                    }

                    // Settings.Secure.getString returns null if not present.
                    Settings.Secure.getString(it, "advertising_id")?.let { id ->
                        // Set the id and lmt to our data object.
                        return NonGooglePlayAdvertisingClient(id, lmt)
                    }
                }
            }
        } catch (ex: RuntimeException) {
            LogController.e("Exception raised while retrieving ad information: ${ex.message}")
        }
        return null
    }

    // A data class that is used to store a queried lmt and the advertising id.
    private data class NonGooglePlayAdvertisingClient(val id: String?, val lmt: Int)

    /**
     * Returns an AdvertisingIdClient object. Otherwise an error is log and null is returned.
     */
    private fun advertisingIdClient(): AdvertisingIdClient.Info? {
        HeliumSdk.context?.let { context ->
            try {
                return AdvertisingIdClient.getAdvertisingIdInfo(context.applicationContext)
            } catch (exception: Exception) {
                LogController.e("Exception raised while retrieving adInfo: ${exception.message}")
            }
        }
        return null
    }

    private var packageName: String? = null

    /**
     * Return an application versionName.
     * @return appVersionName the application versionName.
     */
    var appVersionName: String? = null
        get() {
            // Get the HeliumSdk context.
            HeliumSdk.context?.let { context ->
                try {
                    // If we have a context, let's grab the package info from the package manager to grab the versionName.
                    val packageManager = context.applicationContext.packageManager
                    val packageName = context.packageName
                    // Check the nullability of the packageManager && packageName in case the device has settings
                    // that prevent us from getting a PackageManager and packageName.
                    if (packageManager != null && packageName != null) {
                        val packageInfo =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                packageManager.getPackageInfo(
                                    packageName,
                                    PackageManager.PackageInfoFlags.of(0)
                                )
                            } else {
                                packageManager.getPackageInfo(packageName, 0)
                            }
                        // Checking the nullability of the package info as a last check.
                        // Most likely, the getPackageInfo will throw an error if something else goes wrong.
                        packageInfo?.let {
                            field = it.versionName
                        }
                    }
                } catch (nameNotFoundException: PackageManager.NameNotFoundException) {
                    // If an error is found, let's log it.
                    LogController.e("Exception raised while retrieving appVersionName: ${nameNotFoundException.message}")
                }
            }
            return field
        }
        private set

    internal var sessionId: String? = null
        get() {
            if (field == null) {
                field = generateSessionId()
            }
            return field
        }

    private var sessionStart: Long? = null

    // This shouldn't be stored as orientation changes my happen during execution
    val displayWidth: Int
        get() {
            HeliumSdk.context?.let { context ->
                context.resources?.let {
                    // This shouldn't be stored as orientation changes my happen during execution
                    return it.displayMetrics.widthPixels
                }
            }
            return 0
        }

    // This shouldn't be stored as orientation changes my happen during execution
    val displayHeight: Int
        get() {
            HeliumSdk.context?.let { context ->
                context.resources?.let {
                    // This shouldn't be stored as orientation changes my happen during execution
                    return it.displayMetrics.heightPixels
                }
            }
            return 0
        }

    val manufacturer: String
        get() = Build.MANUFACTURER

    val model: String
        get() = Build.MODEL

    val operatingSystem = "Android"

    val operatingSystemVersion: String
        get() = Build.VERSION.RELEASE

    private val appSetIdMutex = Mutex()

    var appSetId: String? = null
        get() {
            HeliumSdk.context?.let { context ->
                CoroutineScope(IO).launch {
                    appSetIdMutex.withLock {
                        updateAppSetId(context)
                    }
                }
            }
            return field
        }
        private set

    var appSetIdScope: Int? = null
        private set

    /**
     * Returns the device's connection type.
     * For a list of what connection types are available:
     * https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/master/AdCOM%20v1.0%20FINAL.md#list--connection-types-
     * @return a numerical value representing the connection type.
     */
    val connectionType: Int
        get() {
            HeliumSdk.context?.let { context ->
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

            HeliumSdk.context?.let { context ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val telephonyManager = context
                        .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

                    telephonyManager?.let {
                        networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
                TelephonyManager.NETWORK_TYPE_IDEN -> 4
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
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> 5
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
     * Gets the app's package name.
     */
    val appBundle: String?
        get() {
            HeliumSdk.context?.let {
                packageName = it.packageName
            }
            return packageName
        }

    /**
     * Get the device's user agent.
     */
    val userAgent: String
        get() {
            // The user agent might or might not exist. Just return what we have and fetch one
            // later outside of Helium inits. This way we aren't blocking Helium inits.
            // See HB-3871 and HB-3761.
            return HeliumSdk.context?.let { context ->
                val sharedPreferences = context.getSharedPreferences(
                    HELIUM_UA_IDENTIFIER_KEY,
                    Context.MODE_PRIVATE
                )
                sharedPreferences.getString(HELIUM_UA_IDENTIFIER_KEY, "")
            } ?: ""
        }

    fun fetchUserAgent() {
        CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            val uaErrorMessage =
                "The Helium SDK failed to check for the user-agent. This app may result in " +
                        "low fill rates and impressions."
            LogController.w("$uaErrorMessage.\nError found: $error")
        }) {
            HeliumSdk.context?.let { context ->
                val webView = WebView(context)
                val userAgent = webView.settings.userAgentString
                webView.destroy()

                // Only cache the user agent if it's not empty.
                userAgent?.isNotEmpty()?.let {
                    val sharedPreferences = context.getSharedPreferences(
                        HELIUM_UA_IDENTIFIER_KEY,
                        Context.MODE_PRIVATE
                    )
                    sharedPreferences.edit().putString(HELIUM_UA_IDENTIFIER_KEY, userAgent).apply()
                }
            }
        }
    }

    /**
     * Generates a new session ID, starts a session, and updates the AppSet ID.
     */
    fun startSession(context: Context) {
        if (sessionId == null) {
            sessionId = generateSessionId()
        }
        if (sessionStart == null) {
            sessionStart = SystemClock.uptimeMillis() / 1000L
        }
        CoroutineScope(IO).launch {
            appSetIdMutex.withLock {
                updateAppSetId(context)
            }
        }
    }

    /**
     * Gets the current session ID.
     *
     * Note: This change to fetchSessionId() is temporary but necessary
     */
    fun fetchSessionId(): String? {
        if (sessionId == null) {
            sessionId = generateSessionId()
        }
        return sessionId
    }

    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    internal suspend fun updateAppSetId(context: Context) {
        return withContext(IO) {
            try {
                val client = AppSet.getClient(context.applicationContext)
                val task = client.appSetIdInfo

                task.addOnSuccessListener {
                    appSetIdScope = it.scope
                    appSetId = it.id
                }
            } catch (error: Exception) {
                LogController.e("Exception raised while retrieving AppSet ID: ${error.message}")
            }
        }
    }

    /**
     * Returns the session time in seconds.
     */
    val sessionTimeSeconds: Int
        get() {
            val start = sessionStart ?: return 0
            return (SystemClock.uptimeMillis() / 1000L - start).toInt()
        }

    /**
     * Returns the Mobile Country Code and Mobile Network Code in mcc-mnc format.
     */
    val mccmnc: String? = null
        get() {
            HeliumSdk.context?.let { context ->
                /*
                If we do not care about phone permissions, this is another way of doing the same.
                    val mcc = context.resources.configuration.mcc
                    val mnc = context.resources.configuration.mnc
                    if (mcc != 0 && mnc != Configuration.MNC_ZERO)
                        return "$mcc-$mnc"
                */
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // User granted phone state permissions.
                    val telephonyManager = context
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
                return (calendar.get(Calendar.ZONE_OFFSET) +
                        calendar.get(Calendar.DST_OFFSET)) /
                        (60 * 1000)
            }
        }

    /**
     * Gets the TC String from default shared preferences. This can return null.
     * https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework/blob/master/TCFv2/IAB%20Tech%20Lab%20-%20CMP%20API%20v2.md
     *
     * @return The TC String or null if it does not exist
     */
    val tcString: String?
        get() {
            return HeliumSdk.context?.let { context ->
                // Get the default shared preferences
                val sharedPreferences = context.getSharedPreferences(
                    "${context.packageName}_preferences",
                    Context.MODE_PRIVATE
                )
                sharedPreferences.getString(TC_STRING, null)
            } ?: run {
                LogController.w("null TC String because context is null")
                null
            }
        }
}

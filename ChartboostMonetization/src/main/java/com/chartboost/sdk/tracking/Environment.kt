package com.chartboost.sdk.tracking

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.LocaleList
import android.os.StatFs
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.chartboost.sdk.BuildConfig
import com.chartboost.sdk.internal.Libraries.CBUtility
import com.chartboost.sdk.internal.Libraries.DisplayMeasurement
import com.chartboost.sdk.internal.Model.IdentityBodyFields
import com.chartboost.sdk.internal.Model.SessionBodyFields
import com.chartboost.sdk.internal.View.getOrientationAsString
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.privacy.model.CCPA
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.privacy.model.GDPR
import com.chartboost.sdk.privacy.model.LGPD
import com.chartboost.sdk.tracking.Environment.ContextProvider.context
import java.lang.ref.WeakReference
import java.util.Locale

data class EnvironmentData(
    val sessionId: String = "not available",
    val sessionCount: Int = 0,
    val appId: String = "not available",
    val appVersion: String = "not available",
    val chartboostSdkVersion: String = "not available",
    val chartboostSdkAutocacheEnabled: Boolean = false,
    val chartboostSdkGdpr: String = "not available",
    val chartboostSdkCcpa: String = "not available",
    val chartboostSdkCoppa: String = "not available",
    val chartboostSdkLgpd: String = "not available",
    val deviceId: String = "not available",
    val deviceMake: String = "not available",
    val deviceModel: String = "not available",
    val deviceOsVersion: String = "not available",
    val devicePlatform: String = "not available",
    val deviceCountry: String = "not available",
    val deviceLanguage: String = "not available",
    val deviceTimezone: String = "not available",
    val deviceConnectionType: String = "not available",
    val deviceOrientation: String = "not available",
    val deviceBatteryLevel: Int = 0,
    val deviceChargingStatus: Boolean = false,
    val deviceVolume: Int = 0,
    val deviceMute: Boolean = false,
    val deviceAudioOutput: Int = 0,
    val deviceStorage: Long = 0,
    val deviceLowMemoryWarning: Long = 0,
    val sessionImpressionInterstitialCount: Int = 0,
    val sessionImpressionRewardedCount: Int = 0,
    val sessionImpressionBannerCount: Int = 0,
    val sessionDuration: Long = 0,
    val deviceUpTime: Long = SystemClock.uptimeMillis(),
)

internal class Environment(
    private val app: Application,
    private val displayMeasurement: DisplayMeasurement,
) {
    fun build(
        identity: IdentityBodyFields?,
        session: SessionBodyFields?,
        connectionType: String?,
        privacyApi: PrivacyApi,
        appId: String?,
    ): EnvironmentData {
        val batteryInfo = getDeviceBatteryInfo()
        return EnvironmentData(
            sessionId = session?.id ?: "session not ready",
            sessionCount = session?.sessionCounter ?: -1,
            appId = appId ?: "App was not init yet",
            appVersion = appVersion ?: "App was not init yet",
            chartboostSdkVersion = BuildConfig.SDK_VERSION,
            chartboostSdkAutocacheEnabled = false,
            chartboostSdkGdpr =
                privacyApi.getPrivacyStandard(GDPR.GDPR_STANDARD)?.consent
                    as? String ?: "gdpr not available",
            chartboostSdkCcpa =
                privacyApi.getPrivacyStandard(CCPA.CCPA_STANDARD)?.consent
                    as? String ?: "ccpa not available",
            chartboostSdkCoppa =
                privacyApi.getPrivacyStandard(COPPA.COPPA_STANDARD)
                    ?.consent?.toString() ?: "coppa not available",
            chartboostSdkLgpd =
                privacyApi.getPrivacyStandard(LGPD.LGPD_STANDARD)
                    ?.consent?.toString() ?: "lgpd not available",
            deviceId = getDeviceId(identity),
            deviceMake = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceOsVersion = "Android " + Build.VERSION.RELEASE,
            devicePlatform = platform,
            deviceCountry = locale?.country ?: "Cannot retrieve country",
            deviceLanguage = systemLanguage,
            deviceTimezone = currentTimeZone,
            deviceConnectionType = connectionType ?: "connection type not provided",
            deviceOrientation = orientationString,
            deviceBatteryLevel = batteryInfo.batteryLevel,
            deviceChargingStatus = batteryInfo.isCharging,
            deviceVolume = audioVolumeLevelForMusic,
            deviceMute = isAudioMute,
            deviceAudioOutput = audioOutput,
            deviceStorage = deviceStorage,
            deviceLowMemoryWarning = availableHeapMemoryInMB,
            sessionImpressionInterstitialCount = session?.interstitialImpressionCounter ?: 0,
            sessionImpressionRewardedCount = session?.rewardedImpressionCounter ?: 0,
            sessionImpressionBannerCount = session?.bannerImpressionCounter ?: 0,
            sessionDuration = session?.duration ?: -1,
        )
    }

    private val orientationString: String
        get() =
            try {
                app.getOrientationAsString(displayMeasurement)
            } catch (e: Exception) {
                Logger.d("Cannot retrieve orientation", e)
                "Cannot retrieve orientation"
            }

    private val currentTimeZone: String =
        try {
            CBUtility.getCurrentTimezone()
        } catch (e: Exception) {
            Logger.d("Cannot retrieve timezone", e)
            "Cannot retrieve timezone"
        }

    private fun getDeviceId(identity: IdentityBodyFields?): String {
        return identity?.let { it.gaid ?: it.uuid } ?: "unknown"
    }

    private val deviceStorage: Long
        get() =
            try {
                StatFs("${app.cacheDir}/.chartboost").availableBytes
            } catch (e: Exception) {
                Logger.d("Cannot create environment device storage for tracking", e)
                -1
            }

    private val platform: String
        get() =
            if ("Amazon".equals(Build.MANUFACTURER, ignoreCase = true)) {
                "Amazon"
            } else {
                "Android"
            }

    private val systemLanguage: String
        get() =
            if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                try {
                    LocaleList.getDefault()[0].language
                } catch (e: Exception) {
                    Logger.d("Cannot retrieve language", e)
                    "Cannot retrieve language"
                }
            } else {
                locale?.language ?: "Cannot retrieve language"
            }

    private val locale: Locale? =
        try {
            Locale.getDefault()
        } catch (e: Exception) {
            Logger.d("Cannot retrieve locale", e)
            null
        }

    private fun getDeviceBatteryInfo(): DeviceBattery {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            try {
                val batteryManager =
                    app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val batLevel =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                var isCharging = false
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    // silent crash below M
                    isCharging = batteryManager.isCharging
                }
                return DeviceBattery(
                    batLevel,
                    isCharging,
                )
            } catch (e: Exception) {
                Logger.d("Cannot create environment device battery for tracking", e)
            }
        }
        return DeviceBattery()
    }

    private val isAudioMute: Boolean
        get() =
            try {
                (app.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode != AudioManager.RINGER_MODE_NORMAL
            } catch (e: Exception) {
                Logger.d("Cannot create environment audio for tracking", e)
                false
            }

    private val audioVolumeLevelForMusic: Int
        get() =
            try {
                val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val volumeLevel = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVolumeLevel = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                (volumeLevel.toFloat() / maxVolumeLevel * 100).toInt()
            } catch (e: Exception) {
                Logger.d("Cannot create environment audio for tracking", e)
                -1
            }

    /**
     * 0: speaker,
     * 1: wired headphones,
     * 2: bluetooth A2DP,
     * 3: other
     *
     * @return
     */
    private val audioOutput: Int
        get() =
            try {
                val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    getAudioOutputFromDevices(audio)
                } else {
                    getAudioOutputForOlderDevices(audio)
                }
            } catch (e: Exception) {
                Logger.d("Cannot create environment audio output for tracking", e)
                AudioOutputType.OTHER.value
            }

    @RequiresApi(VERSION_CODES.M)
    private fun getAudioOutputFromDevices(audioManager: AudioManager): Int {
        val output: AudioDeviceInfo? = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)[0]
        // TODO investigate if this is correct
        return when (output?.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputType.BUILTIN_SPEAKER.value
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioOutputType.WIRED_HEADPHONES.value
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutputType.BLUETOOTH_A2DP.value
            else -> AudioOutputType.OTHER.value
        }
    }

    private fun getAudioOutputForOlderDevices(audioManager: AudioManager): Int {
        // on android api 15 to 22 isWiredHeadsetOn and isBluetoothA2dpOn is missing
        return if (audioManager.isSpeakerphoneOn) {
            AudioOutputType.BUILTIN_SPEAKER.value
        } else {
            AudioOutputType.OTHER.value
        }
    }

    private val availableHeapMemoryInMB: Long
        get() {
            val mb = 1048576L
            var availHeapSizeInMB: Long = -1
            availHeapSizeInMB =
                try {
                    val runtime = Runtime.getRuntime()
                    val usedMemInMB = runtime.totalMemory() - runtime.freeMemory()
                    (runtime.maxMemory() - usedMemInMB) / mb
                } catch (e: Exception) {
                    Logger.d("Cannot create environment runtime for tracking", e)
                    return availHeapSizeInMB
                }
            return availHeapSizeInMB
        }

    private enum class AudioOutputType(val value: Int) {
        BUILTIN_SPEAKER(0),
        WIRED_HEADPHONES(1),
        BLUETOOTH_A2DP(2),
        OTHER(3),
    }

    private data class DeviceBattery(
        val batteryLevel: Int = 0,
        val isCharging: Boolean = false,
    )

    object ContextProvider {
        private var contextRef: WeakReference<Context>? = null
        private var appContext: Application? = null

        var context: Context?
            get() = contextRef?.get() ?: appContext
            set(value) {
                if (value !is Application) {
                    contextRef = WeakReference(value)
                    appContext = value?.applicationContext as? Application
                } else {
                    appContext = value
                }
            }

        val application: Application?
            get() = appContext
    }

    /**
     * There are times when we need to reference the following where [[Environment]] cannot be
     * instantiated. Instead of bursting pipes to pass them around, let's just make them static.
     */
    companion object {
        var appVersion: String? = null
            get() {
                try {
                    // If we have a context, let's grab the package info from the package manager to grab the version.
                    val packageManager = context?.packageManager
                    val packageName = context?.packageName
                    // Check the nullability of the packageManager && packageName in case the device has settings
                    // that prevent us from getting a PackageManager and packageName.
                    if (packageManager != null && packageName != null) {
                        val packageInfo =
                            if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                                packageManager.getPackageInfo(
                                    packageName,
                                    PackageManager.PackageInfoFlags.of(0),
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
                    Logger.e("Exception raised while retrieving appVersionName: ${nameNotFoundException.message}")
                }
                return field
            }
    }
}

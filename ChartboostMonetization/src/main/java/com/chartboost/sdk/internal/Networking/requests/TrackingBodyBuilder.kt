package com.chartboost.sdk.internal.Networking.requests

import com.chartboost.sdk.internal.adType.AdType
import com.chartboost.sdk.internal.logging.Logger
import com.chartboost.sdk.tracking.EnvironmentData
import com.chartboost.sdk.tracking.TrackingEvent
import org.json.JSONObject

internal class TrackingBodyBuilder(
    private val jsonFactory: () -> JSONObject = { JSONObject() },
) {
    fun buildTrackingRequestBody(
        event: TrackingEvent,
        environment: EnvironmentData,
    ): String {
        return jsonFactory()
            .addSdkParameters(environment)
            .addEventParameters(event)
            .addSessionParameters(environment)
            .addMediation(event)
            .addAdParameters(event)
            .addDeviceParameters(environment)
            .addPayload(environment, event.impressionAdType)
            .toString()
    }

    private inline fun JSONObject.updateJSON(block: JSONObject.() -> JSONObject?): JSONObject =
        runCatching {
            block() ?: this
        }.onFailure {
            Logger.e("Cannot generate tracking body data: ", it)
        }.getOrElse { this }

    private fun JSONObject.addSdkParameters(environment: EnvironmentData): JSONObject =
        updateJSON {
            put("app_id", environment.appId)
            put("chartboost_sdk_version", environment.chartboostSdkVersion)
        }

    private fun JSONObject.addEventParameters(event: TrackingEvent): JSONObject =
        updateJSON {
            put("event_name", event.name.value)
            put("event_message", event.message)
            put("event_type", event.type.name)
            put("event_timestamp", event.timestampInSeconds)
            put("event_latency", event.latency.toDouble())
        }

    private fun JSONObject.addSessionParameters(environment: EnvironmentData): JSONObject =
        updateJSON {
            put("session_id", environment.sessionId)
            put("session_count", environment.sessionCount)
        }

    private fun JSONObject.addMediation(event: TrackingEvent): JSONObject =
        updateJSON {
            event.mediation?.run {
                put("mediation_sdk", mediationType)
                put("mediation_sdk_version", libraryVersion)
                put("mediation_sdk_adapter_version", adapterVersion)
            }
        }

    private fun JSONObject.addAdParameters(event: TrackingEvent): JSONObject =
        updateJSON {
            put("ad_type", event.impressionAdType.lowercase())
            put("ad_impression_id", event.trackAd?.adImpressionId ?: "missing impression id")
            put("ad_creative_id", event.trackAd?.adCreativeId ?: "missing creative id")
            put("ad_location_id", event.location)
            put("template_url", event.trackAd?.templateUrl ?: "")
            event.trackAd?.adSize?.run {
                put("ad_height", height)
                put("ad_width", width)
            }
        }

    private fun JSONObject.addDeviceParameters(environment: EnvironmentData): JSONObject =
        updateJSON {
            put("device_id", environment.deviceId)
            put("device_make", environment.deviceMake)
            put("device_model", environment.deviceModel)
            put("device_os_version", environment.deviceOsVersion)
            put("device_platform", environment.devicePlatform)
            put("device_country", environment.deviceCountry)
            put("device_connection_type", environment.deviceConnectionType)
            put("device_orientation", environment.deviceOrientation)
        }

    private fun JSONObject.addPayload(
        environment: EnvironmentData,
        impressionAdType: String,
    ): JSONObject =
        updateJSON {
            put("payload", createPayloadJson(environment, impressionAdType))
        }

    private fun createPayloadJson(
        environment: EnvironmentData,
        impressionAdType: String,
    ): JSONObject {
        return jsonFactory().updateJSON {
            put("device_battery_level", environment.deviceBatteryLevel)
            put("device_charging_status", environment.deviceChargingStatus)
            put("device_language", environment.deviceLanguage)
            put("device_timezone", environment.deviceTimezone)
            put("device_volume", environment.deviceVolume)
            put("device_mute", environment.deviceMute)
            put("device_audio_output", environment.deviceAudioOutput)
            put("device_storage", environment.deviceStorage)
            put("device_low_memory_warning", environment.deviceLowMemoryWarning)
            put("device_up_time", environment.deviceUpTime)
            put("chartboost_sdk_autocache_enabled", environment.chartboostSdkAutocacheEnabled)
            put("chartboost_sdk_gdpr", environment.chartboostSdkGdpr)
            put("chartboost_sdk_ccpa", environment.chartboostSdkCcpa)
            put("chartboost_sdk_coppa", environment.chartboostSdkCoppa)
            put("chartboost_sdk_lgpd", environment.chartboostSdkLgpd)
            put("session_duration", environment.sessionDuration)
            put("session_impression_count", getImpressionCount(environment, impressionAdType))
        }
    }

    private fun getImpressionCount(
        environment: EnvironmentData,
        impressionType: String,
    ): Int {
        return when (impressionType) {
            AdType.Interstitial.name -> environment.sessionImpressionInterstitialCount
            AdType.Rewarded.name -> environment.sessionImpressionRewardedCount
            AdType.Banner.name -> environment.sessionImpressionBannerCount
            else -> 0
        }
    }
}

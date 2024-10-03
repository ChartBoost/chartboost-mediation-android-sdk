package com.chartboost.sdk.internal.Networking.requests

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.tracking.EnvironmentData
import com.chartboost.sdk.tracking.TrackAd
import com.chartboost.sdk.tracking.TrackingEvent
import com.chartboost.sdk.tracking.TrackingEventName
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Test

class TrackingBodyBuilderTest {
    private val trackAdMock =
        mockk<TrackAd> {
            every { adImpressionId } returns "testImpressionId"
            every { adCreativeId } returns "testCreativeId"
            every { templateUrl } returns "vastUrl"
            every { location } returns "testLocation"
        }

    private val mediation =
        Mediation(
            "testMediationType",
            "testVersion",
            "testAdapterVersion",
        )

    private val eventMock =
        mockk<TrackingEvent> {
            every { trackAd } returns trackAdMock
            every { impressionAdType } returns "Interstitial"
            every { name } returns TrackingEventName.Misc.TOO_MANY_EVENTS
            every { message } returns "testMsg"
            every { type } returns TrackingEvent.Type.ERROR
            every { timestampInSeconds } returns 1L
            every { latency } returns 100f
            every { location } returns "testLocation"
            every { mediation } returns this@TrackingBodyBuilderTest.mediation
        }

    private val envMock =
        mockk<EnvironmentData> {
            every { chartboostSdkAutocacheEnabled } returns true
            every { chartboostSdkGdpr } returns "gdpr_test"
            every { chartboostSdkCcpa } returns "ccpa_test"
            every { chartboostSdkCoppa } returns "coppa_test"
            every { chartboostSdkLgpd } returns "lgpd_test"
            every { deviceBatteryLevel } returns 1
            every { deviceChargingStatus } returns false
            every { deviceLanguage } returns "US"
            every { deviceTimezone } returns "europe"
            every { deviceVolume } returns 100
            every { deviceMute } returns false
            every { deviceAudioOutput } returns 1
            every { deviceLowMemoryWarning } returns 10L
            every { sessionCount } returns 1
            every { appId } returns "1"
            every { chartboostSdkVersion } returns "8.1.0"
            every { deviceId } returns "1"
            every { deviceModel } returns "phone"
            every { deviceOsVersion } returns "8"
            every { devicePlatform } returns "android"
            every { deviceCountry } returns "spain"
            every { deviceConnectionType } returns "WIFI"
            every { deviceOrientation } returns "portrait"
            every { sessionId } returns "1234"
            every { sessionDuration } returns 1000L
            every { deviceUpTime } returns 1000L
            every { sessionImpressionInterstitialCount } returns 1
            every { sessionImpressionRewardedCount } returns 1
            every { sessionImpressionBannerCount } returns 1
            every { deviceStorage } returns 10
            every { deviceMake } returns "testMake"
        }

    private val bodyBuilder = TrackingBodyBuilder()

    private fun JSONObject.testPayload(): JSONObject =
        apply {
            getJSONObject("payload").let {
                it.getBoolean("chartboost_sdk_autocache_enabled") shouldBe true
                it.getString("chartboost_sdk_gdpr") shouldBe "gdpr_test"
                it.getString("chartboost_sdk_ccpa") shouldBe "ccpa_test"
                it.getString("chartboost_sdk_coppa") shouldBe "coppa_test"
                it.getString("chartboost_sdk_lgpd") shouldBe "lgpd_test"
                it.getInt("device_battery_level") shouldBe 1
                it.getBoolean("device_charging_status") shouldBe false
                it.getString("device_language") shouldBe "US"
                it.getString("device_timezone") shouldBe "europe"
                it.getInt("device_volume") shouldBe 100
                it.getBoolean("device_mute") shouldBe false
                it.getLong("device_storage") shouldBe 10
                it.getInt("device_audio_output") shouldBe 1
                it.getLong("device_low_memory_warning") shouldBe 10
                it.getLong("device_up_time") shouldBe 1000
                it.getLong("session_duration") shouldBe 1000
                it.getInt("session_impression_count") shouldBe 1
            }
        }

    private fun JSONObject.testBody(): JSONObject =
        apply {
            getString("session_id") shouldBe "1234"
            getInt("session_count") shouldBe 1
            getString("event_name") shouldBe "too_many_events"
            getString("event_message") shouldBe "testMsg"
            getString("event_type") shouldBe "ERROR"
            getInt("event_timestamp") shouldBe 1
            getInt("event_latency") shouldBe 100
            getString("ad_type") shouldBe "interstitial"
            getString("ad_impression_id") shouldBe "testImpressionId"
            getString("ad_creative_id") shouldBe "testCreativeId"
            getString("app_id") shouldBe "1"
            getString("chartboost_sdk_version") shouldBe "8.1.0"
            getString("device_id") shouldBe "1"
            getString("device_make") shouldBe "testMake"
            getString("device_model") shouldBe "phone"
            getString("device_os_version") shouldBe "8"
            getString("device_platform") shouldBe "android"
            getString("device_country") shouldBe "spain"
            getString("device_connection_type") shouldBe "WIFI"
            getString("device_orientation") shouldBe "portrait"
            getString("ad_location_id") shouldBe "testLocation"
            getString("template_url") shouldBe "vastUrl"
        }

    private fun JSONObject.testMediation(): JSONObject =
        apply {
            getString("mediation_sdk") shouldBe "testMediationType"
            getString("mediation_sdk_version") shouldBe "testVersion"
            getString("mediation_sdk_adapter_version") shouldBe "testAdapterVersion"
        }

    private fun JSONObject.testMediationMissing(): JSONObject =
        apply {
            optString("mediation_sdk", "foo") shouldBe "foo"
            optString("mediation_sdk_version", "foo") shouldBe "foo"
            optString("mediation_sdk_adapter_version", "foo") shouldBe "foo"
        }

    private fun JSONObject.testAdSize(): JSONObject =
        apply {
            getInt("ad_height") shouldBe 100
            getInt("ad_width") shouldBe 100
        }

    private fun JSONObject.testAdSizeMissing(): JSONObject =
        apply {
            optInt("ad_height", Int.MIN_VALUE) shouldBe Int.MIN_VALUE
            optInt("ad_width", Int.MIN_VALUE) shouldBe Int.MIN_VALUE
        }

    @Test
    fun testWithMediation() {
        bodyBuilder.buildTrackingRequestBody(eventMock, envMock)
            .shouldNotBeNull()
            .let(::JSONObject)
            .testPayload()
            .testMediation()
            .testBody()
    }

    @Test
    fun testWithoutMediation() {
        every { eventMock.mediation } returns null
        bodyBuilder.buildTrackingRequestBody(eventMock, envMock)
            .shouldNotBeNull()
            .let(::JSONObject)
            .testPayload()
            .testMediationMissing()
            .testBody()
    }

    @Test
    fun testWithAdSize() {
        with(trackAdMock) {
            every { adSize } returns TrackAd.AdSize(height = 100, width = 100)
        }
        every { eventMock.trackAd } returns trackAdMock
        bodyBuilder.buildTrackingRequestBody(eventMock, envMock)
            .shouldNotBeNull()
            .let(::JSONObject)
            .testPayload()
            .testAdSize()
    }

    @Test
    fun testWithoutAdSize() {
        with(trackAdMock) {
            every { adSize } returns null
        }
        every { eventMock.trackAd } returns trackAdMock
        bodyBuilder.buildTrackingRequestBody(eventMock, envMock)
            .shouldNotBeNull()
            .let(::JSONObject)
            .testPayload()
            .testAdSizeMissing()
    }
}

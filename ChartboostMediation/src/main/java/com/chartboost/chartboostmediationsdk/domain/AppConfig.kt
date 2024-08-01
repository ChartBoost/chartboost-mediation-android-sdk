/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.controllers.banners.VisibilityTracker
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.util.*

/**
 * @suppress
 */
@Serializable
data class AppConfig(
    @SerialName("app_id")
    val appId: String = ChartboostMediationSdk.getAppId() ?: "",
    @Serializable
    @JvmField
    @SerialName("adapter_classes")
    val adapterClasses: Set<String> = emptySet(),
    @SerialName("logging_level")
    val logLevel: Int = 0,
    @SerialName("banner_load_timeout")
    val bannerLoadTimeoutSeconds: Int = 15,
    @SerialName("banner_size_event_delay_ms")
    val bannerSizeEventDelayMs: Long = 1000L,
    @Serializable(with = Endpoints.Event.EventEnumSetSerializer::class)
    @SerialName("metrics_events")
    val metricsEvents: EnumSet<Endpoints.Event> = EnumSet.allOf(Endpoints.Event::class.java),
    @SerialName("fullscreen_load_timeout")
    val fullscreenLoadTimeoutSeconds: Int = 30,
    @SerialName("show_timeout")
    val showTimeoutSeconds: Int = 5,
    @SerialName("start_timeout")
    val startSdkTimeoutSeconds: Int = 20,
    @SerialName("init_timeout")
    val partnerInitTimeoutSeconds: Int = 1,
    @SerialName("init_metrics_post_timeout")
    val initializationMetricsPostTimeout: Int = 2,
    @SerialName("prebid_fetch_timeout")
    val prebidFetchTimeoutSeconds: Long = 5,
    @SerialName("should_notify_loads")
    val shouldNotifyLoads: Boolean = true,
    @SerialName("banner_impression_min_visible_dips")
    val bannerImpressionMinVisibleDips: Int = VisibilityTracker.MIN_VISIBLE_DIPS,
    @SerialName("banner_impression_min_visible_duration_ms")
    val bannerImpressionMinVisibleDurationMs: Int = VisibilityTracker.MIN_VISIBLE_DURATION_MS,
    @SerialName("visibility_tracker_poll_interval_ms")
    val visibilityTrackerPollIntervalMs: Long = VisibilityTracker.VISIBILITY_CHECK_INTERVAL_MS,
    @SerialName("visibility_tracker_traversal_limit")
    val visibilityTrackerTraversalLimit: Int = VisibilityTracker.TRAVERSAL_LIMIT,
    @SerialName("credentials")
    val credentials: JsonObject = buildJsonObject { },
    @SerialName("placements")
    val placements: List<Placement>? = null,
    @SerialName("log_level")
    val logLevelString: String? = null,
    @SerialName("max_queue_size")
    val maxQueueSize: Int = 5,
    @SerialName("queued_ad_ttl")
    val queueAdTtlSeconds: Long = 3600L,
    @SerialName("default_queue_size")
    val defaultQueueSize: Int = 2,
) {
    companion object {
        /**
         * Get an AppConfig using the provided String
         *
         * @param jsonString serialized AppConfig
         *
         * @return An AppConfig deserialized from the provided string. Default AppConfig if null.
         */
        fun fromJsonString(jsonString: String): AppConfig =
            jsonString.let {
                ChartboostMediationJson.decodeFromString(serializer(), it)
            }
    }

    /**
     * Get a serialized String of the AppConfig
     *
     * @return A string serialized from AppConfig
     */
    fun toJsonString() = ChartboostMediationJson.encodeToString(serializer(), this)

    /**
     * Determine if the minimum number of adapters (1) necessary are present
     *
     * @return true if adapterClasses is not empty, false otherwise.
     */
    fun hasMinimumAdapters() = adapterClasses.isNotEmpty()

    /**
     * Determine if the minimum number of credentials (1) necessary are present
     *
     * @return true if credentials is not empty, false otherwise.
     */
    fun hasMinimumCredentials() = credentials.isNotEmpty()
}

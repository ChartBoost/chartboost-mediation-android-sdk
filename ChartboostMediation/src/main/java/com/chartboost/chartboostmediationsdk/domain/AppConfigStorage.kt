/*
 * Copyright 2024-2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.content.Context
import com.chartboost.chartboostmediationsdk.controllers.banners.VisibilityTracker
import com.chartboost.chartboostmediationsdk.network.Endpoints.DEFAULT_INITIALIZATION_EVENT_URL
import com.chartboost.chartboostmediationsdk.utils.LogController
import kotlinx.serialization.json.*
import java.util.*

/**
 * @suppress
 *
 * Collection of server-side config keys/flags used to configure the Chartboost Mediation SDK.
 */
@Suppress("UNCHECKED_CAST")
object AppConfigStorage {
    private var appConfig: AppConfig = AppConfig()

    /**
     * A set of [Partner] objects to iterate over for adapter-related tasks, e.g. initialization.
     */
    var partners: Set<Partner> = emptySet()
        private set

    /**
     * A set of class paths from which to construct adapter instances.
     */
    var adapterClassPaths: Set<String> = emptySet()
        private set
        get() = appConfig.adapterClasses

    /**
     * A map of <Chartboost Placements, Ad Format> pairs post-processed from the placements list.
     */
    var placementsToAdFormats: Map<String, AdFormat>? = null
        private set
        get() =
            field ?: mutableMapOf<String, AdFormat>().apply {
                appConfig.placements?.forEach { placement ->
                    put(
                        placement.chartboostPlacement,
                        AdFormat.fromString(placement.format.toString()),
                    )
                } ?: LogController.e(
                    "Failed to build placements to ad formats map. Placements list is null.",
                )
                field = this
            }

    /**
     * Log level for the Chartboost Mediation SDK. This is no longer used.
     */
    var logLevel: Int = 0
        private set
        get() = appConfig.logLevel

    /**
     * A map of event_trackers for which to collect data.
     */
    var globalEventTrackers: Map<TrackingEvent, List<ServerEventTracker>> = emptyMap()
        private set

    /**
     * A map of default event_trackers - used in case of missing init data.
     */
    private val defaultEventTrackers: Map<TrackingEvent, List<ServerEventTracker>> =
        mapOf(TrackingEvent.INITIALIZATION to listOf(ServerEventTracker(DEFAULT_INITIALIZATION_EVENT_URL)))

    /**
     * Load timeout for partner banner ad requests.
     */
    var bannerLoadTimeoutSeconds: Int = 15
        private set
        get() = appConfig.bannerLoadTimeoutSeconds

    /**
     * Delay before checking creative size for BANNER_SIZE event
     */
    var bannerSizeEventDelayMs: Long = 1000L
        private set
        get() = appConfig.bannerSizeEventDelayMs

    /**
     * Load timeout for partner fullscreen ad requests.
     */
    var fullscreenLoadTimeoutSeconds: Int = 30
        private set
        get() = appConfig.fullscreenLoadTimeoutSeconds

    /**
     * Show timeout for partner interstitial and rewarded ad requests.
     */
    var showTimeoutSeconds: Int = 5
        private set
        get() = appConfig.showTimeoutSeconds

    /**
     * Timeout for partner SDK initializations. Note that this timeout is only effective prior to
     * adapter separation (Chartboost Mediation v4.0.0).
     */
    var startSdkTimeoutSeconds: Int = 20
        private set
        get() = appConfig.startSdkTimeoutSeconds

    /**
     * Timeout for partner SDK initializations. Note that this timeout is only effective for adapter
     * separation onwards (Chartboost Mediation v4.0.0+).
     */
    var partnerInitTimeoutSeconds: Int = 1
        private set
        get() = appConfig.partnerInitTimeoutSeconds

    /**
     * Timeout for sending initialization metrics data to the server. This way we can avoid waiting
     * for a bad initialization that never completes, while also giving sufficient time for the majority of partners to finish.
     */
    var initializationMetricsPostTimeout: Int = 2
        private set
        get() = appConfig.initializationMetricsPostTimeout

    /**
     * Timeout for partner bid token computation.
     */
    var prebidFetchTimeoutSeconds: Long = 5
        private set
        get() = appConfig.prebidFetchTimeoutSeconds

    /**
     * Config flag for whether to generate a unique load identifier for each ad request.
     */
    var shouldNotifyLoads: Boolean = true
        private set
        get() = appConfig.shouldNotifyLoads

    /**
     * The minimum amount of density-independent pixels a banner needs to be visible
     * to be counted as an impression.
     */
    var bannerImpressionMinVisibleDips: Int = VisibilityTracker.MIN_VISIBLE_DIPS
        private set
        get() = appConfig.bannerImpressionMinVisibleDips

    /**
     * The minimum duration in milliseconds that a banner needs to be visible to be
     * counted as an impression.
     */
    var bannerImpressionMinVisibleDurationMs: Int = VisibilityTracker.MIN_VISIBLE_DURATION_MS
        private set
        get() = appConfig.bannerImpressionMinVisibleDurationMs

    /**
     * How often the [VisibilityTracker] should poll to check visibility.
     */
    var visibilityTrackerPollIntervalMs: Long = VisibilityTracker.VISIBILITY_CHECK_INTERVAL_MS
        private set
        get() = appConfig.visibilityTrackerPollIntervalMs

    /**
     * How many parent views to walk up the view hierarchy for the [VisibilityTracker] when
     * checking overall visibility.
     */
    var visibilityTrackerTraversalLimit: Int = VisibilityTracker.TRAVERSAL_LIMIT
        get() = appConfig.visibilityTrackerTraversalLimit

    /**
     * An error, if any, that occurs while processing the app config.
     */
    var parsingError: MetricsError.JsonParseError? = null

    /**
     * Whether or not a valid cached config exists.
     */
    var validCachedConfigExists: Boolean = true

    val serverLogLevelOverride: LogController.LogLevel?
        get() =
            try {
                LogController.LogLevel.valueOf((appConfig.logLevelString ?: "").uppercase())
            } catch (iae: IllegalArgumentException) {
                null
            }

    /**
     * Maximum queue size.
     */
    var maxQueueSize: Int = 5
        private set
        get() = appConfig.maxQueueSize

    /**
     * Time to live of a queued ad. The default time is one hour.
     */
    var queueAdTtlSeconds: Long = 3600L
        private set
        get() = appConfig.queueAdTtlSeconds

    /**
     * The default queue size when no size is specified by the publisher in the dashboard.
     * The default is 2.
     */
    val defaultQueueSize: Int
        get() = appConfig.defaultQueueSize

    /**
     * Whether or not the Mediation SDK should be disabled.
     */
    val shouldDisableSdk: Boolean
        get() = appConfig.shouldDisableSdk

    fun getEnableRateLimiting(context: Context): Boolean {
        val preferences =
            context.getSharedPreferences("CHARTBOOST_MEDIATION_EXPERIMENTAL", Context.MODE_PRIVATE)
        return preferences.getBoolean(
            "com.chartboost.chartboost_mediation.enable_rate_limiting",
            true,
        )
    }

    fun setEnableRateLimiting(
        context: Context,
        enableRateLimiting: Boolean,
    ) {
        if (!enableRateLimiting) LogController.d("Disabling rate limiting.")
        val preferences =
            context.getSharedPreferences("CHARTBOOST_MEDIATION_EXPERIMENTAL", Context.MODE_PRIVATE)
        preferences
            ?.edit()
            ?.putBoolean(
                "com.chartboost.chartboost_mediation.enable_rate_limiting",
                enableRateLimiting,
            )
            ?.apply()
    }

    /**
     * Update fields with values from the app config JSON, using their assigned values as defaults.
     *
     * @param response The app config String.
     */
    fun updateFields(appConfig: AppConfig) {
        this@AppConfigStorage.appConfig = appConfig

        appConfig.eventTrackers.jsonObject.let {
            globalEventTrackers = compileEventTrackersWithFallback(it)
        }

        appConfig.credentials.jsonObject.let {
            partners = PartnerUtil.compilePartners(it)
        }

        appConfig.placements?.forEach {
            PlacementStorage.addRefreshTime(it.chartboostPlacement, it.autoRefreshRate)
            PlacementStorage.addQueueSize(it.chartboostPlacement, it.queueSize, maxQueueSize)
        }
    }

    /**
     * Compile event trackers from the server config JSON, with fallback for INITIALIZATION.
     */
    private fun compileEventTrackersWithFallback(eventTrackersJson: JsonObject): Map<TrackingEvent, List<ServerEventTracker>> {
        val map = EventTrackersUtil.compileEventTrackers(eventTrackersJson).toMutableMap()

        if (shouldAddDefaultInitializationTracker(eventTrackersJson, map)) {
            defaultEventTrackers[TrackingEvent.INITIALIZATION]?.let {
                map[TrackingEvent.INITIALIZATION] = it
            }
        }

        return map
    }

    /**
     * Determine if there is a need to add default initialization tracker
     */
    private fun shouldAddDefaultInitializationTracker(
        eventTrackersJson: JsonObject,
        alreadyMappedTrackers: Map<TrackingEvent, List<ServerEventTracker>>,
    ): Boolean {
        val initializationJsonObject = eventTrackersJson[TrackingEvent.INITIALIZATION.serverName]
        val initializationMappedTrackers = alreadyMappedTrackers[TrackingEvent.INITIALIZATION]
        return when {
            initializationJsonObject == null -> true
            initializationJsonObject !is JsonArray -> true
            initializationJsonObject.jsonArray.isEmpty() -> false // that's how to switch off tracking for an event
            initializationMappedTrackers == null -> true
            initializationMappedTrackers.all { it.url.isBlank() } -> true
            else -> false // in this case, there is at least one tracker with non blank url
        }
    }
}

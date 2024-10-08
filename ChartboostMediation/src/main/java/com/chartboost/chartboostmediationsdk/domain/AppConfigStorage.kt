/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import android.content.Context
import com.chartboost.chartboostmediationsdk.controllers.banners.VisibilityTracker
import com.chartboost.chartboostmediationsdk.network.Endpoints.Event
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
     * A list of metrics events for which to collect data.
     */
    var metricsEvents: EnumSet<Event> = EnumSet.allOf(Event::class.java)
        private set
        get() = appConfig.metricsEvents

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
        return preferences.getBoolean("com.chartboost.chartboost_mediation.enable_rate_limiting", true)
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
            ?.putBoolean("com.chartboost.chartboost_mediation.enable_rate_limiting", enableRateLimiting)
            ?.apply()
    }

    /**
     * Update fields with values from the app config JSON, using their assigned values as defaults.
     *
     * @param response The app config String.
     */
    fun updateFields(appConfig: AppConfig) {
        this@AppConfigStorage.appConfig =
            appConfig.copy(
                metricsEvents = (
                    getReportableMetricsEvents(
                        EnumSet.allOf(Event::class.java),
                        appConfig.metricsEvents,
                    )
                ),
            )
        // TODO make metrics events a separate enum set field in this class
        appConfig.credentials.jsonObject.let {
            partners = compilePartners(it)
        }

        appConfig.placements?.forEach {
            PlacementStorage.addRefreshTime(it.chartboostPlacement, it.autoRefreshRate)
            PlacementStorage.addQueueSize(it.chartboostPlacement, it.queueSize, maxQueueSize)
        }
    }

    /**
     * Get an EnumSet of the metrics events that should be sent to the server.
     *
     * @param fullSet The full set of measurable metrics events.
     * @param serverSet The set of metrics events whose data should be sent to the server.
     *
     * @return An EnumSet of the metrics events that should be sent to the server.
     */
    private fun getReportableMetricsEvents(
        fullSet: EnumSet<Event>,
        serverSet: Set<Event>,
    ): EnumSet<Event> =
        if (serverSet.isEmpty()) {
            fullSet
        } else {
            EnumSet.noneOf(Event::class.java).apply {
                serverSet.forEach { add(it) }
            }
        }

    /**
     * Compile a list of partners from the app config.
     *
     * @return A List of [Partner] objects.
     */
    private fun compilePartners(credentials: JsonObject): Set<Partner> =
        mutableSetOf<Partner>().apply {
            credentials.keys.forEach { partnerId ->
                add(Partner(partnerId, credentials.getValue(partnerId).jsonObject))
            }
        }
}

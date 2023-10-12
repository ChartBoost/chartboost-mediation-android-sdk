/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import android.content.Context
import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.controllers.banners.VisibilityTracker
import com.chartboost.heliumsdk.network.Endpoints.Sdk
import com.chartboost.heliumsdk.utils.LogController
import kotlinx.serialization.json.*
import java.util.*

/**
 * @suppress
 *
 * Collection of server-side config keys/flags used to configure the Helium SDK.
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
        get() = field ?: mutableMapOf<String, AdFormat>().apply {
            appConfig.placements?.forEach { placement ->
                put(
                    placement.chartboostPlacement,
                    AdFormat.fromString(placement.format.toString())
                )
            } ?: LogController.e(
                "Failed to build placements to ad formats map. Placements list is null."
            )
            field = this
        }

    /**
     * Log level for the Helium SDK.
     * ERROR = 0;
     * WARNING = 1;
     * INFO = 2;
     * DEBUG = 3;
     * VERBOSE = 4;
     */
    var logLevel: Int = 0
        private set
        get() = appConfig.logLevel

    /**
     * A list of metrics events for which to collect data.
     */
    var metricsEvents: EnumSet<Sdk.Event> = EnumSet.allOf(Sdk.Event::class.java)
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
     * adapter separation (Helium v4.0.0).
     */
    var startSdkTimeoutSeconds: Int = 20
        private set
        get() = appConfig.startSdkTimeoutSeconds

    /**
     * Timeout for partner SDK initializations. Note that this timeout is only effective for adapter
     * separation onwards (Helium v4.0.0+).
     */
    var partnerInitTimeoutSeconds: Int = 1
        private set
        get() = appConfig.partnerInitTimeoutSeconds

    /**
     * Timeout for sending initialization metrics data to the server. This way we can avoid waiting
     * for a bad initialization that never completes, while also giving sufficient time for the majority of partners to finish.
     */
    var initializationMetricsPostTimeout: Int = 10
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

    fun getEnableRateLimiting(): Boolean {
        return HeliumSdk.context?.let {
            val preferences =
                it.getSharedPreferences("HELIUM_EXPERIMENTAL", Context.MODE_PRIVATE)
            preferences.getBoolean("com.chartboost.helium.enable_rate_limiting", true)
        } ?: true
    }

    fun setEnableRateLimiting(enableRateLimiting: Boolean) {
        if (!enableRateLimiting) LogController.d("Disabling rate limiting.")
        HeliumSdk.context?.let {
            val preferences =
                it.getSharedPreferences("HELIUM_EXPERIMENTAL", Context.MODE_PRIVATE)
            preferences?.edit()
                ?.putBoolean("com.chartboost.helium.enable_rate_limiting", enableRateLimiting)
                ?.apply()
        } ?: LogController.d("Unable to set rate limiting preference.")
    }

    /**
     * Update fields with values from the app config JSON, using their assigned values as defaults.
     *
     * @param response The app config String.
     */
    fun updateFields(appConfig: AppConfig) {
        this@AppConfigStorage.appConfig = appConfig.copy(
            metricsEvents = (getReportableMetricsEvents(
                EnumSet.allOf(Sdk.Event::class.java),
                appConfig.metricsEvents
            ))
        )
        // TODO make metrics events a separate enum set field in this class
        appConfig.credentials.jsonObject.let {
            partners = compilePartners(it)
        }

        appConfig.placements?.forEach {
            PlacementStorage.addRefreshTime(it.chartboostPlacement, it.autoRefreshRate)
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
        fullSet: EnumSet<Sdk.Event>,
        serverSet: Set<Sdk.Event>,
    ): EnumSet<Sdk.Event> {
        return if (serverSet.isEmpty()) {
            fullSet
        } else EnumSet.noneOf(Sdk.Event::class.java).apply {
            serverSet.forEach { add(it) }
        }
    }

    /**
     * Compile a list of partners from the app config.
     *
     * @return A List of [Partner] objects.
     */
    private fun compilePartners(credentials: JsonObject): Set<Partner> {
        return mutableSetOf<Partner>().apply {
            credentials.keys.forEach { partnerId ->
                add(Partner(partnerId, credentials.getValue(partnerId).jsonObject))
            }
        }
    }
}

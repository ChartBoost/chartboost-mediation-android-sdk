package com.chartboost.sdk.internal.di

import com.chartboost.sdk.Mediation
import com.chartboost.sdk.internal.Networking.requests.TrackingBodyBuilder
import com.chartboost.sdk.internal.Networking.requests.TrackingRequest
import com.chartboost.sdk.internal.clickthrough.ClickTracking
import com.chartboost.sdk.internal.clickthrough.ClickTrackingImpl
import com.chartboost.sdk.privacy.PrivacyApi
import com.chartboost.sdk.tracking.Environment
import com.chartboost.sdk.tracking.EventThrottler
import com.chartboost.sdk.tracking.EventTrackerExtensions
import com.chartboost.sdk.tracking.EventTrackerImpl
import com.chartboost.sdk.tracking.TrackingEventCache

internal interface TrackerComponent {
    val eventTracker: EventTrackerExtensions
    val eventThrottler: EventThrottler
    val trackingEventCache: TrackingEventCache
    val trackingBodyBuilder: TrackingBodyBuilder
    val environment: Environment
    val trackingRequest: TrackingRequest
}

internal class TrackerModule(
    androidComponent: Lazy<AndroidComponent>,
    applicationComponent: Lazy<ApplicationComponent>,
    privacyApi: Lazy<PrivacyApi>,
) : TrackerComponent {
    override val eventTracker: EventTrackerExtensions by lazy {
        EventTrackerImpl(
            throttler = lazy { eventThrottler },
            requestBodyBuilder = lazy { applicationComponent.value.requestBodyBuilder },
            config = lazy { applicationComponent.value.sdkConfig.get().trackingConfig },
            privacyApi = privacyApi,
            environment = lazy { environment },
            trackingRequest = lazy { trackingRequest },
            trackingEventCache = lazy { trackingEventCache },
        )
    }

    override val eventThrottler: EventThrottler by lazy {
        val config = applicationComponent.value.sdkConfig.get().trackingConfig
        EventThrottler(config.eventLimit, config.windowDuration)
    }

    override val trackingEventCache: TrackingEventCache by lazy {
        TrackingEventCache(
            androidComponent.value.trackingSharedPreferences,
            trackingBodyBuilder,
        )
    }

    override val trackingBodyBuilder: TrackingBodyBuilder by lazy {
        TrackingBodyBuilder()
    }

    override val environment: Environment by lazy {
        Environment(
            androidComponent.value.app,
            androidComponent.value.displayMeasurement,
        )
    }

    override val trackingRequest: TrackingRequest by lazy {
        TrackingRequest(
            applicationComponent.value.networkService,
            trackingEventCache,
            eventTracker = eventTracker,
        )
    }
}

internal fun getEventTracker(): EventTrackerExtensions = ChartboostDependencyContainer.trackerComponent.eventTracker

internal fun clickTracking(
    adType: String = "missing ad type",
    location: String = "missing location",
    mediation: Mediation? = null,
    eventTracker: EventTrackerExtensions,
): ClickTracking = ClickTrackingImpl(adType, location, mediation, eventTracker)

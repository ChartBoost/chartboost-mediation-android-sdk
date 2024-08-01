/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.controllers

import android.app.Activity
import android.content.Context
import android.util.Size
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize.Companion.asSize
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.network.Endpoints.Event.EXPIRATION
import com.chartboost.chartboostmediationsdk.network.Endpoints.Event.INITIALIZATION
import com.chartboost.chartboostmediationsdk.network.Endpoints.Event.LOAD
import com.chartboost.chartboostmediationsdk.network.Endpoints.Event.PREBID
import com.chartboost.chartboostmediationsdk.network.Endpoints.Event.SHOW
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.core.ChartboostCore
import com.chartboost.core.consent.ConsentKey
import com.chartboost.core.consent.ConsentObserver
import com.chartboost.core.consent.ConsentValue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule
import kotlin.system.measureTimeMillis

/**
 * @suppress
 */
class PartnerController {
    companion object {
        /**
         * Store adapter info (versions, names, et al.) keyed by the adapter partner IDs.
         */
        var adapterInfo: MutableMap<String, AdapterInfo> = mutableMapOf()
    }

    /**
     * Collection of partner adapter/SDK initialization statuses.
     */
    enum class PartnerInitializationStatus(
        val value: Int,
    ) {
        IDLE(0), // Initialization not started (the default state).
        INITIALIZING(1), // Initialization is in progress.
        INITIALIZED(2), // Initialization has been successfully completed.
        FAILED(3), // Initialization has failed.
        SKIPPED(4), // Initialization deliberately skipped by the publisher.
    }

    /**
     * Data class for partner ad show result for internal use.
     * For external use, see [com.chartboost.chartboostmediationsdk.ad.ChartboostMediationAdShowResult].
     */
    data class PartnerShowResult(
        val partnerAd: PartnerAd?,
        val metrics: Set<Metrics>,
    )

    /**
     * Store adapters keyed by their partner IDs.
     */
    var adapters: MutableMap<String, PartnerAdapter> = ConcurrentHashMap()

    /**
     * Return adapterInfo as a List
     */
    val allAdapterInfo
        get() = adapterInfo.values.toList()

    /**
     * Store partner initialization statuses keyed by the partner IDs.
     */
    var initStatuses: MutableMap<String, PartnerInitializationStatus> = mutableMapOf()

    /**
     * Compute the token fetching timeout in milliseconds.
     */
    private val prebidFetchTimeoutMs
        get() = AppConfigStorage.prebidFetchTimeoutSeconds * 1000L

    private val partnerControllerConsentObserver: ConsentObserver =
        object : ConsentObserver {
            override fun onConsentModuleReady(
                appContext: Context,
                initialConsents: Map<ConsentKey, ConsentValue>,
            ) {
                adapters.forEach { (_, adapter) ->
                    CoroutineScope(Main).launch {
                        try {
                            adapter.setConsents(appContext, initialConsents, initialConsents.keys)
                        } catch (e: Exception) {
                            LogController.e(
                                "Failed to send initial consents to adapter ${adapter.configuration.partnerDisplayName} due to $e.",
                            )
                        }
                    }
                }
            }

            override fun onConsentChange(
                appContext: Context,
                fullConsents: Map<ConsentKey, ConsentValue>,
                modifiedKeys: Set<ConsentKey>,
            ) {
                adapters.forEach { (_, adapter) ->
                    CoroutineScope(Main).launch {
                        try {
                            adapter.setConsents(appContext, fullConsents, modifiedKeys)
                        } catch (e: Exception) {
                            LogController.e(
                                "Failed to send consent change to adapter ${adapter.configuration.partnerDisplayName} due to $e.",
                            )
                        }
                    }
                }
            }
        }

    /**
     * Instantiate adapters for all supplied class names, and initialize them on the main thread.
     *
     * @param context The current [Context].
     * @param partnerConfigurationMap The map of partner configurations for initialization purposes.
     * @param adapterClasses A Set of server-side adapter classes to create.
     * @param skippedPartnerIds A Set of partners whose initialization should be skipped as per the publisher's request.
     * @param onPartnerInitializationComplete A callback to be invoked when the initialization is deemed complete.
     */
    fun setUpAdapters(
        context: Context,
        partnerConfigurationMap: Map<String, PartnerConfiguration>,
        adapterClasses: Set<String>,
        skippedPartnerIds: Set<String>,
        onPartnerInitializationComplete: (ChartboostMediationError?) -> Unit,
    ) {
        if (!requiredDataIsValid(context, adapterClasses)) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = INITIALIZATION,
                auctionIdentifier = null,
                chartboostMediationError = ChartboostMediationError.OtherError.InvalidArgument,
                chartboostMediationErrorMessage = ChartboostMediationError.OtherError.InvalidArgument.message,
                eventResult =
                    AppConfigStorage.parsingError?.let {
                        if (AppConfigStorage.validCachedConfigExists) {
                            EventResult.SdkInitializationResult.InitResult2B(it)
                        } else {
                            EventResult.SdkInitializationResult.InitResult1B(it)
                        }
                    },
            )

            onPartnerInitializationComplete(ChartboostMediationError.OtherError.InvalidArgument)
            return
        }

        var initCompletionReported = false

        createAdapters(adapterClasses)

        var setUpError: ChartboostMediationError? = null
        CoroutineScope(Main).launch(
            CoroutineExceptionHandler { _, error ->
                LogController.e("Failed to set up adapters: $error")
                setUpError = ChartboostMediationError.InitializationError.Exception
            },
        ) {
            // A subset of partners has not completed initialization within the allotted time.
            // Report init completion to Chartboost Mediation once the time is up anyway, but still let the
            // partners in question finish initialization (they are not cancelled).
            val timer =
                Timer().schedule(AppConfigStorage.partnerInitTimeoutSeconds * 1000L) {
                    if (!initCompletionReported) {
                        cancel()

                        initCompletionReported = true
                        onPartnerInitializationComplete(setUpError)
                    }
                }

            val metricsDataSet = mutableSetOf<Metrics>()

            skippedPartnerIds.forEach { partnerId ->
                val metrics = Metrics(partnerId, INITIALIZATION)
                metricsDataSet.add(metrics)

                initStatuses[partnerId] = PartnerInitializationStatus.SKIPPED

                metrics.start = System.currentTimeMillis()
                metrics.end = System.currentTimeMillis()
                metrics.duration = metrics.end?.minus(metrics.start ?: 0) ?: 0
                metrics.partnerSdkVersion = adapters[partnerId]?.configuration?.partnerSdkVersion ?: ""
                metrics.partnerAdapterVersion = adapters[partnerId]?.configuration?.adapterVersion ?: ""
                metrics.chartboostMediationError =
                    ChartboostMediationError.InitializationError.Skipped
                metrics.chartboostMediationErrorMessage =
                    ChartboostMediationError.InitializationError.Skipped.message

                // Remove adapters that are deliberately skipped by the publisher. This way, we
                // don't call other ad operations on them.
                adapters.remove(partnerId)
            }

            Timer().schedule(AppConfigStorage.initializationMetricsPostTimeout * 1000L) {
                MetricsManager.postMetricsData(
                    metricsDataSet,
                    eventResult =
                        AppConfigStorage.parsingError?.let {
                            EventResult.SdkInitializationResult.InitResult2B(it)
                        } ?: if (AppConfigStorage.validCachedConfigExists) {
                            EventResult.SdkInitializationResult.InitResult2A
                        } else {
                            EventResult.SdkInitializationResult.InitResult1A
                        },
                )
                cancel()
            }

            // Concurrently initialize all partners.
            adapters.keys
                .map { partnerId ->
                    val metrics = Metrics(partnerId, INITIALIZATION)
                    metricsDataSet.add(metrics)

                    async {
                        measureTimeMillis {
                            adapters[partnerId]?.let { adapter ->
                                partnerConfigurationMap[adapter.configuration.partnerId]?.let {
                                    metrics.start = System.currentTimeMillis()
                                    setUp(context, adapter, it, metrics)
                                }
                            }
                        }.also {
                            metrics.duration = it
                            metrics.partnerSdkVersion = adapterInfo[partnerId]?.partnerVersion ?: ""
                            metrics.partnerAdapterVersion =
                                adapterInfo[partnerId]?.adapterVersion ?: ""
                        }
                    }
                }.awaitAll()

            // All partners have been successfully initialized within the allotted timeout.
            // Immediately report init completion to Chartboost Mediation.
            if (!initCompletionReported) {
                timer.cancel()

                initCompletionReported = true
                onPartnerInitializationComplete(setUpError)
            }
        }

        ChartboostCore.consent.removeObserver(partnerControllerConsentObserver)
        ChartboostCore.consent.addObserver(partnerControllerConsentObserver)
    }

    /**
     * Notify all adapters and partner SDKs of the COPPA applicability so they can take appropriate action.
     *
     * @param context The context to use for the call.
     * @param isUserUnderage True if the user is subject to COPPA, false otherwise.
     */
    fun setIsUserUnderage(
        context: Context,
        isUserUnderage: Boolean,
    ) {
        adapters.forEach { (_, adapter) ->
            try {
                adapter.setIsUserUnderage(context, isUserUnderage)
            } catch (ignored: Exception) {
                LogController.e("Failed to route setIsUserUnderage to adapter ${adapter.configuration.partnerDisplayName}")
            }
        }
    }

    /**
     * Issue calls to adapters to ask the partner SDKs for network bidding tokens.
     * Note that calls are made concurrently and we'll wait for all of them to finish (within the timeout)
     * before moving on. If 1 or more adapters time out, the others will not be affected.
     *
     * @param context The context to use for the call.
     * @param request The [PartnerAdPreBidRequest] instance containing data necessary for this operation.
     */
    suspend fun routeGetBidderInformation(
        context: Context,
        request: PartnerAdPreBidRequest,
    ): Map<String, Map<String, String>> {
        val bidTokens: ConcurrentHashMap<String, Map<String, String>> = ConcurrentHashMap()
        if (!requiredDataIsValid(context, request)) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = PREBID,
                auctionIdentifier = null,
                chartboostMediationError = ChartboostMediationError.OtherError.InvalidArgument,
                chartboostMediationErrorMessage = ChartboostMediationError.OtherError.InvalidArgument.message,
                loadId = request.loadId,
            )
            return bidTokens
        }

        val bidJob =
            CoroutineScope(Main).launch(
                CoroutineExceptionHandler { _, error ->
                    MetricsManager.postMetricsDataForFailedEvent(
                        partner = null,
                        event = PREBID,
                        auctionIdentifier = null,
                        chartboostMediationError =
                            (error as? ChartboostMediationAdException)?.chartboostMediationError
                                ?: ChartboostMediationError.PrebidError.Exception,
                        chartboostMediationErrorMessage = error.message,
                        loadId = request.loadId,
                    )
                },
            ) {
                val metricsDataSet = mutableSetOf<Metrics>()

                withContext(IO) {
                    withTimeoutOrNull(prebidFetchTimeoutMs) {
                        adapters.keys
                            .map { partnerId ->
                                val metrics = Metrics(partnerId, PREBID)
                                metricsDataSet.add(metrics)

                                async {
                                    measureTimeMillis {
                                        adapters[partnerId]?.let { adapter ->
                                            metrics.start = System.currentTimeMillis()

                                            fun endMetricsInError(
                                                metrics: Metrics,
                                                exception: Throwable,
                                            ) {
                                                metrics.end = System.currentTimeMillis()
                                                metrics.isSuccess = false
                                                metrics.chartboostMediationError =
                                                    (exception as? ChartboostMediationAdException)?.chartboostMediationError
                                                        ?: ChartboostMediationError.PrebidError.Exception
                                                metrics.chartboostMediationErrorMessage = exception.message
                                            }

                                            bidTokens[partnerId] =
                                                try {
                                                    adapter
                                                        .fetchBidderInformation(context, request)
                                                        .let { result ->
                                                            result.fold(
                                                                {
                                                                    // For token fetches, empty token (bidding not supported)
                                                                    // is still a success.
                                                                    metrics.end = System.currentTimeMillis()
                                                                    metrics.isSuccess = true

                                                                    it
                                                                },
                                                                { exception ->
                                                                    endMetricsInError(metrics, exception)

                                                                    emptyMap()
                                                                },
                                                            )
                                                        }
                                                } catch (e: Exception) {
                                                    endMetricsInError(metrics, e)

                                                    emptyMap()
                                                }
                                        }
                                    }.also {
                                        metrics.duration = it
                                    }
                                }
                            }.awaitAll()
                    }
                }

                MetricsManager.postMetricsData(metricsDataSet, request.loadId)
            }
        bidJob.join()
        return bidTokens
    }

    /**
     * Issue a call to the partner adapter to load the given ad.
     *
     * @param context The context to use for the call.
     * @param auctionId The current auction ID.
     * @param lineItemId The current line item ID.
     * @param isMediation True if this is a mediation request, false if it's a bidding one.
     * @param request The [PartnerAdLoadRequest] instance containing data necessary for this operation.
     */
    internal suspend fun routeLoad(
        context: Context,
        auctionId: String,
        lineItemId: String?,
        isMediation: Boolean,
        request: PartnerAdLoadRequest,
        loadMetricsSet: MutableSet<Metrics>,
        placementType: String?,
    ): Result<PartnerAd> {
        if (!requiredDataIsValid(context, request.partnerPlacement, request.mediationPlacement)) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = request.partnerId,
                event = LOAD,
                auctionIdentifier = auctionId,
                chartboostMediationError = ChartboostMediationError.OtherError.InvalidArgument,
                chartboostMediationErrorMessage = ChartboostMediationError.OtherError.InvalidArgument.message,
                loadId = request.identifier,
                placementType = placementType,
                size = request.bannerSize?.asSize()?.takeIf { request.bannerSize.isAdaptive },
                networkType = Metrics.NetworkType.getType(isMediation),
                lineItemId = lineItemId,
                partnerPlacement = request.partnerPlacement,
            )
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.OtherError.InvalidArgument))
        }

        var result: Result<PartnerAd> =
            Result.failure(ChartboostMediationAdException(ChartboostMediationError.OtherError.Unknown))
        val job =
            CoroutineScope(Main).launch(
                CoroutineExceptionHandler { _, error ->
                    MetricsManager.postMetricsDataForFailedEvent(
                        partner = request.partnerId,
                        event = LOAD,
                        auctionIdentifier = auctionId,
                        chartboostMediationError =
                            (error as? ChartboostMediationAdException)?.chartboostMediationError
                                ?: ChartboostMediationError.LoadError.Exception,
                        chartboostMediationErrorMessage = error.message,
                        loadId = request.identifier,
                        placementType = placementType,
                        size = request.bannerSize?.asSize()?.takeIf { request.bannerSize.isAdaptive },
                        networkType = Metrics.NetworkType.getType(isMediation),
                        lineItemId = lineItemId,
                        partnerPlacement = request.partnerPlacement,
                    )
                    result =
                        Result.failure(ChartboostMediationAdException(ChartboostMediationError.LoadError.Exception))
                },
            ) {
                withContext(IO) {
                    withTimeoutOrNull(getLoadTimeoutMs(request.format)) {
                        adapters[request.partnerId]?.let { adapter ->
                            val metrics = Metrics(adapter.configuration.partnerId, LOAD)

                            // Since it's possible for a Chartboost Mediation placement to consist of multiple line items
                            // at load time and we are tracking metrics data per line item, we need to batch data for
                            // the same placement in the same dataset.
                            loadMetricsSet.add(metrics)

                            metrics.auctionId = auctionId
                            metrics.lineItemId = lineItemId
                            metrics.networkType = Metrics.NetworkType.getType(isMediation)
                            metrics.partnerPlacement = request.partnerPlacement
                            metrics.placementType = placementType
                            if (request.bannerSize?.isAdaptive == true) {
                                metrics.size =
                                    Size(
                                        request.bannerSize.width,
                                        request.bannerSize.height,
                                    )
                            }

                            measureTimeMillis {
                                withContext(Main) {
                                    metrics.start = System.currentTimeMillis()

                                    try {
                                        adapter
                                            .load(
                                                context,
                                                request,
                                                createPartnerAdListener(
                                                    request.adInteractionListener,
                                                    auctionId,
                                                ),
                                            ).let {
                                                handleLoadResult(it, metrics)
                                                result = it
                                            }
                                    } catch (e: Exception) {
                                        metrics.end = System.currentTimeMillis()
                                        metrics.isSuccess = false
                                        metrics.chartboostMediationError =
                                            (e as? ChartboostMediationAdException)?.chartboostMediationError
                                                ?: ChartboostMediationError.LoadError.Exception
                                        metrics.chartboostMediationErrorMessage = e.message

                                        result =
                                            Result.failure(
                                                ChartboostMediationAdException(
                                                    ChartboostMediationError.LoadError.Exception,
                                                ),
                                            )
                                    }
                                }
                            }.also {
                                metrics.duration = it
                            }
                        } ?: run {
                            MetricsManager.postMetricsDataForFailedEvent(
                                partner = request.partnerId,
                                event = LOAD,
                                auctionIdentifier = auctionId,
                                chartboostMediationError = ChartboostMediationError.LoadError.AdapterNotFound,
                                chartboostMediationErrorMessage = ChartboostMediationError.LoadError.AdapterNotFound.message,
                                loadId = request.identifier,
                                placementType = placementType,
                                size = request.bannerSize?.asSize()?.takeIf { request.bannerSize.isAdaptive },
                            )

                            result =
                                Result.failure(ChartboostMediationAdException(ChartboostMediationError.LoadError.AdapterNotFound))
                        }
                    } ?: run {
                        MetricsManager.postMetricsDataForFailedEvent(
                            partner = request.partnerId,
                            event = LOAD,
                            auctionIdentifier = auctionId,
                            chartboostMediationError = ChartboostMediationError.LoadError.AdRequestTimeout,
                            chartboostMediationErrorMessage = ChartboostMediationError.LoadError.AdRequestTimeout.message,
                            loadId = request.identifier,
                            placementType = placementType,
                            size = request.bannerSize?.asSize()?.takeIf { request.bannerSize.isAdaptive },
                            networkType = Metrics.NetworkType.getType(isMediation),
                            lineItemId = lineItemId,
                            partnerPlacement = request.partnerPlacement,
                        )

                        result =
                            Result.failure(ChartboostMediationAdException(ChartboostMediationError.LoadError.AdRequestTimeout))
                    }
                }
            }
        job.join()
        return result
    }

    /**
     * Issue a call to the partner adapter to show the given ad.
     *
     * @param activity The current [Activity].
     * @param partnerAd The [PartnerAd] to attempt to show.
     * @param auctionIdentifier The current auction ID.
     */
    suspend fun routeShow(
        activity: Activity,
        partnerAd: PartnerAd?,
        auctionIdentifier: String,
        loadId: String,
    ): PartnerShowResult {
        var internalAdShowResult =
            PartnerShowResult(
                partnerAd = partnerAd,
                metrics =
                    setOf(
                        Metrics(partnerAd?.request?.partnerId, SHOW).apply {
                            isSuccess = false
                            auctionId = auctionIdentifier
                            chartboostMediationError = ChartboostMediationError.ShowError.Unknown
                            chartboostMediationErrorMessage =
                                ChartboostMediationError.ShowError.Unknown.message
                        },
                    ),
            )

        if (!requiredDataIsValid(activity) || partnerAd == null) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = SHOW,
                auctionIdentifier = auctionIdentifier,
                chartboostMediationError = ChartboostMediationError.OtherError.InvalidArgument,
                chartboostMediationErrorMessage = ChartboostMediationError.OtherError.InvalidArgument.message,
                loadId = loadId,
                partnerPlacement = partnerAd?.request?.partnerPlacement,
            )

            internalAdShowResult.metrics.first().chartboostMediationError =
                ChartboostMediationError.OtherError.InvalidArgument
            internalAdShowResult.metrics.first().chartboostMediationErrorMessage =
                ChartboostMediationError.OtherError.InvalidArgument.message

            return internalAdShowResult
        }

        val partnerShowJob =
            CoroutineScope(Main).launch(
                CoroutineExceptionHandler { _, error ->
                    MetricsManager.postMetricsDataForFailedEvent(
                        partner = partnerAd.request.partnerId,
                        event = SHOW,
                        auctionIdentifier = auctionIdentifier,
                        chartboostMediationError = ChartboostMediationError.ShowError.Exception,
                        chartboostMediationErrorMessage =
                            error.message
                                ?: ChartboostMediationError.ShowError.Exception.message,
                        partnerPlacement = partnerAd.request.partnerPlacement,
                    )

                    internalAdShowResult =
                        PartnerShowResult(
                            partnerAd = partnerAd,
                            metrics =
                                setOf(
                                    Metrics(partnerAd.request.partnerId, SHOW).apply {
                                        isSuccess = false
                                        auctionId = auctionIdentifier
                                        chartboostMediationError = ChartboostMediationError.ShowError.Exception
                                        chartboostMediationErrorMessage =
                                            error.message ?: ChartboostMediationError.ShowError.Exception.message
                                    },
                                ),
                        )
                },
            ) {
                val metricsDataSet = mutableSetOf<Metrics>()
                val metrics = Metrics(partnerAd.request.partnerId, SHOW)
                metricsDataSet.add(metrics)

                adapters[partnerAd.request.partnerId]?.let { adapter ->
                    metrics.auctionId = auctionIdentifier

                    withTimeoutOrNull(AppConfigStorage.showTimeoutSeconds * 1000L) {
                        metrics.start = System.currentTimeMillis()

                        measureTimeMillis {
                            try {
                                handleShowResult(adapter.show(activity, partnerAd), metrics)
                            } catch (e: Exception) {
                                metrics.end = System.currentTimeMillis()
                                metrics.isSuccess = false
                                metrics.chartboostMediationError =
                                    ChartboostMediationError.ShowError.Exception
                                metrics.chartboostMediationErrorMessage =
                                    e.message
                                        ?: ChartboostMediationError.ShowError.Exception.message
                            }
                        }.also {
                            metrics.duration = it
                        }
                    }
                }

                internalAdShowResult =
                    PartnerShowResult(
                        partnerAd = partnerAd,
                        metrics = metricsDataSet,
                    )

                MetricsManager.postMetricsData(metricsDataSet, loadId)
            }
        partnerShowJob.join()
        return internalAdShowResult
    }

    /**
     * Issue a call to the partner adapter to invalidate the given ad.
     *
     * @param partnerAd The [PartnerAd] for which to invalidate the ad.
     */
    fun routeInvalidate(partnerAd: PartnerAd) {
        adapters[partnerAd.request.partnerId]?.let { adapter ->
            CoroutineScope(Main).launch(
                CoroutineExceptionHandler { _, error ->
                    LogController.e(
                        "Invalidation failed for ${partnerAd.request.partnerId} and Chartboost Mediation" +
                            "placement ${partnerAd.request.mediationPlacement}. Error: ${error.message}.",
                    )
                },
            ) {
                adapter.invalidate(partnerAd)
            }
        }
    }

    /**
     * Use reflection to create a new adapter instance for each partner. If there's a problem with a
     * subset of the class names, the others will not be affected.
     *
     * @param classNames A Set of class names for which to instantiate adapters.
     */
    private fun createAdapters(classNames: Set<String>) {
        classNames.forEach { name ->
            try {
                val adapterClass = Class.forName(name)
                val adapter = adapterClass.newInstance() as PartnerAdapter

                adapters[adapter.configuration.partnerId] = adapter
                initStatuses[adapter.configuration.partnerId] = PartnerInitializationStatus.IDLE
            } catch (exception: Exception) {
                LogController.e(
                    "Failed to create adapter $name. Error: ${exception.message}." +
                        "The associated network will not be initialized.",
                )
            }
        }
    }

    /**
     * Set up the given adapter and initialize the partner SDK with the provided details.
     * Note that this method also handles timeout in the `catch` block.
     *
     * @param context The current [Context].
     * @param adapter The adapter to initialize.
     * @param partnerConfiguration A map containing partner-specific data for initialization.
     */
    private suspend fun setUp(
        context: Context,
        adapter: PartnerAdapter,
        partnerConfiguration: PartnerConfiguration,
        metrics: Metrics,
    ) {
        initStatuses[adapter.configuration.partnerId] = PartnerInitializationStatus.INITIALIZING

        try {
            adapter.setUp(context, partnerConfiguration).let {
                metrics.end = System.currentTimeMillis()
                metrics.duration = metrics.end?.minus(metrics.start ?: 0) ?: 0

                handleSetupResult(it, adapter, metrics)
            }
        } catch (exception: Exception) {
            metrics.end = System.currentTimeMillis()
            metrics.duration = metrics.end?.minus(metrics.start ?: 0) ?: 0
            metrics.isSuccess = false
            metrics.chartboostMediationError =
                (exception as? ChartboostMediationAdException)?.chartboostMediationError
                    ?: ChartboostMediationError.InitializationError.Exception
            metrics.chartboostMediationErrorMessage = exception.message

            initStatuses[adapter.configuration.partnerId] = PartnerInitializationStatus.FAILED
        }
    }

    /**
     * Handle a successful/failed partner SDK initialization attempt.
     *
     * @param result The result of the initialization attempt.
     * @param adapter The adapter that attempted to initialize.
     */
    private fun handleSetupResult(
        result: Result<Map<String, Any>>,
        adapter: PartnerAdapter,
        metrics: Metrics,
    ) {
        initStatuses[adapter.configuration.partnerId] =
            if (result.isSuccess) {
                // Preferred to be called after a successful setup as select partner SDKs might require
                // initialization first before version (and other data) are made available.
                setUpAdapterInfo(adapter)

                metrics.isSuccess = true
                PartnerInitializationStatus.INITIALIZED
            } else {
                // Remove adapters that failed to initialize so that we don't call other operations
                // (token fetch, load, show) on them. Note that we still wait for adapters that don't finish
                // initialization within the timeout period (their result will be handled here as well).
                adapters.remove(adapter.configuration.partnerId)

                val chartboostMediationError =
                    (result.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                        ?: ChartboostMediationError.InitializationError.Unknown

                metrics.isSuccess = false
                metrics.chartboostMediationError = chartboostMediationError
                metrics.chartboostMediationErrorMessage = chartboostMediationError.message

                PartnerInitializationStatus.FAILED
            }

        metrics.partnerSdkVersion = adapterInfo[adapter.configuration.partnerId]?.partnerVersion ?: ""
        metrics.partnerAdapterVersion = adapterInfo[adapter.configuration.partnerId]?.adapterVersion ?: ""
    }

    /**
     * Create and store an [AdapterInfo] instance for the given adapter.
     *
     * @param adapter The adapter for which to create an [AdapterInfo].
     */
    private fun setUpAdapterInfo(adapter: PartnerAdapter) {
        try {
            adapterInfo[adapter.configuration.partnerId] =
                AdapterInfo(
                    partnerVersion = adapter.configuration.partnerSdkVersion,
                    adapterVersion = adapter.configuration.adapterVersion,
                    partnerId = adapter.configuration.partnerId,
                    partnerDisplayName = adapter.configuration.partnerDisplayName,
                )
        } catch (exception: Exception) {
            LogController.e(
                "Failed to make AdapterInfo for " + "${adapter.configuration.partnerDisplayName}. Its version data will not be available." +
                    " Error: ${
                        (exception as? ChartboostMediationAdException)?.chartboostMediationError ?: ChartboostMediationError.InitializationError.Exception
                    }",
            )
        }
    }

    /**
     * Handle the result of the current ad load call. Note that this method does not handle timeout.
     *
     * @param result The result of the ad load call.
     * @param metrics The [Metrics] instance for the current ad load call.
     */
    private fun handleLoadResult(
        result: Result<PartnerAd>,
        metrics: Metrics,
    ) {
        metrics.end = System.currentTimeMillis()
        metrics.isSuccess = result.isSuccess

        if (!result.isSuccess) {
            val chartboostMediationError =
                (result.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                    ?: ChartboostMediationError.LoadError.Unknown

            metrics.chartboostMediationError = chartboostMediationError
            metrics.chartboostMediationErrorMessage = chartboostMediationError.message
        }
    }

    /**
     * Get the load timeout (in ms) for the given ad format.
     *
     * @param format The current [PartnerAdFormat].
     *
     * @return The timeout (in ms) for the given ad format.
     */
    private fun getLoadTimeoutMs(format: PartnerAdFormat): Long =
        when (format) {
            PartnerAdFormats.BANNER -> AppConfigStorage.bannerLoadTimeoutSeconds * 1000L

            PartnerAdFormats.INTERSTITIAL, PartnerAdFormats.REWARDED, PartnerAdFormats.REWARDED_INTERSTITIAL ->
                AppConfigStorage.fullscreenLoadTimeoutSeconds *
                    1000L

            else -> {
                LogController.e("Unknown ad format: $format. Using default timeout.")
                AppConfigStorage.fullscreenLoadTimeoutSeconds * 1000L
            }
        }

    /**
     * Create and listen to partner ad events.
     *
     * @param adInteractionListener The [AdInteractionListener] to notify of partner ad events.
     * @param auctionId The auction ID for the current ad load call.
     */
    private fun createPartnerAdListener(
        adInteractionListener: AdInteractionListener,
        auctionId: String,
    ): PartnerAdListener =
        object : PartnerAdListener {
            override fun onPartnerAdImpression(partnerAd: PartnerAd) {
                adInteractionListener.onImpressionTracked(partnerAd)
            }

            override fun onPartnerAdClicked(partnerAd: PartnerAd) {
                adInteractionListener.onClicked(partnerAd)
            }

            override fun onPartnerAdRewarded(partnerAd: PartnerAd) {
                adInteractionListener.onRewarded(partnerAd)
            }

            override fun onPartnerAdDismissed(
                partnerAd: PartnerAd,
                error: ChartboostMediationAdException?,
            ) {
                adInteractionListener.onDismissed(partnerAd, error)
            }

            override fun onPartnerAdExpired(partnerAd: PartnerAd) {
                adInteractionListener.onExpired(partnerAd)

                MetricsManager.postMetricsData(
                    setOf(
                        Metrics(
                            partnerAd.request.partnerId,
                            EXPIRATION,
                        ).apply {
                            this.auctionId = auctionId
                        },
                    ),
                )
            }
        }

    /**
     * Handle the show result of the current ad show call.
     *
     * @param result The result of the ad show call.
     * @param metrics The metrics to update with the show result.
     */
    private fun handleShowResult(
        result: Result<PartnerAd>,
        metrics: Metrics,
    ) {
        metrics.end = System.currentTimeMillis()
        metrics.isSuccess = result.isSuccess
        if (!result.isSuccess) {
            (result.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                ?: ChartboostMediationError.ShowError.Unknown.apply {
                    metrics.chartboostMediationError = this
                    metrics.chartboostMediationErrorMessage = this.message
                }
        }
    }

    /**
     * Validate the given collection of data for emptiness and nullability. The result applies to
     * the collection as a whole (all entries must be valid for the collection to be valid).
     *
     * Note: Do NOT use this method for any data that can optionally be empty or null.
     *
     * @param data The collection of data to validate.
     *
     * @return True if the whole collection is valid, false otherwise.
     */
    private fun requiredDataIsValid(vararg data: Any?): Boolean =
        data.all {
            when (it) {
                is Collection<*> -> {
                    it.isNotEmpty()
                }

                is String -> {
                    it.trim().isNotEmpty()
                }

                else -> {
                    it != null
                }
            }
        }
}

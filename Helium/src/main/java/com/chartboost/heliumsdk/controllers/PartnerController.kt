/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.controllers

import android.content.Context
import android.util.Size
import com.chartboost.heliumsdk.PartnerConsents
import com.chartboost.heliumsdk.domain.AdFormat
import com.chartboost.heliumsdk.domain.AdInteractionListener
import com.chartboost.heliumsdk.domain.AdapterInfo
import com.chartboost.heliumsdk.domain.AppConfigStorage
import com.chartboost.heliumsdk.domain.ChartboostMediationAdException
import com.chartboost.heliumsdk.domain.ChartboostMediationError
import com.chartboost.heliumsdk.domain.EventResult
import com.chartboost.heliumsdk.domain.GdprConsentStatus
import com.chartboost.heliumsdk.domain.Metrics
import com.chartboost.heliumsdk.domain.MetricsManager
import com.chartboost.heliumsdk.domain.PartnerAd
import com.chartboost.heliumsdk.domain.PartnerAdListener
import com.chartboost.heliumsdk.domain.PartnerAdLoadRequest
import com.chartboost.heliumsdk.domain.PartnerAdapter
import com.chartboost.heliumsdk.domain.PartnerConfiguration
import com.chartboost.heliumsdk.domain.PreBidRequest
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event.EXPIRATION
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event.INITIALIZATION
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event.LOAD
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event.PREBID
import com.chartboost.heliumsdk.network.Endpoints.Sdk.Event.SHOW
import com.chartboost.heliumsdk.utils.LogController
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
    enum class PartnerInitializationStatus(val value: Int) {
        IDLE(0), // Initialization not started (the default state).
        INITIALIZING(1), // Initialization is in progress.
        INITIALIZED(2), // Initialization has been successfully completed.
        FAILED(3), // Initialization has failed.
        SKIPPED(4) // Initialization deliberately skipped by the publisher.
    }

    /**
     * Data class for partner ad show result for internal use.
     * For external use, see [com.chartboost.heliumsdk.ad.ChartboostMediationAdShowResult].
     */
    data class PartnerShowResult(
        val partnerAd: PartnerAd?,
        val metrics: Set<Metrics>,
    )

    /**
     * Store adapters keyed by their partner IDs.
     */
    var adapters: MutableMap<String, PartnerAdapter> = mutableMapOf()

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
        onPartnerInitializationComplete: (ChartboostMediationError?) -> Unit
    ) {
        if (!requiredDataIsValid(context, adapterClasses)) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = INITIALIZATION,
                auctionIdentifier = null,
                chartboostMediationError = ChartboostMediationError.CM_INVALID_ARGUMENTS,
                chartboostMediationErrorMessage = ChartboostMediationError.CM_INVALID_ARGUMENTS.message,
                eventResult = AppConfigStorage.parsingError?.let {
                    if(AppConfigStorage.validCachedConfigExists) {
                        EventResult.SdkInitializationResult.InitResult2B(it)
                    } else {
                        EventResult.SdkInitializationResult.InitResult1B(it)
                    }
                }
            )

            onPartnerInitializationComplete(ChartboostMediationError.CM_INVALID_ARGUMENTS)
            return
        }

        var initCompletionReported = false

        createAdapters(adapterClasses)

        var setUpError: ChartboostMediationError? = null
        CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            LogController.e("Failed to set up adapters: $error")
            setUpError = ChartboostMediationError.CM_INITIALIZATION_FAILURE_EXCEPTION
        }) {
            // A subset of partners has not completed initialization within the allotted time.
            // Report init completion to Chartboost Mediation once the time is up anyway, but still let the
            // partners in question finish initialization (they are not cancelled).
            val timer = Timer().schedule(AppConfigStorage.partnerInitTimeoutSeconds * 1000L) {
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
                metrics.partnerSdkVersion = adapters[partnerId]?.partnerSdkVersion ?: ""
                metrics.partnerAdapterVersion = adapters[partnerId]?.adapterVersion ?: ""
                metrics.chartboostMediationError =
                    ChartboostMediationError.CM_INITIALIZATION_SKIPPED
                metrics.chartboostMediationErrorMessage =
                    ChartboostMediationError.CM_INITIALIZATION_SKIPPED.message

                // Remove adapters that are deliberately skipped by the publisher. This way, we
                // don't call other ad operations on them.
                adapters.remove(partnerId)
            }

            Timer().schedule(AppConfigStorage.initializationMetricsPostTimeout * 1000L) {
                MetricsManager.postMetricsData(
                    metricsDataSet,
                    eventResult = AppConfigStorage.parsingError?.let {
                        EventResult.SdkInitializationResult.InitResult2B(it)
                    } ?: if(AppConfigStorage.validCachedConfigExists) {
                        EventResult.SdkInitializationResult.InitResult2A
                    } else {
                        EventResult.SdkInitializationResult.InitResult1A
                    }
                )
                cancel()
            }

            // Concurrently initialize all partners.
            adapters.keys.map { partnerId ->
                val metrics = Metrics(partnerId, INITIALIZATION)
                metricsDataSet.add(metrics)

                async {
                    measureTimeMillis {
                        adapters[partnerId]?.let { adapter ->
                            partnerConfigurationMap[adapter.partnerId]?.let {
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
    }

    /**
     * Notify all adapters and partner SDKs of the GDPR applicability and the user consent status
     * so they can take appropriate action.
     *
     * @param context The current [Context].
     * @param applies True if the user is subject to GDPR, false if the user is not subject to GDPR.
     * @param status The user's [GdprConsentStatus] consent status.
     * @param partnerConsents Per-partner consents.
     */
    fun setGdpr(context: Context, applies: Boolean?, status: GdprConsentStatus, partnerConsents: PartnerConsents) {
        val partnerIdToConsentMap = partnerConsents.getPartnerIdToConsentGivenMapCopy()
        adapters.forEach { (_, adapter) ->
            try {
                when(partnerIdToConsentMap[adapter.partnerId]) {
                    true -> adapter.setGdpr(context, applies, GdprConsentStatus.GDPR_CONSENT_GRANTED)
                    false -> adapter.setGdpr(context, applies, GdprConsentStatus.GDPR_CONSENT_DENIED)
                    null -> adapter.setGdpr(context, applies, status)
                }
            } catch (ignored: Exception) {
                LogController.e("Failed to route setGdpr to adapter ${adapter.partnerDisplayName}.")
            }
        }
    }

    /**
     * Notify all adapters and partner SDKs of the CCPA consent status and the CCPA privacy string
     * so they can take appropriate action.
     *
     * @param context The context to use for the call.
     * @param hasGrantedCcpaConsent Whether or not CCPA consent has been granted
     * @param privacyString The CCPA privacy string.
     * @param partnerConsents Per-partner consents.
     */
    fun setCcpaConsent(context: Context, hasGrantedCcpaConsent: Boolean?, privacyString: String, partnerConsents: PartnerConsents) {
        val partnerIdToConsentMap = partnerConsents.getPartnerIdToConsentGivenMapCopy()
        adapters.forEach { (_, adapter) ->
            try {
                if (partnerIdToConsentMap.containsKey(adapter.partnerId)) {
                    if (partnerIdToConsentMap[adapter.partnerId] == true) {
                        adapter.setCcpaConsent(
                            context,
                            true,
                            PrivacyController.PrivacyString.GRANTED.consentString
                        )
                    } else {
                        adapter.setCcpaConsent(
                            context,
                            false,
                            PrivacyController.PrivacyString.DENIED.consentString
                        )
                    }
                } else if (hasGrantedCcpaConsent != null) {
                    adapter.setCcpaConsent(context, hasGrantedCcpaConsent, privacyString)
                }
                // if CCPA has not been set on a per-partner basis or globally, do nothing.
            } catch (ignored: Exception) {
                LogController.e("Failed to route setCcpaPrivacyString to adapter ${adapter.partnerDisplayName}")
            }
        }
    }

    /**
     * Notify all adapters and partner SDKs of the COPPA applicability so they can take appropriate action.
     *
     * @param context The context to use for the call.
     * @param isSubjectToCoppa True if the user is subject to COPPA, false otherwise.
     */
    fun setUserSubjectToCoppa(context: Context, isSubjectToCoppa: Boolean) {
        adapters.forEach { (_, adapter) ->
            try {
                adapter.setUserSubjectToCoppa(context, isSubjectToCoppa)
            } catch (ignored: Exception) {
                LogController.e("Failed to route setUserSubjectToCoppa to adapter ${adapter.partnerDisplayName}")
            }
        }
    }

    /**
     * Issue calls to adapters to ask the partner SDKs for network bidding tokens.
     * Note that calls are made concurrently and we'll wait for all of them to finish (within the timeout)
     * before moving on. If 1 or more adapters time out, the others will not be affected.
     *
     * @param context The context to use for the call.
     * @param request The [PreBidRequest] instance containing data necessary for this operation.
     */
    suspend fun routeGetBidderInformation(
        context: Context,
        request: PreBidRequest
    ): Map<String, Map<String, String>> {
        val bidTokens: ConcurrentHashMap<String, Map<String, String>> = ConcurrentHashMap()
        if (!requiredDataIsValid(context, request)) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = PREBID,
                auctionIdentifier = null,
                chartboostMediationError = ChartboostMediationError.CM_INVALID_ARGUMENTS,
                chartboostMediationErrorMessage = ChartboostMediationError.CM_INVALID_ARGUMENTS.message,
                loadId = request.loadId
            )
            return bidTokens
        }

        val bidJob = CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = PREBID,
                auctionIdentifier = null,
                chartboostMediationError = (error as? ChartboostMediationAdException)?.chartboostMediationError
                    ?: ChartboostMediationError.CM_PREBID_FAILURE_EXCEPTION,
                chartboostMediationErrorMessage = error.message,
                loadId = request.loadId
            )
        }) {
            val metricsDataSet = mutableSetOf<Metrics>()

            withContext(IO) {
                withTimeoutOrNull(prebidFetchTimeoutMs) {
                    adapters.keys.map { partnerId ->
                        val metrics = Metrics(partnerId, PREBID)
                        metricsDataSet.add(metrics)

                        async {
                            measureTimeMillis {
                                adapters[partnerId]?.let { adapter ->
                                    metrics.start = System.currentTimeMillis()

                                    bidTokens[partnerId] = try {
                                        adapter.fetchBidderInformation(context, request)
                                            .let { result ->
                                                // For token fetches, empty token (bidding not supported)
                                                // is still a success. Failure is indicated by an exception.
                                                metrics.end = System.currentTimeMillis()
                                                metrics.isSuccess = true

                                                result
                                            }
                                    } catch (e: Exception) {
                                        metrics.end = System.currentTimeMillis()
                                        metrics.isSuccess = false
                                        metrics.chartboostMediationError =
                                            (e as? ChartboostMediationAdException)?.chartboostMediationError
                                                ?: ChartboostMediationError.CM_PREBID_FAILURE_EXCEPTION
                                        metrics.chartboostMediationErrorMessage = e.message

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
        placementType: String?
    ): Result<PartnerAd> {
        if (!requiredDataIsValid(context, request.partnerPlacement, request.chartboostPlacement)) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = request.partnerId,
                event = LOAD,
                auctionIdentifier = auctionId,
                chartboostMediationError = ChartboostMediationError.CM_INVALID_ARGUMENTS,
                chartboostMediationErrorMessage = ChartboostMediationError.CM_INVALID_ARGUMENTS.message,
                loadId = request.identifier,
                placementType = placementType,
                size = request.size?.takeIf { (placementType == "adaptive_banner") }
            )
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_INVALID_ARGUMENTS))
        }

        var result: Result<PartnerAd> =
            Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_UNKNOWN_ERROR))
        val job = CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            MetricsManager.postMetricsDataForFailedEvent(
                partner = request.partnerId,
                event = LOAD,
                auctionIdentifier = auctionId,
                chartboostMediationError = (error as? ChartboostMediationAdException)?.chartboostMediationError
                    ?: ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION,
                chartboostMediationErrorMessage = error.message,
                loadId = request.identifier,
                placementType = placementType,
                size = request.size?.takeIf { (placementType == "adaptive_banner") }
            )
            result =
                Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION))
        }) {
            withContext(IO) {
                withTimeoutOrNull(getLoadTimeoutMs(request.format)) {
                    adapters[request.partnerId]?.let { adapter ->
                        val metrics = Metrics(adapter.partnerId, LOAD)

                        // Since it's possible for a Chartboost Mediation placement to consist of multiple line items
                        // at load time and we are tracking metrics data per line item, we need to batch data for
                        // the same placement in the same dataset.
                        loadMetricsSet.add(metrics)

                        metrics.auctionId = auctionId
                        metrics.lineItemId = lineItemId
                        metrics.networkType = Metrics.NetworkType.getType(isMediation)
                        metrics.partnerPlacement = request.partnerPlacement
                        metrics.placementType = placementType
                        if (placementType == "adaptive_banner"
                            && request.size != null) {
                            metrics.size = Size(
                                request.size.width,
                                request.size.height
                            )
                        }

                        measureTimeMillis {
                            withContext(Main) {
                                metrics.start = System.currentTimeMillis()

                                try {
                                    adapter.load(
                                        context, request, createPartnerAdListener(
                                            request.adInteractionListener, auctionId
                                        )
                                    ).let {
                                        handleLoadResult(it, metrics)
                                        result = it
                                    }
                                } catch (e: Exception) {
                                    metrics.end = System.currentTimeMillis()
                                    metrics.isSuccess = false
                                    metrics.chartboostMediationError =
                                        (e as? ChartboostMediationAdException)?.chartboostMediationError
                                            ?: ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION
                                    metrics.chartboostMediationErrorMessage = e.message

                                    result =
                                        Result.failure(
                                            ChartboostMediationAdException(
                                                ChartboostMediationError.CM_LOAD_FAILURE_EXCEPTION
                                            )
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
                            chartboostMediationError = ChartboostMediationError.CM_LOAD_FAILURE_ADAPTER_NOT_FOUND,
                            chartboostMediationErrorMessage = ChartboostMediationError.CM_LOAD_FAILURE_ADAPTER_NOT_FOUND.message,
                            loadId = request.identifier,
                            placementType = placementType,
                            size = request.size?.takeIf { (placementType == "adaptive_banner") }
                        )

                        result =
                            Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_ADAPTER_NOT_FOUND))
                    }
                } ?: run {
                    MetricsManager.postMetricsDataForFailedEvent(
                        partner = request.partnerId,
                        event = LOAD,
                        auctionIdentifier = auctionId,
                        chartboostMediationError = ChartboostMediationError.CM_LOAD_FAILURE_TIMEOUT,
                        chartboostMediationErrorMessage = ChartboostMediationError.CM_LOAD_FAILURE_TIMEOUT.message,
                        loadId = request.identifier,
                        placementType = placementType,
                        size = request.size?.takeIf { (placementType == "adaptive_banner") }
                    )

                    result =
                        Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_TIMEOUT))
                }
            }
        }
        job.join()
        return result
    }

    /**
     * Issue a call to the partner adapter to show the given ad.
     *
     * @param context The context to use for the call.
     * @param partnerAd The [PartnerAd] to attempt to show.
     * @param auctionIdentifier The current auction ID.
     */
    suspend fun routeShow(
        context: Context, partnerAd: PartnerAd?, auctionIdentifier: String, loadId: String
    ): PartnerShowResult {
        var internalAdShowResult = PartnerShowResult(
            partnerAd = partnerAd,
            metrics = setOf(Metrics(partnerAd?.request?.partnerId, SHOW).apply {
                isSuccess = false
                auctionId = auctionIdentifier
                chartboostMediationError = ChartboostMediationError.CM_SHOW_FAILURE_UNKNOWN
                chartboostMediationErrorMessage =
                    ChartboostMediationError.CM_SHOW_FAILURE_UNKNOWN.message
            }),
        )

        if (!requiredDataIsValid(context) || partnerAd == null) {
            MetricsManager.postMetricsDataForFailedEvent(
                partner = null,
                event = SHOW,
                auctionIdentifier = auctionIdentifier,
                chartboostMediationError = ChartboostMediationError.CM_INVALID_ARGUMENTS,
                chartboostMediationErrorMessage = ChartboostMediationError.CM_INVALID_ARGUMENTS.message,
                loadId = loadId
            )

            internalAdShowResult.metrics.first().chartboostMediationError =
                ChartboostMediationError.CM_INVALID_ARGUMENTS
            internalAdShowResult.metrics.first().chartboostMediationErrorMessage =
                ChartboostMediationError.CM_INVALID_ARGUMENTS.message

            return internalAdShowResult
        }

        val partnerShowJob = CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
            MetricsManager.postMetricsDataForFailedEvent(
                partner = partnerAd.request.partnerId,
                event = SHOW,
                auctionIdentifier = auctionIdentifier,
                chartboostMediationError = ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION,
                chartboostMediationErrorMessage = error.message
                    ?: ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION.message
            )

            internalAdShowResult = PartnerShowResult(
                partnerAd = partnerAd,
                metrics = setOf(Metrics(partnerAd.request.partnerId, SHOW).apply {
                    isSuccess = false
                    auctionId = auctionIdentifier
                    chartboostMediationError = ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION
                    chartboostMediationErrorMessage =
                        error.message ?: ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION.message
                })
            )
        }) {
            val metricsDataSet = mutableSetOf<Metrics>()
            val metrics = Metrics(partnerAd.request.partnerId, SHOW)
            metricsDataSet.add(metrics)

            adapters[partnerAd.request.partnerId]?.let { adapter ->
                metrics.auctionId = auctionIdentifier

                withTimeoutOrNull(AppConfigStorage.showTimeoutSeconds * 1000L) {
                    metrics.start = System.currentTimeMillis()

                    measureTimeMillis {
                        try {
                            handleShowResult(adapter.show(context, partnerAd), metrics)
                        } catch (e: Exception) {
                            metrics.end = System.currentTimeMillis()
                            metrics.isSuccess = false
                            metrics.chartboostMediationError =
                                ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION
                            metrics.chartboostMediationErrorMessage =
                                e.message
                                    ?: ChartboostMediationError.CM_SHOW_FAILURE_EXCEPTION.message
                        }
                    }.also {
                        metrics.duration = it
                    }
                }
            }

            internalAdShowResult = PartnerShowResult(
                partnerAd = partnerAd,
                metrics = metricsDataSet
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
            CoroutineScope(Main).launch(CoroutineExceptionHandler { _, error ->
                LogController.e(
                    "Invalidation failed for ${partnerAd.request.partnerId} and Chartboost Mediation" +
                            "placement ${partnerAd.request.chartboostPlacement}. Error: ${error.message}."
                )
            }) {
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

                adapters[adapter.partnerId] = adapter
                initStatuses[adapter.partnerId] = PartnerInitializationStatus.IDLE
            } catch (exception: Exception) {
                LogController.e(
                    "Failed to create adapter $name. Error: ${exception.message}." +
                            "The associated network will not be initialized."
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
        metrics: Metrics
    ) {
        initStatuses[adapter.partnerId] = PartnerInitializationStatus.INITIALIZING

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
                    ?: ChartboostMediationError.CM_INITIALIZATION_FAILURE_EXCEPTION
            metrics.chartboostMediationErrorMessage = exception.message

            initStatuses[adapter.partnerId] = PartnerInitializationStatus.FAILED
        }
    }

    /**
     * Handle a successful/failed partner SDK initialization attempt.
     *
     * @param result The result of the initialization attempt.
     * @param adapter The adapter that attempted to initialize.
     */
    private fun handleSetupResult(
        result: Result<Unit>, adapter: PartnerAdapter, metrics: Metrics
    ) {
        initStatuses[adapter.partnerId] = if (result.isSuccess) {
            // Preferred to be called after a successful setup as select partner SDKs might require
            // initialization first before version (and other data) are made available.
            setUpAdapterInfo(adapter)

            metrics.isSuccess = true
            PartnerInitializationStatus.INITIALIZED
        } else {
            // Remove adapters that failed to initialize so that we don't call other operations
            // (token fetch, load, show) on them. Note that we still wait for adapters that don't finish
            // initialization within the timeout period (their result will be handled here as well).
            adapters.remove(adapter.partnerId)

            val chartboostMediationError = (result.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                ?: ChartboostMediationError.CM_INITIALIZATION_FAILURE_UNKNOWN

            metrics.isSuccess = false
            metrics.chartboostMediationError = chartboostMediationError
            metrics.chartboostMediationErrorMessage = chartboostMediationError.message

            PartnerInitializationStatus.FAILED
        }

        metrics.partnerSdkVersion = adapterInfo[adapter.partnerId]?.partnerVersion ?: ""
        metrics.partnerAdapterVersion = adapterInfo[adapter.partnerId]?.adapterVersion ?: ""
    }

    /**
     * Create and store an [AdapterInfo] instance for the given adapter.
     *
     * @param adapter The adapter for which to create an [AdapterInfo].
     */
    private fun setUpAdapterInfo(adapter: PartnerAdapter) {
        try {
            adapterInfo[adapter.partnerId] = AdapterInfo(
                partnerVersion = adapter.partnerSdkVersion,
                adapterVersion = adapter.adapterVersion,
                partnerId = adapter.partnerId,
                partnerDisplayName = adapter.partnerDisplayName,
            )
        } catch (exception: Exception) {
            LogController.e(
                "Failed to make AdapterInfo for " + "${adapter.partnerDisplayName}. Its version data will not be available." + " Error: ${
                    (exception as? ChartboostMediationAdException)?.chartboostMediationError ?: ChartboostMediationError.CM_INITIALIZATION_FAILURE_EXCEPTION
                }"
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
        result: Result<PartnerAd>, metrics: Metrics
    ) {
        metrics.end = System.currentTimeMillis()
        metrics.isSuccess = result.isSuccess

        if (!result.isSuccess) {
            val chartboostMediationError =
                (result.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                    ?: ChartboostMediationError.CM_LOAD_FAILURE_UNKNOWN

            metrics.chartboostMediationError = chartboostMediationError
            metrics.chartboostMediationErrorMessage = chartboostMediationError.message
        }
    }

    /**
     * Get the load timeout (in ms) for the given ad format.
     *
     * @param format The current [AdFormat].
     *
     * @return The timeout (in ms) for the given ad format.
     */
    private fun getLoadTimeoutMs(format: AdFormat): Long {
        return when (format) {
            AdFormat.BANNER, AdFormat.ADAPTIVE_BANNER -> AppConfigStorage.bannerLoadTimeoutSeconds * 1000L
            AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.REWARDED_INTERSTITIAL -> AppConfigStorage.fullscreenLoadTimeoutSeconds * 1000L
            else -> {
                LogController.e("Unknown ad format: $format. Using default timeout.")
                AppConfigStorage.fullscreenLoadTimeoutSeconds * 1000L
            }
        }
    }

    /**
     * Create and listen to partner ad events.
     *
     * @param adInteractionListener The [AdInteractionListener] to notify of partner ad events.
     * @param auctionId The auction ID for the current ad load call.
     */
    private fun createPartnerAdListener(
        adInteractionListener: AdInteractionListener, auctionId: String
    ): PartnerAdListener {
        return object : PartnerAdListener {
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
                error: ChartboostMediationAdException?
            ) {
                adInteractionListener.onDismissed(partnerAd, error)
            }

            override fun onPartnerAdExpired(partnerAd: PartnerAd) {
                adInteractionListener.onExpired(partnerAd)

                MetricsManager.postMetricsData(
                    setOf(Metrics(
                        partnerAd.request.partnerId, EXPIRATION
                    ).apply {
                        this.auctionId = auctionId
                    })
                )
            }
        }
    }

    /**
     * Handle the show result of the current ad show call.
     *
     * @param result The result of the ad show call.
     * @param metrics The metrics to update with the show result.
     */
    private fun handleShowResult(
        result: Result<PartnerAd>, metrics: Metrics
    ) {
        metrics.end = System.currentTimeMillis()
        metrics.isSuccess = result.isSuccess
        if (!result.isSuccess) {
            (result.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                ?: ChartboostMediationError.CM_SHOW_FAILURE_UNKNOWN.apply {
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
    private fun requiredDataIsValid(vararg data: Any?): Boolean {
        return data.all {
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
}

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
import androidx.lifecycle.ProcessLifecycleOwner
import com.chartboost.chartboostmediationsdk.Ilrd
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationAdShowResult
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking.AUCTION_ID_HEADER_KEY
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking.OLD_RATE_LIMIT_HEADER_KEY
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY
import com.chartboost.chartboostmediationsdk.network.Endpoints
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.model.MetricsRequestBody
import com.chartboost.chartboostmediationsdk.utils.BackgroundTimeMonitoring
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import com.chartboost.chartboostmediationsdk.utils.LogController
import com.chartboost.chartboostmediationsdk.utils.toJSONObject
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.jsonObject

/**
 * @suppress
 */
class AdController(
    private val bidController: BidController,
    private val partnerController: PartnerController,
    private val privacyController: PrivacyController,
    private val loadRateLimiter: LoadRateLimiter,
    private val backgroundTimeMonitor: BackgroundTimeMonitoring,
    private val ilrd: Ilrd,
) {
    companion object {
        /**
         * Convert an [Ad.AdType] value to an [AdFormat] value.
         *
         * @param adType The [Ad.AdType] value to convert.
         *
         * @return The corresponding [AdFormat] value.
         */
        internal fun adTypeToAdFormat(adType: Int): AdFormat =
            when (adType) {
                Ad.AdType.BANNER -> AdFormat.BANNER
                Ad.AdType.ADAPTIVE_BANNER -> AdFormat.ADAPTIVE_BANNER
                Ad.AdType.INTERSTITIAL -> AdFormat.INTERSTITIAL
                Ad.AdType.REWARDED -> AdFormat.REWARDED
                Ad.AdType.REWARDED_INTERSTITIAL -> AdFormat.REWARDED_INTERSTITIAL
                else -> throw IllegalArgumentException("Unknown AdType value: $adType")
            }

        /**
         * Converts an [AdFormat] into a [PartnerAdFormat].
         *
         * @param adFormat The [AdFormat] to convert.
         *
         * @return The corresponding [PartnerAdFormat].
         * @throws IllegalArgumentException If the conversion cannot be made.
         */
        internal fun adFormatToPartnerAdFormat(adFormat: AdFormat) =
            when (adFormat) {
                AdFormat.INTERSTITIAL -> PartnerAdFormats.INTERSTITIAL
                AdFormat.REWARDED -> PartnerAdFormats.REWARDED
                AdFormat.BANNER -> PartnerAdFormats.BANNER
                AdFormat.ADAPTIVE_BANNER -> PartnerAdFormats.BANNER
                AdFormat.REWARDED_INTERSTITIAL -> PartnerAdFormats.REWARDED_INTERSTITIAL
                else -> throw IllegalArgumentException("Cannot translate $adFormat to a PartnerAdFormat")
            }
    }

    private var bannerImpressionDepth = 0
    private var interstitialImpressionDepth = 0
    private var rewardedImpressionDepth = 0
    private var rewardedInterstitialImpressionDepth = 0

    suspend fun load(
        context: Context,
        adLoadParams: AdLoadParams,
        metricsSet: MutableSet<Metrics>,
    ): Result<CachedAd> {
        val millisUntilNextLoadIsAllowed =
            loadRateLimiter.millisUntilNextLoadIsAllowed(adLoadParams.adIdentifier.placement)
        if (millisUntilNextLoadIsAllowed > 0 && AppConfigStorage.getEnableRateLimiting(context)) {
            LogController.w(
                "${adLoadParams.adIdentifier.placement} has been rate limited. Please try again in ${millisUntilNextLoadIsAllowed / 1000}.${millisUntilNextLoadIsAllowed % 1000} seconds",
            )
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.LoadError.RateLimited))
        }

        // height must be at least 50dp and no more than 1800dp. 0dp is the exception to this.
        adLoadParams.bannerSize
            ?.height
            ?.takeIf { (it < 50 && it != 0) || it > 1800 }
            ?.let { height ->
                LogController.w("Banner height must be at least 50 and no more than 1800. Banner height is $height.")
                return Result.failure(ChartboostMediationAdException(ChartboostMediationError.LoadError.InvalidBannerSize))
            }

        val backgroundMonitorOperation = backgroundTimeMonitor.startMonitoringOperation()
        val loadStart = System.currentTimeMillis()
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundMonitorOperation)

        val result =
            withContext(IO) {
                val adFormat = adTypeToAdFormat(adLoadParams.adIdentifier.adType)
                ChartboostMediationNetworking.makeBidRequest(
                    privacyController = privacyController,
                    partnerController = partnerController,
                    adLoadParams = adLoadParams,
                    bidTokens =
                        partnerController.routeGetBidderInformation(
                            context,
                            PartnerAdPreBidRequest(
                                mediationPlacement = adLoadParams.adIdentifier.placement,
                                format = adFormatToPartnerAdFormat(adFormat),
                                loadId = adLoadParams.loadId,
                                bannerSize = adLoadParams.bannerSize,
                                keywords = adLoadParams.keywords.get(),
                                partnerSettings = adLoadParams.partnerSettings,
                            ),
                        ),
                    rateLimitHeaderValue =
                        loadRateLimiter
                            .getLoadRateLimitSeconds(adLoadParams.adIdentifier.placement)
                            .toString(),
                    impressionDepth = getImpressionDepth(adLoadParams.adIdentifier.adType),
                )
            }

        ProcessLifecycleOwner.get().lifecycle.removeObserver(backgroundMonitorOperation)

        when (result) {
            is ChartboostMediationNetworkingResult.Success -> {
                updateLoadRateLimiter(
                    adLoadParams.adIdentifier.placement,
                    result.headers.toMultimap(),
                )
                val auctionResult =
                    AuctionResult(
                        bids =
                            Bids(
                                adLoadParams,
                                result.body ?: BidsResponse.EMPTY_BIDS_RESPONSE,
                            ),
                        headers = result.headers.toMultimap(),
                        chartboostMediationError = null,
                    )
                val cachedAd = CachedAd(auctionResult.bids)
                val partnerAdResult =
                    bidController.loadBids(
                        context = context,
                        bids = auctionResult.bids,
                        bannerSize = adLoadParams.bannerSize,
                        adInteractionListener =
                            createInteractionListener(
                                auctionResult.bids,
                                adLoadParams.adInteractionListener,
                                cachedAd,
                            ),
                        loadMetricsSet = metricsSet,
                        adLoadParams = adLoadParams,
                    )
                MetricsManager.postMetricsData(
                    metricsSet,
                    loadId = adLoadParams.loadId,
                    queueId = adLoadParams.queueId,
                    loadStart = loadStart,
                    backgroundDurationMs = backgroundMonitorOperation.backgroundTimeUntilNow(),
                    eventResult =
                        if (partnerAdResult.isSuccess) {
                            EventResult.AdLoadResult.AdLoadSuccess
                        } else {
                            EventResult.AdLoadResult.AdLoadPartnerFailure(
                                MetricsError.SimpleError(
                                    (partnerAdResult.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                                        ?: ChartboostMediationError.LoadError.NetworkingError,
                                ),
                            )
                        },
                )
                partnerAdResult.fold({
                    cachedAd.partnerAd = it
                    cachedAd.winningBidInfo = auctionResult.bids.bidInfo
                    cachedAd.ilrdJson = auctionResult.bids.activeBid?.ilrd
                    cachedAd.loadId = adLoadParams.loadId
                    sendAuctionWinnerRequest(auctionResult.bids, adLoadParams.loadId)
                    return Result.success(cachedAd)
                }, {
                    return Result.failure(it)
                })
            }

            is ChartboostMediationNetworkingResult.Failure -> {
                if (result.error != ChartboostMediationError.LoadError.AuctionNoBid &&
                    result.error != ChartboostMediationError.LoadError.RateLimited
                ) {
                    LogController.e(result.error.message)
                }

                MetricsManager.postMetricsDataForFailedEvent(
                    partner = null,
                    event = Endpoints.Event.LOAD,
                    auctionIdentifier = result.headers?.get(AUCTION_ID_HEADER_KEY) ?: "",
                    chartboostMediationError = result.error,
                    chartboostMediationErrorMessage = result.error.message,
                    placementType = adLoadParams.adIdentifier.placementType,
                    loadStart = loadStart,
                    backgroundDuration = backgroundMonitorOperation.backgroundTimeUntilNow(),
                    size =
                        if (adLoadParams.bannerSize?.isAdaptive == true) {
                            Size(adLoadParams.bannerSize.width, adLoadParams.bannerSize.height)
                        } else {
                            null
                        },
                    loadId = adLoadParams.loadId,
                    eventResult =
                        EventResult.AdLoadResult.AdLoadUnspecifiedFailure(
                            MetricsError.SimpleError(result.error),
                        ),
                )

                return Result.failure(ChartboostMediationAdException(result.error))
            }

            is ChartboostMediationNetworkingResult.JsonParsingFailure -> {
                val cmError = ChartboostMediationError.LoadError.InvalidBidResponse
                val exceptionMessage =
                    result.exception.message?.split('\n', limit = 2)?.let {
                        it[0]
                    } ?: ""

                val malformedJson =
                    result.exception.message?.substring(exceptionMessage.length)?.let {
                        if (it.startsWith("\nJSON input: ")) {
                            it.substring("\nJSON input: ".length)
                        } else {
                            it
                        }
                    } ?: ""

                val jsonParseError =
                    MetricsError.JsonParseError(
                        cmError,
                        result.exception,
                        exceptionMessage,
                        malformedJson,
                    )

                MetricsManager.postMetricsDataForFailedEvent(
                    partner = null,
                    event = Endpoints.Event.LOAD,
                    auctionIdentifier = result.headers[AUCTION_ID_HEADER_KEY],
                    chartboostMediationError = cmError,
                    chartboostMediationErrorMessage = cmError.message,
                    placementType = adLoadParams.adIdentifier.placementType,
                    loadStart = loadStart,
                    backgroundDuration = backgroundMonitorOperation.backgroundTimeUntilNow(),
                    size =
                        if (adLoadParams.bannerSize?.isAdaptive == true) {
                            Size(adLoadParams.bannerSize.width, adLoadParams.bannerSize.height)
                        } else {
                            null
                        },
                    loadId = adLoadParams.loadId,
                    eventResult = EventResult.AdLoadResult.AdLoadJsonFailure(jsonParseError),
                )

                return Result.failure(ChartboostMediationAdException(result.error))
            }
        }
    }

    private fun createInteractionListener(
        bids: Bids,
        adInteractionListener: AdInteractionListener,
        cachedAd: CachedAd,
    ): AdInteractionListener {
        return object : AdInteractionListener {
            override fun onImpressionTracked(partnerAd: PartnerAd) {
                adInteractionListener.onImpressionTracked(partnerAd)
                CoroutineScope(IO).launch {
                    ChartboostMediationNetworking.trackPartnerImpression(
                        ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: "",
                        bids.auctionId,
                        cachedAd.loadId,
                        partnerAd.partnerBannerSize?.type ?: partnerAd.request.format,
                    )
                }
            }

            override fun onClicked(partnerAd: PartnerAd) {
                CoroutineScope(IO).launch {
                    ChartboostMediationNetworking.trackClick(
                        bids.auctionId,
                        cachedAd.loadId,
                        partnerAd.partnerBannerSize?.type ?: partnerAd.request.format,
                    )
                }
                adInteractionListener.onClicked(partnerAd)
            }

            override fun onRewarded(partnerAd: PartnerAd) {
                if (partnerAd.request.format != PartnerAdFormats.REWARDED &&
                    partnerAd.request.format != PartnerAdFormats.REWARDED_INTERSTITIAL
                ) {
                    LogController.w("Received rewarded callback for non-rewarded placement. Ignoring.")
                    return
                }

                CoroutineScope(IO).launch {
                    ChartboostMediationNetworking.trackReward(
                        bids.auctionId,
                        cachedAd.loadId,
                        partnerAd.request.format,
                    )
                    val activeBid = bids.activeBid
                    if (activeBid != null) {
                        bids.rewardedCallbackData?.let { rewardedCallbackData ->
                            ChartboostMediationNetworking.makeRewardedCallbackRequest(
                                activeBid,
                                cachedAd.customData,
                                rewardedCallbackData,
                            )
                        }
                    }
                }

                adInteractionListener.onRewarded(partnerAd)
            }

            override fun onDismissed(
                partnerAd: PartnerAd,
                error: ChartboostMediationAdException?,
            ) {
                adInteractionListener.onDismissed(partnerAd, error)
            }

            override fun onExpired(partnerAd: PartnerAd) {
                adInteractionListener.onExpired(partnerAd)
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun show(
        activity: Activity,
        cachedAd: CachedAd,
    ): ChartboostMediationAdShowResult {
        val internalShowResult =
            partnerController.routeShow(
                activity,
                cachedAd.partnerAd,
                cachedAd.bids.auctionId,
                cachedAd.loadId,
            )
        val showSucceeded = internalShowResult.metrics.first().isSuccess
        val metricsRequestBody =
            MetricsManager.buildMetricsDataRequestBody(internalShowResult.metrics)
        val payloadJson =
            ChartboostMediationJson
                .writeJson(
                    metricsRequestBody,
                    MetricsRequestBody.serializer(),
                ).jsonObject
                .toJSONObject()

        if (showSucceeded) {
            internalShowResult.partnerAd?.let { partnerAd ->
                cachedAd.partnerAd = partnerAd
                ChartboostMediationNetworking.trackChartboostImpression(
                    cachedAd.bids,
                    cachedAd.loadId,
                    partnerAd.partnerBannerSize?.type ?: partnerAd.request.format,
                )
                cachedAd.ilrdJson?.let {
                    ilrd.onIlrdReceived(partnerAd.request.mediationPlacement, it.toJSONObject())
                }
                if (PartnerAdFormats.REWARDED == partnerAd.request.format) {
                    rewardedImpressionDepth++
                } else if (PartnerAdFormats.INTERSTITIAL == partnerAd.request.format) {
                    interstitialImpressionDepth++
                } else if (PartnerAdFormats.REWARDED_INTERSTITIAL == partnerAd.request.format) {
                    rewardedInterstitialImpressionDepth++
                }
                return ChartboostMediationAdShowResult(payloadJson, null)
            } ?: run {
                return ChartboostMediationAdShowResult(
                    payloadJson,
                    ChartboostMediationError.ShowError.AdNotReady,
                )
            }
        } else {
            return ChartboostMediationAdShowResult(
                payloadJson,
                internalShowResult.metrics.first().chartboostMediationError
                    ?: ChartboostMediationError.ShowError.Unknown,
            )
        }
    }

    fun invalidate(cachedAd: CachedAd) {
        cachedAd.partnerAd?.let { partnerController.routeInvalidate(it) }
            ?: LogController.d(ChartboostMediationError.InvalidateError.AdNotFound.message)
    }

    internal fun incrementBannerImpressionDepth() {
        bannerImpressionDepth++
    }

    private fun updateLoadRateLimiter(
        placement: String,
        headers: Map<String, List<String>>,
    ) {
        (
            headers[RATE_LIMIT_HEADER_KEY]?.firstOrNull()
                ?: headers[OLD_RATE_LIMIT_HEADER_KEY]?.firstOrNull()
        )?.let { rateLimit ->
            try {
                loadRateLimiter.setLoadRateLimit(placement, rateLimit.toInt())
            } catch (e: NumberFormatException) {
                LogController.w(
                    "Unable to retrieve rate limit on $placement due to number format exception.",
                )
            }
        }
    }

    private fun sendAuctionWinnerRequest(
        bids: Bids,
        loadId: String,
    ) {
        CoroutineScope(IO).launch {
            ChartboostMediationNetworking.logAuctionWinner(
                bids,
                loadId,
                bids.activeBid?.adIdentifier?.placementType ?: "",
            )
        }
    }

    private fun getImpressionDepth(adType: Int): Int =
        when (adType) {
            Ad.AdType.INTERSTITIAL -> interstitialImpressionDepth
            Ad.AdType.REWARDED -> rewardedImpressionDepth
            Ad.AdType.BANNER -> bannerImpressionDepth
            Ad.AdType.ADAPTIVE_BANNER -> bannerImpressionDepth
            Ad.AdType.REWARDED_INTERSTITIAL -> rewardedInterstitialImpressionDepth
            else -> bannerImpressionDepth
        }
}

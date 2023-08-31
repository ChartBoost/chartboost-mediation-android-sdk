/*
 * Copyright 2022-2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.controllers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.chartboost.heliumsdk.Ilrd
import com.chartboost.heliumsdk.ad.ChartboostMediationAdShowResult
import com.chartboost.heliumsdk.domain.*
import com.chartboost.heliumsdk.domain.Ad.AdType.*
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking.AUCTION_ID_HEADERY_KEY
import com.chartboost.heliumsdk.network.ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY
import com.chartboost.heliumsdk.network.Endpoints
import com.chartboost.heliumsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.heliumsdk.network.model.MetricsRequestBody
import com.chartboost.heliumsdk.utils.Environment
import com.chartboost.heliumsdk.utils.HeliumJson
import com.chartboost.heliumsdk.utils.LogController
import com.chartboost.heliumsdk.utils.LogController.postMetricsData
import com.chartboost.heliumsdk.utils.toJSONObject
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
    private val ilrd: Ilrd
) {
    companion object {
        /**
         * Convert an [Ad.AdType] value to an [AdFormat] value.
         *
         * @param adType The [Ad.AdType] value to convert.
         *
         * @return The corresponding [AdFormat] value.
         */
        internal fun adTypeToAdFormat(adType: Int): AdFormat {
            return when (adType) {
                Ad.AdType.BANNER -> AdFormat.BANNER
                Ad.AdType.INTERSTITIAL -> AdFormat.INTERSTITIAL
                Ad.AdType.REWARDED -> AdFormat.REWARDED
                Ad.AdType.REWARDED_INTERSTITIAL -> AdFormat.REWARDED_INTERSTITIAL
                else -> throw IllegalArgumentException("Unknown AdType value: $adType")
            }
        }
    }

    private var bannerImpressionDepth = 0
    private var interstitialImpressionDepth = 0
    private var rewardedImpressionDepth = 0
    private var rewardedInterstitialImpressionDepth = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun load(
        context: Context,
        adLoadParams: AdLoadParams,
        metricsSet: MutableSet<Metrics>
    ): Result<CachedAd> {
        val millisUntilNextLoadIsAllowed =
            loadRateLimiter.millisUntilNextLoadIsAllowed(adLoadParams.adIdentifier.placementName)
        if (millisUntilNextLoadIsAllowed > 0 && AppConfigStorage.getEnableRateLimiting()) {
            LogController.w("${adLoadParams.adIdentifier.placementName} has been rate limited. Please try again in ${millisUntilNextLoadIsAllowed / 1000}.${millisUntilNextLoadIsAllowed % 1000} seconds")
            return Result.failure(ChartboostMediationAdException(ChartboostMediationError.CM_LOAD_FAILURE_RATE_LIMITED))
        }

        if (AppConfigStorage.shouldNotifyLoads) {
            sendLoadId(adLoadParams.adIdentifier, adLoadParams.loadId)
        }

        val result = withContext(IO) {
            ChartboostMediationNetworking.makeBidRequest(
                privacyController = privacyController,
                partnerController = partnerController,
                adLoadParams = adLoadParams,
                bidTokens = partnerController.routeGetBidderInformation(
                    context,
                    PreBidRequest(
                        adLoadParams.adIdentifier.placementName,
                        adTypeToAdFormat(adLoadParams.adIdentifier.adType),
                        adLoadParams.loadId
                    )
                ),
                rateLimitHeaderValue = loadRateLimiter.getLoadRateLimitSeconds(adLoadParams.adIdentifier.placementName)
                    .toString(),
                impressionDepth = getImpressionDepth(adLoadParams.adIdentifier.adType)
            )
        }

        when (result) {
            is ChartboostMediationNetworkingResult.Success -> {
                updateLoadRateLimiter(
                    adLoadParams.adIdentifier.placementName,
                    result.headers.toMultimap()
                )
                val auctionResult = AuctionResult(
                    bids = Bids(
                        adLoadParams,
                        result.body ?: BidsResponse.EMPTY_BIDS_RESPONSE,
                    ),
                    headers = result.headers.toMultimap(),
                    chartboostMediationError = null
                )
                val cachedAd = CachedAd(auctionResult.bids.auctionId)
                val partnerAdResult = bidController.loadBids(
                    context = context,
                    bids = auctionResult.bids,
                    bannerSize = adLoadParams.bannerSize,
                    adInteractionListener = createInteractionListener(
                        auctionResult.bids, adLoadParams.adInteractionListener, cachedAd
                    ),
                    loadMetricsSet = metricsSet
                )
                postMetricsData(
                    metricsSet,
                    loadId = adLoadParams.loadId,
                    eventResult = if (partnerAdResult.isSuccess) {
                        EventResult.AdLoadResult.AdLoadSuccess
                    } else {
                        EventResult.AdLoadResult.AdLoadPartnerFailure(
                            MetricsError.SimpleError(
                                (partnerAdResult.exceptionOrNull() as? ChartboostMediationAdException)?.chartboostMediationError
                                    ?: ChartboostMediationError.CM_LOAD_FAILURE_UNKNOWN
                            )
                        )
                    }
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
                if (result.error != ChartboostMediationError.CM_LOAD_FAILURE_AUCTION_NO_BID
                    && result.error != ChartboostMediationError.CM_LOAD_FAILURE_RATE_LIMITED
                ) {
                    LogController.e(result.error.message)
                }

                LogController.postMetricsDataForFailedEvent(
                    partner = null,
                    event = Endpoints.Sdk.Event.LOAD,
                    auctionIdentifier = result.headers?.get(AUCTION_ID_HEADERY_KEY) ?: "",
                    chartboostMediationError = result.error,
                    chartboostMediationErrorMessage = result.error.message,
                    loadId = adLoadParams.loadId,
                    eventResult = EventResult.AdLoadResult.AdLoadUnspecifiedFailure(
                        MetricsError.SimpleError(result.error)
                    )
                )

                return Result.failure(ChartboostMediationAdException(result.error))
            }
            is ChartboostMediationNetworkingResult.JsonParsingFailure -> {
                val cmError = ChartboostMediationError.CM_LOAD_FAILURE_INVALID_BID_RESPONSE
                val exceptionMessage = result.exception.message?.split('\n', limit = 2)?.let {
                    it[0]
                } ?: ""

                val malformedJson =
                    result.exception.message?.substring(exceptionMessage.length)?.let {
                        if (it.startsWith("\nJSON input: ")) {
                            it.substring("\nJSON input: ".length)
                        } else it
                    } ?: ""

                val jsonParseError = MetricsError.JsonParseError(
                    cmError,
                    result.exception,
                    exceptionMessage,
                    malformedJson
                )

                LogController.postMetricsDataForFailedEvent(
                    partner = null,
                    event = Endpoints.Sdk.Event.LOAD,
                    auctionIdentifier = result.headers[AUCTION_ID_HEADERY_KEY],
                    chartboostMediationError = cmError,
                    chartboostMediationErrorMessage = cmError.message,
                    loadId = adLoadParams.loadId,
                    eventResult = EventResult.AdLoadResult.AdLoadJsonFailure(jsonParseError)
                )

                return Result.failure(ChartboostMediationAdException(result.error))
            }
        }
    }

    private fun createInteractionListener(
        bids: Bids, adInteractionListener: AdInteractionListener, cachedAd: CachedAd
    ): AdInteractionListener {
        return object : AdInteractionListener {
            override fun onImpressionTracked(partnerAd: PartnerAd) {
                adInteractionListener.onImpressionTracked(partnerAd)
                CoroutineScope(IO).launch {
                    ChartboostMediationNetworking.trackPartnerImpression(
                        Environment.appSetId ?: "",
                        bids.auctionId,
                        cachedAd.loadId
                    )
                }
            }

            override fun onClicked(partnerAd: PartnerAd) {
                CoroutineScope(IO).launch {
                    ChartboostMediationNetworking.trackClick(
                        bids.auctionId,
                        cachedAd.loadId
                    )
                }
                adInteractionListener.onClicked(partnerAd)
            }

            override fun onRewarded(partnerAd: PartnerAd) {
                if (partnerAd.request.format != AdFormat.REWARDED && partnerAd.request.format != AdFormat.REWARDED_INTERSTITIAL) {
                    LogController.w("Received rewarded callback for non-rewarded placement. Ignoring.")
                    return
                }

                CoroutineScope(IO).launch {
                    ChartboostMediationNetworking.trackReward(
                        bids.auctionId,
                        cachedAd.loadId
                    )
                    val activeBid = bids.activeBid
                    if (activeBid != null) {
                        bids.rewardedCallbackData?.let { rewardedCallbackData ->
                            ChartboostMediationNetworking.makeRewardedCallbackRequest(
                                activeBid,
                                cachedAd.customData,
                                rewardedCallbackData
                            )
                        }
                    }
                }

                adInteractionListener.onRewarded(partnerAd)
            }

            override fun onDismissed(partnerAd: PartnerAd, error: ChartboostMediationAdException?) {
                adInteractionListener.onDismissed(partnerAd, error)
            }

            override fun onExpired(partnerAd: PartnerAd) {
                adInteractionListener.onExpired(partnerAd)
            }
        }
    }

    private fun sendLoadId(adIdentifier: AdIdentifier, loadId: String) {
        CoroutineScope(IO).launch {
            ChartboostMediationNetworking.trackAdLoad(
                adIdentifier.placementName,
                adIdentifier.placementType,
                loadId,
                "new"
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun show(
        context: Context,
        cachedAd: CachedAd
    ): ChartboostMediationAdShowResult {
        val internalShowResult = partnerController.routeShow(
            context,
            cachedAd.partnerAd,
            cachedAd.auctionId,
            cachedAd.loadId
        )
        val showSucceeded = internalShowResult.metrics.first().isSuccess
        val metricsRequestBody = LogController.buildMetricsDataRequestBody(internalShowResult.metrics)
        val payloadJson = HeliumJson.writeJson(
            metricsRequestBody,
            MetricsRequestBody.serializer()
        ).jsonObject.toJSONObject()

        if (showSucceeded) {
            internalShowResult.partnerAd?.let { partnerAd ->
                cachedAd.partnerAd = partnerAd
                ChartboostMediationNetworking.trackChartboostImpression(cachedAd.auctionId, cachedAd.loadId)
                cachedAd.ilrdJson?.let {
                    ilrd.onIlrdReceived(partnerAd.request.chartboostPlacement, it.toJSONObject())
                }
                if (AdFormat.REWARDED == partnerAd.request.format) {
                    rewardedImpressionDepth++
                } else if (AdFormat.INTERSTITIAL == partnerAd.request.format) {
                    interstitialImpressionDepth++
                } else if (AdFormat.REWARDED_INTERSTITIAL == partnerAd.request.format) {
                    rewardedInterstitialImpressionDepth++
                }
                return ChartboostMediationAdShowResult(payloadJson, null)
            } ?: run {
                return ChartboostMediationAdShowResult(
                    payloadJson,
                    ChartboostMediationError.CM_SHOW_FAILURE_AD_NOT_READY
                )
            }
        } else {
            return ChartboostMediationAdShowResult(
                payloadJson,
                internalShowResult.metrics.first().chartboostMediationError
                    ?: ChartboostMediationError.CM_SHOW_FAILURE_UNKNOWN
            )
        }
    }

    fun invalidate(cachedAd: CachedAd) {
        cachedAd.partnerAd?.let { partnerController.routeInvalidate(it) }
            ?: LogController.d(ChartboostMediationError.CM_INVALIDATE_FAILURE_AD_NOT_FOUND.message)
    }

    internal fun incrementBannerImpressionDepth() {
        bannerImpressionDepth++
    }

    private fun updateLoadRateLimiter(placement: String, headers: Map<String, List<String>>) {
        headers[RATE_LIMIT_HEADER_KEY]?.firstOrNull()?.let { rateLimit ->
            try {
                loadRateLimiter.setLoadRateLimit(placement, rateLimit.toInt())
            } catch (e: NumberFormatException) {
                LogController.w(
                    "Unable to retrieve rate limit on $placement due to number format exception."
                )
            }
        }
    }

    private fun sendAuctionWinnerRequest(bids: Bids, loadId: String) {
        CoroutineScope(IO).launch {
            ChartboostMediationNetworking.logAuctionWinner(bids, loadId)
        }
    }

    private fun getImpressionDepth(adType: Int): Int {
        return when (adType) {
            Ad.AdType.INTERSTITIAL -> interstitialImpressionDepth
            Ad.AdType.REWARDED -> rewardedImpressionDepth
            Ad.AdType.BANNER -> bannerImpressionDepth
            Ad.AdType.REWARDED_INTERSTITIAL -> rewardedInterstitialImpressionDepth
            else -> bannerImpressionDepth
        }
    }
}

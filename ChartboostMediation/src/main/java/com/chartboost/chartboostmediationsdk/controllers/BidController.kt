/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.controllers

import android.content.Context
import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import com.chartboost.chartboostmediationsdk.controllers.AdController.Companion.adFormatToPartnerAdFormat
import com.chartboost.chartboostmediationsdk.controllers.AdController.Companion.adTypeToAdFormat
import com.chartboost.chartboostmediationsdk.domain.*
import com.chartboost.chartboostmediationsdk.utils.LogController

/**
 * @suppress
 *
 * This class is responsible for managing loading bids.
 */
class BidController(
    private val partnerController: PartnerController,
) {
    /**
     * Load all the bids from a given collection of [Bids].
     *
     * @param bids A collection wrapper class containing [Bid]s to load.
     * @param adInteractionListener Necessary to forward future ad interaction events from the partner to the caller of this function.
     */
    suspend fun loadBids(
        context: Context,
        bids: Bids,
        bannerSize: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
        adInteractionListener: AdInteractionListener,
        loadMetricsSet: MutableSet<Metrics>,
        adLoadParams: AdLoadParams,
    ): Result<PartnerAd> {
        var activeBid = bids.activeBid
        while (activeBid != null) {
            LogController.d(
                "Loading bid for ${activeBid.partnerName} with placement name " +
                    "${activeBid.partnerPlacement} on Chartboost placement ${activeBid.adIdentifier}",
            )
            val partnerAdResult =
                partnerController.routeLoad(
                    context = context,
                    auctionId = bids.auctionId,
                    lineItemId = activeBid.lineItemId,
                    isMediation = activeBid.isMediation,
                    request = constructAdLoadRequest(activeBid, activeBid.size, adInteractionListener, adLoadParams.keywords),
                    loadMetricsSet = loadMetricsSet,
                    placementType = activeBid.adIdentifier.placementType,
                )

            if (!partnerAdResult.isSuccess) {
                LogController.d(
                    "Loading bid FAILED for Chartboost placement ${bids.adIdentifier} with error: ${partnerAdResult.exceptionOrNull()}",
                )

                bids.incrementActiveBid()
                activeBid = bids.activeBid
            } else if ((bannerSize?.isAdaptive == true) && ChartboostMediationSdk.isDiscardOversizedAdsEnabled()) {
                val details = partnerAdResult.getOrNull()?.details

                val requestedWidth = bannerSize.width
                val requestedHeight = bannerSize.height

                val reportedWidth = (details?.get("banner_width_dips") as? String?)?.toInt() ?: -1
                val reportedHeight = (details?.get("banner_height_dips") as? String?)?.toInt() ?: -1

                if ((reportedWidth > 0 && reportedWidth > requestedWidth) ||
                    (reportedHeight > 0 && reportedHeight > requestedHeight)
                ) {
                    LogController.d(
                        """
                        Loading bid FAILED for Chartboost placement ${bids.adIdentifier} due to oversized ad
                        Requested size: ($requestedWidth, $requestedHeight)
                        Returned size: ($reportedWidth, $reportedHeight)
                        """.trimIndent(),
                    )

                    bids.incrementActiveBid()
                    activeBid = bids.activeBid
                }
            } else {
                return partnerAdResult
            }
        }
        return Result.failure(ChartboostMediationAdException(ChartboostMediationError.LoadError.WaterfallExhaustedNoFill))
    }

    /**
     * Construct an [PartnerAdLoadRequest] instance containing the data needed by adapters to load and show ads.
     *
     * @param bid The current [Bid] instance from which to derive the necessary data.
     * @param bannerSize The width and height of the expected banner if it is a banner.
     * @param adInteractionListener Listener for ad interactions.
     *
     * @return An [PartnerAdLoadRequest] instance.
     */
    private fun constructAdLoadRequest(
        bid: Bid,
        bannerSize: ChartboostMediationBannerAdView.ChartboostMediationBannerSize?,
        adInteractionListener: AdInteractionListener,
        keywords: Keywords,
    ): PartnerAdLoadRequest {
        val format = adTypeToAdFormat(bid.adIdentifier.adType)
        return PartnerAdLoadRequest(
            // TODO: This is a hack forcing the Reference Adapter to load + show ads. Remove this hack once not needed.
            partnerId = if (shouldForceReference(bid.adIdentifier.placement)) "reference" else bid.partnerName,
            mediationPlacement = bid.adIdentifier.placement,
            partnerPlacement = bid.partnerPlacement,
            bannerSize = bannerSize,
            format = adFormatToPartnerAdFormat(format),
            adm = getAdMarkup(bid),
            identifier = bid.loadRequestId,
            partnerSettings = bid.partnerSettings,
            adInteractionListener = adInteractionListener,
            keywords = keywords.get(),
        )
    }

    /**
     * Parse the bid response for the ad markup ("adm").
     *
     * @param bid The current [BidResponse] instance from which to derive the necessary data.
     *
     * @return The ad markup String.
     */
    private fun getAdMarkup(bid: Bid): String =
        if (!bid.isMediation) {
            bid.adm ?: ""
        } else {
            ""
        }

    /**
     * Check if we should force the Reference Adapter to load + show ads.
     *
     * @param placement The placement name to check.
     *
     * @return True if we should force the Reference Adapter, false otherwise.
     */
    private fun shouldForceReference(placement: String): Boolean = ChartboostMediationSdk.getTestMode() == 1 && placement.startsWith("REF")
}

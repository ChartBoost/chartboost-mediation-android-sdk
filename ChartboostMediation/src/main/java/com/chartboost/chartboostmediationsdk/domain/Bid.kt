/*
 * Copyright 2022-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView.ChartboostMediationBannerSize
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * @suppress
 *
 * This class holds bid-related data.
 *
 * @property requestedSize The requested size of the banner ad.
 * @property adIdentifier The current [AdIdentifier] instance.
 * @property bidResponse The seatbid JSON response.
 * @property loadRequestId The unique request identifier for this load call.
 */
class Bid(
    private val requestedSize: ChartboostMediationBannerSize?,
    val adIdentifier: AdIdentifier,
    private val bidResponse: BidResponse,
    val loadRequestId: String,
) : Comparable<Bid> {
    val partnerName: String = bidResponse.partnerName
    val nurl: String = bidResponse.bidInfoArray.firstOrNull()?.nurl ?: ""
    val lurl: String = bidResponse.bidInfoArray.firstOrNull()?.lurl ?: ""
    val price: Double = bidResponse.bidInfoArray.firstOrNull()?.price ?: Double.NaN
    val partnerPlacement: String = bidResponse.partnerPlacement
    val isMediation: Boolean = bidResponse.isMediation
    val partnerSettings: MutableMap<String, String> = bidResponse.partnerSettings
    val bidInfo = bidResponse.bidInfo
    val size: ChartboostMediationBannerSize?

    var lineItemId: String? = bidResponse.lineItemId
    var ilrd: JsonObject? =
        bidResponse.bidInfoArray
            .firstOrNull()
            ?.ext
            ?.ilrd
            ?.jsonObject
    var adRevenue = bidResponse.adRevenue
    var cpmPrice = bidResponse.cpmPrice
    var adm = bidResponse.bidInfoArray.firstOrNull()?.adm

    init {
        val adaptiveBannerWidth = bidResponse.bidInfoArray.firstOrNull()?.adaptiveBannerWidth
        val adaptiveBannerHeight = bidResponse.bidInfoArray.firstOrNull()?.adaptiveBannerHeight
        size =
            if (requestedSize?.isAdaptive == true &&
                adaptiveBannerWidth != null &&
                adaptiveBannerHeight != null
            ) {
                ChartboostMediationBannerSize.bannerSize(adaptiveBannerWidth, adaptiveBannerHeight)
            } else {
                requestedSize
            }
    }

    override fun compareTo(other: Bid): Int = price.compareTo(other.price)

    /**
     * Add ILRD data to the bid.
     *
     * @param updatedIlrdJson The new JSON blob to add.
     * @param overwriteExisting If the key exists, overwrite the data if there's a conflict
     */
    fun updateIlrd(
        updatedIlrdJson: JsonObject?,
        overwriteExisting: Boolean,
    ) {
        updatedIlrdJson?.let { updatedJson ->
            ilrd = ilrd?.let {
                buildJsonObject {
                    (it as Map<String, JsonElement>).entries.forEach {
                        put(it.key, it.value)
                    }
                    updatedJson.keys.forEach { key ->
                        if (overwriteExisting || !it.containsKey(key)) {
                            put(key, updatedJson.getValue(key))
                        }
                    }
                }
            } ?: updatedJson
        }
    }
}

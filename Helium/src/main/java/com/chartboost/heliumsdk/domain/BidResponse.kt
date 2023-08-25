/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.domain

import com.chartboost.heliumsdk.utils.HeliumJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * @suppress
 */
@Serializable
data class BidResponse(
    @SerialName(ID_KEY)
    val id: String? = null,

    @SerialName(BID_ID_KEY)
    val bidId: String? = null,

    @SerialName(CURRENCY_KEY)
    val currency: String? = null,

    @SerialName(BID_ARRAY_KEY)
    val bidInfoArray: List<BidInfo> = emptyList(),

    @SerialName(SEAT_KEY)
    val partnerName: String,

    @SerialName(HELIUM_BID_ID_KEY)
    val heliumBidId: String
) : Comparable<BidResponse> {

    val isMediation: Boolean =
        bidInfoArray.firstOrNull()?.id.equals(MEDIATION_KEY, ignoreCase = true)

    val partnerPlacementName = bidInfoArray.firstOrNull()?.ext?.partnerPlacementName ?: ""

    var lineItemId: String? = null

    var adRevenue: Double = bidInfoArray.firstOrNull()?.ext?.adRevenue ?: Double.NaN

    var cpmPrice: Double = bidInfoArray.firstOrNull()?.ext?.cpmPrice ?: Double.NaN

    var ilrd = bidInfoArray.firstOrNull()?.ext?.ilrd

    val partnerSettings: MutableMap<String, String> = mutableMapOf<String, String>().apply {
        bidInfoArray.firstOrNull()?.ext?.bidderInfo?.jsonObject?.getValue(HELIUM_KEY)?.jsonObject?.let {
            it.keys.forEach { key ->
                val value = HeliumJson.decodeFromJsonElement<String>(it.getValue(key))
                put(key, value)
            }
        }
    }

    val bidInfo: HashMap<String, String>
        get() {
            return hashMapOf<String, String>().apply {
                if (!cpmPrice.isNaN()) {
                    put(PRICE_KEY, cpmPrice.toString())
                }

                put(PARTNER_ID_KEY, partnerName)

                lineItemId?.let { put(LINE_ITEM_ID_KEY, it) }

                bidInfoArray.firstOrNull()?.ext?.ilrd?.jsonObject?.let { ilrd ->
                    if (ilrd.containsKey(LINE_ITEM_NAME_KEY)) {
                        // This ridiculous bit of code is because JsonLiteral.toString()
                        // automatically adds extra quotes to strings
                        (ilrd[LINE_ITEM_NAME_KEY] as JsonPrimitive?)?.content?.let {
                            put(LINE_ITEM_NAME_KEY, it)
                        }
                    }
                }
            }
        }

    init {
        if (isMediation) {
            bidInfoArray.firstOrNull()?.ext?.let {
                lineItemId = it.lineItemId
            } ?: throw Throwable("ext cannot be null when mediating.")
        }
    }

    /**
     * Add ILRD data to the bid.
     *
     * @param updatedIlrdJson The new Json blob to add.
     * @param overwriteExisting If the key exists, overwrite the data if there's a conflict
     */
    fun updateIlrd(updatedIlrdJson: JsonElement, overwriteExisting: Boolean) {
        ilrd = ilrd?.let{

            val oldIlrdMap = (it.jsonObject as Map<String, JsonElement>).toMutableMap()
            val updatedIlrdJsonMap = updatedIlrdJson.jsonObject as Map<String, JsonElement>

            for (key in updatedIlrdJsonMap.keys) {
                if (overwriteExisting || !oldIlrdMap.containsKey(key)) {
                    oldIlrdMap[key] = updatedIlrdJsonMap[key] as JsonElement
                }
            }

            JsonObject(oldIlrdMap)
        } ?: updatedIlrdJson
    }

    override fun compareTo(other: BidResponse) =
        bidInfoArray.firstOrNull()?.compareTo(other.bidInfoArray.first()) ?: -1

    companion object {
        private const val ID_KEY = "id"
        private const val BID_ID_KEY = "bidid"
        private const val CURRENCY_KEY = "cur"
        private const val BID_ARRAY_KEY = "bid"
        private const val MEDIATION_KEY = "MEDIATION"
        private const val HELIUM_KEY = "helium"
        private const val PRICE_KEY = "price"
        private const val PARTNER_ID_KEY = "partner_id"
        private const val LINE_ITEM_ID_KEY = "line_item_id"
        private const val LINE_ITEM_NAME_KEY = "line_item_name"
        private const val SEAT_KEY = "seat"
        private const val HELIUM_BID_ID_KEY = "helium_bid_id"
    }
}

/**
 * @suppress
 */
@Serializable
data class BidInfo(
    @SerialName(ID_KEY)
    val id: String,

    @SerialName(IMP_ID)
    val impressionId: String,

    @SerialName(PRICE_KEY)
    val price: Double = 0.0,

    @SerialName(LURL_KEY)
    val lurl: String = "",

    @SerialName(NURL_KEY)
    val nurl: String = "",

    @SerialName(ADM_KEY)
    val adm: String? = null,

    @SerialName(BURL_KEY)
    val burl: String = "",

    @SerialName(EXT_KEY)
    val ext: BidInfoExt? = null
) : Comparable<BidInfo> {

    override fun compareTo(other: BidInfo) = price.compareTo(other.price)

    companion object {
        private const val ADM_KEY = "adm"
        private const val PRICE_KEY = "price"
        private const val ID_KEY = "id"
        private const val IMP_ID = "impid"
        private const val EXT_KEY = "ext"
        private const val BURL_KEY = "burl"
        private const val NURL_KEY = "nurl"
        private const val LURL_KEY = "lurl"
    }
}

/**
 * @suppress
 */
@Serializable
data class BidInfoExt(
    @SerialName(BIDDER_KEY)
    val bidderInfo: JsonElement? = null,

    @SerialName(ILRD_KEY)
    val ilrd: JsonElement? = null,

    // Needs to be nullable in case it is not present, null check in BidResponse assigns NaN
    @SerialName(CPM_PRICE_KEY)
    val cpmPrice: Double? = null,

    // Needs to be nullable in case it is not present, null check in BidResponse assigns NaN
    @SerialName(AD_REVENUE_KEY)
    val adRevenue: Double? = null,

    @SerialName(PARTNER_PLACEMENT_KEY)
    val partnerPlacementName: String?,

    @SerialName(LINE_ITEM_ID_KEY)
    val lineItemId: String? = null
) {
    companion object {
        private const val BIDDER_KEY = "bidder"
        private const val PARTNER_PLACEMENT_KEY = "partner_placement"
        private const val ILRD_KEY = "ilrd"
        private const val AD_REVENUE_KEY = "ad_revenue"
        private const val CPM_PRICE_KEY = "cpm_price"
        private const val LINE_ITEM_ID_KEY = "line_item_id"
    }
}

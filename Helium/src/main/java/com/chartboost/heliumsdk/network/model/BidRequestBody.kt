/*
 * Copyright 2022-2023 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network.model

import com.chartboost.heliumsdk.HeliumSdk
import com.chartboost.heliumsdk.controllers.PartnerController
import com.chartboost.heliumsdk.controllers.PartnerController.Companion.adapterInfo
import com.chartboost.heliumsdk.controllers.PartnerController.PartnerInitializationStatus.FAILED
import com.chartboost.heliumsdk.controllers.PartnerController.PartnerInitializationStatus.INITIALIZED
import com.chartboost.heliumsdk.controllers.PrivacyController
import com.chartboost.heliumsdk.domain.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @suppress
 */
@Serializable
class BidRequestBody private constructor(
    @SerialName("user")
    val user: BidRequestUser,

    /**
     * Indicates whether the bid is a test (1) or not (0).
     */
    @SerialName("test")
    val testFlag: Int = HeliumSdk.getTestMode(),

    @SerialName("imp")
    val impressionList: List<BidRequestImpression>,

    @SerialName("app")
    val app: BidRequestApp,

    @SerialName("device")
    val device: BidRequestDevice,

    @SerialName("regs")
    val regs: BidRequestRegs,

    @SerialName("ext")
    val ext: BidRequestExt?,
) {
    constructor(
        adLoadParams: AdLoadParams,
        partnerController: PartnerController,
        privacyController: PrivacyController,
        impressionDepth: Int,
        bidTokens: Map<String, Map<String, String>>,
    ) : this(
        user = BidRequestUser(
            consent = privacyController.userConsent,
            impressionDepth = impressionDepth,
            keywords = adLoadParams.keywords
        ),
        impressionList = listOf(
            BidRequestImpression(
                adIdentifier = adLoadParams.adIdentifier,
                size = adLoadParams.bannerSize
            )
        ),
        app = BidRequestApp(),
        device = BidRequestDevice(
            size = adLoadParams.bannerSize,
        ),
        regs = BidRequestRegs(
            isCoppa = privacyController.coppa,
            gdpr = privacyController.gdpr,
            ccpaConsent = privacyController.ccpaConsent
        ),
        ext = BidRequestExt(
            adapters = partnerController.adapters,
            bidTokens = bidTokens,
            loadId = adLoadParams.loadId,
            initStatuses = partnerController.initStatuses
        )
    )
}

/**
 * @suppress
 */
@Serializable
class BidRequestExt(
    /**
     * Map of all active bidders to their bid token and [AdapterInfo] key-value pairs
     */
    @SerialName("bidders")
    val activeBidders: Map<String, Map<String, String>>,

    /**
     * Map of all inactive bidders to their bid token and [AdapterInfo] key-value pairs
     */
    @SerialName("inactive_bidders")
    val inactiveBidders: Map<String, Map<String, String>>,

    /**
     * Chartboost Mediation request  ID required by Google Bidding
     */
    @SerialName("helium_sdk_request_id")
    val heliumSdkRequestId: String?
) {
    constructor(
        adapters: Map<String, PartnerAdapter>,
        bidTokens: Map<String, Map<String, String>>,
        loadId: String,
        initStatuses: Map<String, PartnerController.PartnerInitializationStatus>
    ) : this(
        activeBidders = mapActiveBidders(adapters, bidTokens, initStatuses),
        inactiveBidders = mapInactiveBidders(adapters, bidTokens, initStatuses),
        heliumSdkRequestId = loadId
    )

    /**
     * @suppress
     */
    companion object {

        private fun mapAllBidders(
            adapters: Map<String, PartnerAdapter>,
            bidTokens: Map<String, Map<String, String>>
        ): Map<String, Map<String, String>> {
            return mutableMapOf<String, Map<String, String>>().apply {
                adapters.forEach { partnerId ->
                    val partnerValues = mutableMapOf<String, String>().apply {
                        bidTokens[partnerId.key]?.forEach { tokenEntry ->
                            put(tokenEntry.key, tokenEntry.value)
                        }
                        adapterInfo[partnerId.key]?.let {
                            put("version", it.partnerVersion)
                            put("adapter_version", it.adapterVersion)
                        }
                    }
                    put(partnerId.key, partnerValues)
                }
            }
        }

        private fun mapActiveBidders(
            adapters: Map<String, PartnerAdapter>,
            bidTokens: Map<String, Map<String, String>>,
            initStatuses: Map<String, PartnerController.PartnerInitializationStatus>
        ): Map<String, Map<String, String>> {
            return mutableMapOf<String, Map<String, String>>().apply {
                mapAllBidders(adapters, bidTokens).forEach { bidder ->
                    if (initStatuses[bidder.key] == INITIALIZED) {
                        put(bidder.key, bidder.value)
                    }
                }
            }
        }

        private fun mapInactiveBidders(
            adapters: Map<String, PartnerAdapter>,
            bidTokens: Map<String, Map<String, String>>,
            initStatuses: Map<String, PartnerController.PartnerInitializationStatus>
        ): Map<String, Map<String, String>> {
            return mutableMapOf<String, Map<String, String>>().apply {
                mapAllBidders(adapters, bidTokens).forEach { bidder ->
                    val initStatus = initStatuses[bidder.key]
                    if (initStatus != null && initStatus != INITIALIZED) {
                        put(
                            bidder.key,
                            bidder.value.toMutableMap().apply {
                                put("status", initStatus.name)
                                if (initStatus == FAILED) {
                                    put("status", "Initialization Failed")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

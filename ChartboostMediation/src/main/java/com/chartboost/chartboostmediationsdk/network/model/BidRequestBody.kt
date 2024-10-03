/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network.model

import com.chartboost.chartboostmediationsdk.ChartboostMediationSdk
import com.chartboost.chartboostmediationsdk.controllers.PartnerController
import com.chartboost.chartboostmediationsdk.controllers.PartnerController.Companion.adapterInfo
import com.chartboost.chartboostmediationsdk.controllers.PartnerController.PartnerInitializationStatus.FAILED
import com.chartboost.chartboostmediationsdk.controllers.PartnerController.PartnerInitializationStatus.INITIALIZED
import com.chartboost.chartboostmediationsdk.controllers.PrivacyController
import com.chartboost.chartboostmediationsdk.domain.AdLoadParams
import com.chartboost.chartboostmediationsdk.domain.AdapterInfo
import com.chartboost.chartboostmediationsdk.domain.PartnerAdapter
import com.chartboost.core.ChartboostCore
import com.chartboost.core.consent.ConsentKeys
import com.chartboost.core.consent.ConsentValues
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
    val testFlag: Int = ChartboostMediationSdk.getTestMode(),
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
    companion object {
        suspend operator fun invoke(
            adLoadParams: AdLoadParams,
            partnerController: PartnerController,
            privacyController: PrivacyController,
            impressionDepth: Int,
            bidTokens: Map<String, Map<String, String>>,
        ): BidRequestBody =
            BidRequestBody(
                user =
                    BidRequestUser(
                        consent =
                            ChartboostCore.consent.consents[ConsentKeys.GDPR_CONSENT_GIVEN]?.takeIf { it.isNotBlank() }
                                ?.equals(ConsentValues.GRANTED),
                        impressionDepth = impressionDepth,
                        keywords = adLoadParams.keywords,
                    ),
                impressionList =
                    listOf(
                        BidRequestImpression(
                            adIdentifier = adLoadParams.adIdentifier,
                            size = adLoadParams.bannerSize,
                        ),
                    ),
                app = BidRequestApp(),
                device =
                    BidRequestDevice(
                        size = adLoadParams.bannerSize,
                    ),
                regs =
                    BidRequestRegs(
                        isCoppa = ChartboostCore.analyticsEnvironment.isUserUnderage,
                        gdpr = privacyController.gdpr?.toString(),
                        usPrivacy = ChartboostCore.consent.consents[ConsentKeys.USP] ?: "",
                        gpp = ChartboostCore.consent.consents[ConsentKeys.GPP] ?: "",
                        // To be replaced with Chartboost Core consents in a future release.
                        gppSid = privacyController.gppSid ?: "",
                    ),
                ext =
                    BidRequestExt(
                        adapters = partnerController.adapters,
                        bidTokens = bidTokens,
                        loadId = adLoadParams.loadId,
                        initStatuses = partnerController.initStatuses,
                    ),
            )
    }
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
    val chartboostMediationSdkRequestId: String?,
) {
    constructor(
        adapters: Map<String, PartnerAdapter>,
        bidTokens: Map<String, Map<String, String>>,
        loadId: String,
        initStatuses: Map<String, PartnerController.PartnerInitializationStatus>,
    ) : this(
        activeBidders = mapActiveBidders(adapters, bidTokens, initStatuses),
        inactiveBidders = mapInactiveBidders(adapters, bidTokens, initStatuses),
        chartboostMediationSdkRequestId = loadId,
    )

    /**
     * @suppress
     */
    companion object {
        private fun mapAllBidders(
            adapters: Map<String, PartnerAdapter>,
            bidTokens: Map<String, Map<String, String>>,
        ): Map<String, Map<String, String>> =
            mutableMapOf<String, Map<String, String>>().apply {
                adapters.forEach { partnerId ->
                    val partnerValues =
                        mutableMapOf<String, String>().apply {
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

        private fun mapActiveBidders(
            adapters: Map<String, PartnerAdapter>,
            bidTokens: Map<String, Map<String, String>>,
            initStatuses: Map<String, PartnerController.PartnerInitializationStatus>,
        ): Map<String, Map<String, String>> =
            mutableMapOf<String, Map<String, String>>().apply {
                mapAllBidders(adapters, bidTokens).forEach { bidder ->
                    if (initStatuses[bidder.key] == INITIALIZED) {
                        put(bidder.key, bidder.value)
                    }
                }
            }

        private fun mapInactiveBidders(
            adapters: Map<String, PartnerAdapter>,
            bidTokens: Map<String, Map<String, String>>,
            initStatuses: Map<String, PartnerController.PartnerInitializationStatus>,
        ): Map<String, Map<String, String>> =
            mutableMapOf<String, Map<String, String>>().apply {
                initStatuses.forEach { bidder ->
                    val initStatus = bidder.value
                    if (initStatus != null && initStatus != INITIALIZED) {
                        put(
                            bidder.key,
                            mutableMapOf<String, String>().apply {
                                put("status", initStatus.name)
                                if (initStatus == FAILED) {
                                    put("status", "Initialization Failed")
                                }
                            },
                        )
                    }
                }
            }
    }
}

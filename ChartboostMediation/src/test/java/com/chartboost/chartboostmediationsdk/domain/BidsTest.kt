/*
 * Copyright 2025 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.domain

import com.chartboost.chartboostmediationsdk.ad.ChartboostMediationBannerAdView
import io.mockk.mockk
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class BidsTest {
    @Test
    fun `adEventTrackers should be parsed from BidsExt correctly`() {
        val eventTrackersJson =
            buildJsonObject {
                put("load", Json.encodeToJsonElement(listOf(ServerEventTracker("init_load_url_1"), ServerEventTracker("init_load_url_2"))))
                put("show", Json.encodeToJsonElement(listOf(ServerEventTracker("init_show_url"))))
                put("click", Json.encodeToJsonElement(listOf(ServerEventTracker("init_click_url"))))
                put("initialization", Json.encodeToJsonElement(listOf(ServerEventTracker("init_initialization_url"))))
                put("banner_size", Json.encodeToJsonElement(listOf(ServerEventTracker("init_banner_size_url"))))
            }

        val bidsExt =
            BidsExt(
                responseTimeMillis = JsonNull,
                eventTrackers = eventTrackersJson,
            )

        val bidInfo =
            BidInfo(
                id = "123",
                impressionId = "imp1",
                price = 1.5,
                lurl = "",
                nurl = "",
                adm = null,
                burl = "",
                ext =
                    BidInfoExt(
                        bidderInfo = null,
                        ilrd = null,
                        cpmPrice = 1.5,
                        adRevenue = 0.5,
                        partnerPlacement = "test_partner_placement",
                        lineItemId = "line_item_id",
                    ),
            )

        val bidResponse =
            BidResponse(
                id = "auction123",
                bidId = "bid-abc",
                currency = "USD",
                bidInfoArray = listOf(bidInfo),
                partnerName = "test_partner",
                chartboostMediationBidId = "helium_bid_1",
            )

        val bidsResponse =
            BidsResponse(
                id = "auction123",
                seatbid = listOf(bidResponse),
                bidsExt = bidsExt,
            )

        val adLoadParams =
            AdLoadParams(
                adIdentifier = AdIdentifier(Ad.AdType.BANNER, "test_placement"),
                bannerSize = ChartboostMediationBannerAdView.ChartboostMediationBannerSize.STANDARD,
                loadId = "load123",
                adInteractionListener = mockk(),
                partnerSettings = mockk(),
                keywords = Keywords(),
            )

        val bids = Bids(adLoadParams, bidsResponse)

        val trackers = bids.adEventTrackers
        assertEquals(5, trackers.size)

        assertEquals("init_load_url_1", trackers[TrackingEvent.LOAD]?.get(0)?.url)
        assertEquals("init_load_url_2", trackers[TrackingEvent.LOAD]?.get(1)?.url)
        assertEquals("init_show_url", trackers[TrackingEvent.SHOW]?.get(0)?.url)
        assertEquals("init_click_url", trackers[TrackingEvent.CLICK]?.get(0)?.url)
        assertEquals("init_initialization_url", trackers[TrackingEvent.INITIALIZATION]?.get(0)?.url)
        assertEquals("init_banner_size_url", trackers[TrackingEvent.BANNER_SIZE]?.get(0)?.url)
    }
}

/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import com.chartboost.chartboostmediationsdk.domain.Ad
import com.chartboost.chartboostmediationsdk.domain.AdFormat
import com.chartboost.chartboostmediationsdk.domain.AdIdentifier
import com.chartboost.chartboostmediationsdk.domain.AdInteractionListener
import com.chartboost.chartboostmediationsdk.domain.Bid
import com.chartboost.chartboostmediationsdk.domain.Bids
import com.chartboost.chartboostmediationsdk.domain.ChartboostMediationAdException
import com.chartboost.chartboostmediationsdk.domain.PartnerAd
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworkingTest.Companion.LOAD_ID
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.testutils.NetworkTestJsonObjects
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert

fun ChartboostMediationNetworkingTest.`verify result for chartboost ("helium") impression success`() =
    runTest {
        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.trackChartboostImpression(
                testBids(),
                LOAD_ID,
                AdFormat.BANNER.key,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Event.HELIUM_IMPRESSION.endpoint
        val expectedRequestJson = NetworkTestJsonObjects.COMPLEX_IMPRESSION_REQUEST.trimmedJsonString

        val actualUrl = request.requestUrl.toString()

        Assert.assertEquals(expectedUrl, actualUrl)
        Assert.assertEquals(
            ChartboostMediationNetworkingTest.SESSION_ID,
            request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString(),
        )
        Assert.assertEquals(
            ChartboostMediationNetworkingTest.APP_ID,
            request.getHeader(ChartboostMediationNetworking.DEBUG_HEADER_KEY),
        )
        Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
        val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
        Assert.assertEquals(expectedResponseHttpCode, httpCode)
    }

fun ChartboostMediationNetworkingTest.`verify result for chartboost ("helium") impression failure`() =
    runTest {
        val expectedResponseHttpCode = 404
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.trackChartboostImpression(
                testBids(),
                LOAD_ID,
                AdFormat.BANNER.key,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Event.HELIUM_IMPRESSION.endpoint
        val expectedRequestJson = NetworkTestJsonObjects.COMPLEX_IMPRESSION_REQUEST.trimmedJsonString

        val actualUrl = request.requestUrl.toString()

        Assert.assertEquals(expectedUrl, actualUrl)
        Assert.assertEquals(
            ChartboostMediationNetworkingTest.SESSION_ID,
            request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString(),
        )
        Assert.assertEquals(
            ChartboostMediationNetworkingTest.APP_ID,
            request.getHeader(ChartboostMediationNetworking.DEBUG_HEADER_KEY),
        )
        Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Failure)
    }

fun testBids(): Bids {
    val adIdentifier: AdIdentifier = mockk()
    every { adIdentifier.adType } returns Ad.AdType.BANNER
    every { adIdentifier.placement } returns "-- PLACEMENT_NAME --"
    every { adIdentifier.placementType } returns "banner"

    val activeBid: Bid = mockk()
    every { activeBid.adIdentifier } returns adIdentifier
    every { activeBid.partnerName } returns "active_bid_partner"
    every { activeBid.isMediation } returns true
    every { activeBid.lineItemId } returns "-- LINE_ITEM_ID --"
    every { activeBid.partnerPlacement } returns "-- PARTNER_PLACEMENT --"
    every { activeBid.price } returns 9.99
    every { activeBid.lurl } returns "lurl"
    every { activeBid.nurl } returns "nurl"

    val allBids: MutableList<Bid> = mutableListOf()
    allBids.add(activeBid)

    val bids: Bids = mockk()
    every { bids.activeBid } returns activeBid
    every { bids.auctionId } returns ChartboostMediationNetworkingTest.AUCTION_ID
    every { bids.adIdentifier } returns adIdentifier
    every { bids.bids } returns allBids
    every { bids.iterator() } returns allBids.iterator()

    return bids
}

class MockAdInteractionListener : AdInteractionListener {
    override fun onImpressionTracked(partnerAd: PartnerAd) {
    }

    override fun onClicked(partnerAd: PartnerAd) {
    }

    override fun onRewarded(partnerAd: PartnerAd) {
    }

    override fun onDismissed(
        partnerAd: PartnerAd,
        error: ChartboostMediationAdException?,
    ) {
    }

    override fun onExpired(partnerAd: PartnerAd) {
    }
}

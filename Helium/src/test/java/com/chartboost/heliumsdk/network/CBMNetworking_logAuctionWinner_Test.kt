/*
 * Copyright 2023 Chartboost, Inc.
 * 
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.domain.Bids
import com.chartboost.heliumsdk.domain.BidsResponse
import com.chartboost.heliumsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.heliumsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.heliumsdk.utils.HeliumJson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert

fun ChartboostMediationNetworkingTest.`verify log auction winner request success`() = runTest {
    val expectedResponseHttpCode = 200
    MockResponse()
        .setResponseCode(expectedResponseHttpCode)
        .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
        .let {
            mockWebServer.enqueue(it)
        }

    val bidsResponse = HeliumJson.decodeFromString(
        BidsResponse.serializer(),
        NetworkTestJsonObjects.INTERSTITIAL_BID_RESPONSE.rawJsonString
    )

    val adLoadParams = setupInterstitialAdLoadParams()

    val bids = Bids(
        adLoadParams,
        bidsResponse,
    )

    val response = ChartboostMediationNetworking.logAuctionWinner(
        bids,
        ChartboostMediationNetworkingTest.LOAD_ID
    )

    val request = mockWebServer.takeRequest()

    val expectedUrl = Endpoints.Sdk.Event.WINNER.endpoint
    val expectedRequestJson =
        NetworkTestJsonObjects.LOG_AUCTION_WINNER_REQUEST.minifiedJsonString.format(
            ChartboostMediationNetworkingTest.AUCTION_ID
        )

    val actualUrl = request.requestUrl.toString()

    Assert.assertEquals(expectedUrl, actualUrl)
    Assert.assertEquals(
        ChartboostMediationNetworkingTest.SESSION_ID,
        request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(
        ChartboostMediationNetworkingTest.LOAD_ID,
        request.getHeader(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY).toString()
    )
    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

    Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
    val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
    Assert.assertEquals(expectedResponseHttpCode, httpCode)
}

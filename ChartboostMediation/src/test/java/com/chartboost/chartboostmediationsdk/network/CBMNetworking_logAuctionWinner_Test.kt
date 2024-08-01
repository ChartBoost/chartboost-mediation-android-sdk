/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import com.chartboost.chartboostmediationsdk.domain.AdFormat
import com.chartboost.chartboostmediationsdk.domain.Bids
import com.chartboost.chartboostmediationsdk.domain.BidsResponse
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert

fun ChartboostMediationNetworkingTest.`verify log auction winner request success`() =
    runTest {
        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }

        val bidsResponse =
            ChartboostMediationJson.decodeFromString(
                BidsResponse.serializer(),
                NetworkTestJsonObjects.INTERSTITIAL_BID_RESPONSE.rawJsonString,
            )

        val adLoadParams = setupInterstitialAdLoadParams()

        val bids =
            Bids(
                adLoadParams,
                bidsResponse,
            )

        val response =
            ChartboostMediationNetworking.logAuctionWinner(
                bids,
                ChartboostMediationNetworkingTest.LOAD_ID,
                AdFormat.BANNER.key,
            )

        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Event.WINNER.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.LOG_AUCTION_WINNER_REQUEST.minifiedJsonString.format(
                ChartboostMediationNetworkingTest.AUCTION_ID,
            )

        val actualUrl = request.requestUrl.toString()

        Assert.assertEquals(expectedUrl, actualUrl)
        Assert.assertEquals(
            ChartboostMediationNetworkingTest.SESSION_ID,
            request.getHeader(ChartboostMediationNetworking.SESSION_ID_HEADER_KEY).toString(),
        )
        Assert.assertEquals(
            ChartboostMediationNetworkingTest.LOAD_ID,
            request.getHeader(ChartboostMediationNetworking.MEDIATION_LOAD_ID_HEADER_KEY).toString(),
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

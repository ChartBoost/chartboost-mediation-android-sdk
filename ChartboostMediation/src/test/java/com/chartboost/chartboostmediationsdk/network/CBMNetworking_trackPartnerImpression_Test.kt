/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import com.chartboost.chartboostmediationsdk.domain.AdFormat
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworkingTest.Companion.APP_SET_ID
import com.chartboost.chartboostmediationsdk.network.ChartboostMediationNetworkingTest.Companion.LOAD_ID
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.testutils.NetworkTestJsonObjects
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert

fun ChartboostMediationNetworkingTest.`verify result for partner impression success`() =
    runTest {
        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.HTTP_200_SIMPLE_SUCCESS.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.trackPartnerImpression(
                APP_SET_ID,
                ChartboostMediationNetworkingTest.AUCTION_ID,
                LOAD_ID,
                AdFormat.BANNER.key,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Event.PARTNER_IMPRESSION.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.SIMPLE_AUCTION_ID_REQUEST.trimmedJsonString.format(
                ChartboostMediationNetworkingTest.AUCTION_ID,
            )

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

fun ChartboostMediationNetworkingTest.`verify result for partner impression failure`() =
    runTest {
        val expectedResponseHttpCode = 404
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.trackPartnerImpression(
                APP_SET_ID,
                ChartboostMediationNetworkingTest.AUCTION_ID,
                LOAD_ID,
                AdFormat.BANNER.key,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Event.PARTNER_IMPRESSION.endpoint
        val expectedRequestJson =
            String.format(
                NetworkTestJsonObjects.SIMPLE_AUCTION_ID_REQUEST.trimmedJsonString,
                ChartboostMediationNetworkingTest.AUCTION_ID,
            )

        val actualUrl = request.requestUrl.toString()

        Assert.assertEquals(expectedUrl, actualUrl)
        // partner impressions should not contain a helium session id header
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

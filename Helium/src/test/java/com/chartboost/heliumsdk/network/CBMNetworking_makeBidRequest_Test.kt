/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.heliumsdk.network

import com.chartboost.heliumsdk.network.ChartboostMediationNetworkingTest.Companion.BANNER_IMPRESSION_DEPTH
import com.chartboost.heliumsdk.network.ChartboostMediationNetworkingTest.Companion.INTERSTITIAL_IMPRESSION_DEPTH
import com.chartboost.heliumsdk.network.ChartboostMediationNetworkingTest.Companion.RATE_LIMIT_HEADER_VALUE
import com.chartboost.heliumsdk.network.ChartboostMediationNetworkingTest.Companion.REWARDED_IMPRESSION_DEPTH
import com.chartboost.heliumsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.heliumsdk.network.testutils.NetworkTestJsonObjects
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert

/*
    Banners
 */

fun ChartboostMediationNetworkingTest.`verify banner bid request success`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupBannerAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BANNER_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                BANNER_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.BANNER_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
        val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
        Assert.assertEquals(expectedResponseHttpCode, httpCode)

        val responseRateLimitHeader = response.headers[ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY]
        Assert.assertEquals(RATE_LIMIT_HEADER_VALUE, responseRateLimitHeader)
    }

fun ChartboostMediationNetworkingTest.`verify banner bid request failure due to json parsing`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupBannerAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BAD_BANNER_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                BANNER_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.BANNER_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertNotNull(response)
        Assert.assertTrue(response is ChartboostMediationNetworkingResult.JsonParsingFailure)
    }

fun ChartboostMediationNetworkingTest.`verify banner bid request failure due to http code`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupBannerAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 404
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BAD_BANNER_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                BANNER_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.BANNER_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertNotNull(response)
        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Failure)
    }

/*
    Interstitial
 */

fun ChartboostMediationNetworkingTest.`verify interstitial bid request success`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupInterstitialAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.INTERSTITIAL_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                INTERSTITIAL_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.INTERSTITIAL_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
        val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
        Assert.assertEquals(expectedResponseHttpCode, httpCode)

        val responseRateLimitHeader = response.headers[ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY]
        Assert.assertEquals(RATE_LIMIT_HEADER_VALUE, responseRateLimitHeader)
    }

fun ChartboostMediationNetworkingTest.`verify interstitial bid request failure due to json parsing`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupInterstitialAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BAD_INTERSTITIAL_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                INTERSTITIAL_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.INTERSTITIAL_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertNotNull(response)
        Assert.assertTrue(response is ChartboostMediationNetworkingResult.JsonParsingFailure)
    }

fun ChartboostMediationNetworkingTest.`verify interstitial bid request failure due to http code`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupInterstitialAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 404
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BAD_INTERSTITIAL_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                INTERSTITIAL_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.INTERSTITIAL_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertNotNull(response)
        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Failure)
    }

/*
    Rewarded
 */

fun ChartboostMediationNetworkingTest.`verify rewarded bid request success`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupRewardedAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.REWARDED_BID_RESPONSE_GET_CALLBACK.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                REWARDED_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.REWARDED_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)
        val httpCode = (response as ChartboostMediationNetworkingResult.Success).httpCode
        Assert.assertEquals(expectedResponseHttpCode, httpCode)

        val responseRateLimitHeader = response.headers[ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY]
        Assert.assertEquals(RATE_LIMIT_HEADER_VALUE, responseRateLimitHeader)
    }

fun ChartboostMediationNetworkingTest.`verify rewarded bid request failure due to json parsing`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupRewardedAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BAD_REWARDED_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                REWARDED_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.REWARDED_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertNotNull(response)
        Assert.assertTrue(response is ChartboostMediationNetworkingResult.JsonParsingFailure)
    }

fun ChartboostMediationNetworkingTest.`verify rewarded bid request failure due to http code`() =
    runTest {
        val mockedPrivacyController = setupPrivacyController()
        val mockedPartnerController = setupPartnerController()
        val adLoadParams = setupRewardedAdLoadParams()
        val bidTokens = setupBidTokens()

        val expectedResponseHttpCode = 404
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setBody(NetworkTestJsonObjects.BAD_REWARDED_BID_RESPONSE.trimmedJsonString)
            .setHeader(ChartboostMediationNetworking.RATE_LIMIT_HEADER_KEY, RATE_LIMIT_HEADER_VALUE)
            .let {
                mockWebServer.enqueue(it)
            }

        val response =
            ChartboostMediationNetworking.makeBidRequest(
                mockedPrivacyController,
                mockedPartnerController,
                adLoadParams,
                bidTokens,
                "0",
                REWARDED_IMPRESSION_DEPTH,
            )
        val request = mockWebServer.takeRequest()

        val expectedUrl = Endpoints.Rtb.AUCTIONS.endpoint
        val expectedRequestJson =
            NetworkTestJsonObjects.REWARDED_BID_REQUEST.minifiedJsonString

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
//    Commented out until we can figure out why the robolectric tests are non-deterministic
//    Assert.assertEquals(expectedRequestJson, request.body.readUtf8())

        Assert.assertNotNull(response)
        Assert.assertTrue(response is ChartboostMediationNetworkingResult.Failure)
    }

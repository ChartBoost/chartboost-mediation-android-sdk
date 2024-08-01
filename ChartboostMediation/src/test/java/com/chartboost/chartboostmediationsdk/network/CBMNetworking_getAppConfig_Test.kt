/*
 * Copyright 2023-2024 Chartboost, Inc.
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file.
 */

package com.chartboost.chartboostmediationsdk.network

import com.chartboost.chartboostmediationsdk.domain.AppConfig
import com.chartboost.chartboostmediationsdk.network.model.ChartboostMediationNetworkingResult
import com.chartboost.chartboostmediationsdk.network.testutils.NetworkTestJsonObjects
import com.chartboost.chartboostmediationsdk.utils.ChartboostMediationJson
import com.chartboost.core.ChartboostCore
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert
import java.util.concurrent.TimeUnit

fun ChartboostMediationNetworkingTest.`verify result for sdk_init success with new config`() =
    runTest {
        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY, ChartboostMediationNetworkingTest.INIT_HASH_NEW)
            .setBody(NetworkTestJsonObjects.HTTP_200_SDK_INIT_SUCCESS.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }

        launch {
            val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""
            val response =
                ChartboostMediationNetworking.getAppConfig(
                    ChartboostMediationNetworkingTest.APP_ID,
                    ChartboostMediationNetworkingTest.INIT_HASH_OLD,
                    appSetId,
                )

            val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)

            val expectedRequestUrl = "${Endpoints.Sdk.SDK_INIT.endpoint}/${ChartboostMediationNetworkingTest.APP_ID}"
            val expectedRequestInitHashHeaderValue = ChartboostMediationNetworkingTest.INIT_HASH_OLD
            val expectedResult =
                ChartboostMediationJson.decodeFromString(
                    AppConfig.serializer(),
                    NetworkTestJsonObjects.HTTP_200_SDK_INIT_SUCCESS.trimmedJsonString,
                )

            val actualRequestUrl = request?.requestUrl.toString()
            val actualRequestInitHashHeaderValue = request?.getHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY)
            val actualRequestDebugHeaderValue = request?.getHeader(ChartboostMediationNetworking.DEBUG_HEADER_KEY)

            Assert.assertEquals(expectedRequestUrl, actualRequestUrl)
            Assert.assertEquals(
                expectedRequestInitHashHeaderValue,
                actualRequestInitHashHeaderValue,
            )
            Assert.assertEquals(ChartboostMediationNetworkingTest.APP_ID, actualRequestDebugHeaderValue)

            Assert.assertNotNull(response)
            Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)

            val httpCode =
                (response as ChartboostMediationNetworkingResult.Success<AppConfig?>).httpCode
            Assert.assertEquals(expectedResponseHttpCode, httpCode)

            val responseInitHashHeader = response.headers[ChartboostMediationNetworking.INIT_HASH_HEADER_KEY]
            Assert.assertEquals(ChartboostMediationNetworkingTest.INIT_HASH_NEW, responseInitHashHeader)

            val result = response.body
            Assert.assertTrue(expectedResult == result)
        }
    }

fun ChartboostMediationNetworkingTest.`verify result for sdk_init success with no new config`() =
    runTest {
        val expectedResponseHttpCode = 204
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY, ChartboostMediationNetworkingTest.INIT_HASH_NEW)
            .let {
                mockWebServer.enqueue(it)
            }

        launch {
            val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""
            val response =
                ChartboostMediationNetworking.getAppConfig(
                    ChartboostMediationNetworkingTest.APP_ID,
                    ChartboostMediationNetworkingTest.INIT_HASH_OLD,
                    appSetId,
                )

            val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)

            val expectedRequestUrl = "${Endpoints.Sdk.SDK_INIT.endpoint}/${ChartboostMediationNetworkingTest.APP_ID}"
            val expectedRequestInitHashHeaderValue = ChartboostMediationNetworkingTest.INIT_HASH_OLD

            val actualRequestUrl = request?.requestUrl.toString()
            val actualRequestInitHashHeaderValue = request?.getHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY)
            val actualRequestDebugHeaderValue = request?.getHeader(ChartboostMediationNetworking.DEBUG_HEADER_KEY)

            Assert.assertEquals(expectedRequestUrl, actualRequestUrl)
            Assert.assertEquals(
                expectedRequestInitHashHeaderValue,
                actualRequestInitHashHeaderValue,
            )
            Assert.assertEquals(ChartboostMediationNetworkingTest.APP_ID, actualRequestDebugHeaderValue)

            Assert.assertNotNull(response)
            Assert.assertTrue(response is ChartboostMediationNetworkingResult.Success)

            val httpCode =
                (response as ChartboostMediationNetworkingResult.Success<AppConfig?>).httpCode
            Assert.assertEquals(expectedResponseHttpCode, httpCode)

            val responseInitHashHeader = response.headers[ChartboostMediationNetworking.INIT_HASH_HEADER_KEY]
            Assert.assertEquals(ChartboostMediationNetworkingTest.INIT_HASH_NEW, responseInitHashHeader)

            val result = response.body
            Assert.assertNull(result)
        }
    }

fun ChartboostMediationNetworkingTest.`verify result for sdk_init failure due to JSON parsing`() =
    runTest {
        val expectedResponseHttpCode = 200
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY, ChartboostMediationNetworkingTest.INIT_HASH_NEW)
            .setBody(NetworkTestJsonObjects.HTTP_200_SDK_INIT_FAILURE.trimmedJsonString)
            .let {
                mockWebServer.enqueue(it)
            }

        launch {
            val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""
            val response =
                ChartboostMediationNetworking.getAppConfig(
                    ChartboostMediationNetworkingTest.APP_ID,
                    ChartboostMediationNetworkingTest.INIT_HASH_OLD,
                    appSetId,
                )

            val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)

            val expectedRequestUrl = "${Endpoints.Sdk.SDK_INIT.endpoint}/${ChartboostMediationNetworkingTest.APP_ID}"
            val expectedRequestInitHashHeaderValue = ChartboostMediationNetworkingTest.INIT_HASH_OLD

            val actualRequestUrl = request?.requestUrl.toString()
            val actualRequestInitHashHeaderValue = request?.getHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY)
            val actualRequestDebugHeaderValue = request?.getHeader(ChartboostMediationNetworking.DEBUG_HEADER_KEY)

            Assert.assertEquals(expectedRequestUrl, actualRequestUrl)
            Assert.assertEquals(
                expectedRequestInitHashHeaderValue,
                actualRequestInitHashHeaderValue,
            )
            Assert.assertEquals(ChartboostMediationNetworkingTest.APP_ID, actualRequestDebugHeaderValue)

            Assert.assertNotNull(response)
            Assert.assertTrue(response is ChartboostMediationNetworkingResult.JsonParsingFailure)
        }
    }

fun ChartboostMediationNetworkingTest.`verify result for sdk_init failure due to http code`() =
    runTest {
        val expectedResponseHttpCode = 404
        MockResponse()
            .setResponseCode(expectedResponseHttpCode)
            .setHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY, ChartboostMediationNetworkingTest.INIT_HASH_NEW)
            .let {
                mockWebServer.enqueue(it)
            }

        launch {
            val appSetId = ChartboostCore.analyticsEnvironment.getVendorIdentifier() ?: ""
            val response =
                ChartboostMediationNetworking.getAppConfig(
                    ChartboostMediationNetworkingTest.APP_ID,
                    ChartboostMediationNetworkingTest.INIT_HASH_OLD,
                    appSetId,
                )

            val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)

            val expectedRequestUrl = "${Endpoints.Sdk.SDK_INIT.endpoint}/${ChartboostMediationNetworkingTest.APP_ID}"
            val expectedRequestInitHashHeaderValue = ChartboostMediationNetworkingTest.INIT_HASH_OLD

            val actualRequestUrl = request?.requestUrl.toString()
            val actualRequestInitHashHeaderValue = request?.getHeader(ChartboostMediationNetworking.INIT_HASH_HEADER_KEY)
            val actualRequestDebugHeaderValue = request?.getHeader(ChartboostMediationNetworking.DEBUG_HEADER_KEY)

            Assert.assertEquals(expectedRequestUrl, actualRequestUrl)
            Assert.assertEquals(
                expectedRequestInitHashHeaderValue,
                actualRequestInitHashHeaderValue,
            )
            Assert.assertEquals(ChartboostMediationNetworkingTest.APP_ID, actualRequestDebugHeaderValue)

            Assert.assertNotNull(response)
            Assert.assertTrue(response is ChartboostMediationNetworkingResult.Failure)
        }
    }
